package gossip;

import partitions.Node;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static gossip.GossipAction.MessageType.ALIVE;
import static gossip.GossipAction.MessageType.SUSPECT;

public class FailureDetector extends Thread {

    String nodeId;
    // incarnation number of the local node
    int incarnationNumber = 0;
    // node lookups by id
    Map<String, Node> nodes;
    // number of other nodes that are sent PING_REQs when we suspect a node is unavailable
    int subgroupSize;
    SortedMap<String, GossipClient> clients;
    // awaitedAcks contains the acks that have been sent out into the world, along with any other nodes that are waiting
    // to find out the status of the associated pings, which must be informed if the ack returns successfully
    // The first map's key is the node that is being tested for aliveness
    // The list contains the node that is waiting to be informed about the aliveness
    Map<String, List<String>> awaitedAcks = new HashMap<>();

    TaskScheduler gossipScheduler;
    TaskScheduler timeoutScheduler;
    // update messages that should be shared via piggybacked gossip messages, in preference order
    LinkedList<StateUpdate> updates = new LinkedList<>();

    public FailureDetector(long gossipPeriod, long timeoutPeriod, String nodeId,
                           int subgroupSize, SortedMap<String, GossipClient> clients, List<Node> nodes) {
        this.subgroupSize = subgroupSize;
        this.gossipScheduler = new FixedRateScheduler(gossipPeriod);
        this.timeoutScheduler = new DelayScheduler(timeoutPeriod);
        this.nodeId = nodeId;
        this.clients = clients;

        Map<String, Node> nodeMap = new HashMap<>();
        for (Node node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        this.nodes = nodeMap;
    }

    public FailureDetector(TaskScheduler gossipScheduler, TaskScheduler timeoutScheduler,
                           String nodeId, int subgroupSize, SortedMap<String, GossipClient> clients, Map<String, Node> nodes) {
        this.subgroupSize = subgroupSize;
        this.gossipScheduler = gossipScheduler;
        this.timeoutScheduler = timeoutScheduler;
        this.nodeId = nodeId;
        this.clients = clients;
        this.nodes = nodes;
    }

    public void sendGossip(GossipAction.ActionType actionType, String target, String node) throws IOException {
        GossipAction action = piggybackMessages(new GossipAction(actionType, this.nodeId, target));
        sendWithClient(node, action);
    }

    public void sendGossip(GossipAction.ActionType actionType, String node) throws IOException {
        GossipAction action = piggybackMessages(new GossipAction(actionType, this.nodeId, ""));
        sendWithClient(node, action);
    }

    private synchronized void addAwaitedAck(GossipAction action) {
        List<String> currentActions = awaitedAcks.getOrDefault(action.getTarget(), new ArrayList<>());
        currentActions.add(action.getSenderId());
        awaitedAcks.put(action.getTarget(), currentActions);
    }

    private void  forwardAcks(GossipAction action) throws IOException {
        if (awaitedAcks.containsKey(action.getTarget())) {
            List<String> currentActions = awaitedAcks.get(action.getTarget());
            for (String nodeToInform : currentActions) {
                sendGossip(GossipAction.ActionType.ACK, action.getTarget(), nodeToInform);
            }
            awaitedAcks.put(action.getTarget(), new ArrayList<>());
        }
    }

    private synchronized void removeAwaitedAcks(GossipAction action) {
        awaitedAcks.put(action.getTarget(), new ArrayList<>());
        timeoutScheduler.cancel(action.getTarget());
    }

    private void processPiggybacks(List<GossipAction.Message> messages) {
        for (GossipAction.Message message : messages) {
            // if the node is spreading malicious rumors that this node is dead, we must correct the record
            if (message.target.equals(nodeId)) {
                if (message.incarnationNumber == incarnationNumber) {
                    incarnationNumber++;
                    updates.push(new StateUpdate(new GossipAction.Message(
                            ALIVE, incarnationNumber, nodeId)));
                }
                continue;
            }

            if (!nodes.containsKey(message.target)) {
                continue;
            }
            Node node = nodes.get(message.target);

            switch (message.messageType) {
                case ALIVE:
                    if (message.incarnationNumber > node.getIncarnation()) {
                        node.setStatus(Node.Status.READY);
                        updates.push(new StateUpdate(message));
                        node.setIncarnation(message.incarnationNumber);
                    }
                    break;
                case SUSPECT:
                    if (message.incarnationNumber > node.getIncarnation()) {
                        node.setStatus(Node.Status.SUSPECT);
                        updates.push(new StateUpdate(message));
                    }
                    break;
            }
        }
    }

    public void handleAction(GossipAction action) throws IOException, InterruptedException {
        processPiggybacks(action.getMessages());

        switch (action.actionType) {
            case PING:
                sendGossip(GossipAction.ActionType.ACK, nodeId, action.getSenderId());
                break;
            case ACK:
                if (nodes.containsKey(action.getTarget())) {
                    nodes.get(action.getTarget()).setStatus(Node.Status.READY);
                }
                forwardAcks(action);
                removeAwaitedAcks(action);
                break;
            case PING_REQ:
                addAwaitedAck(action);
                sendGossip(GossipAction.ActionType.PING, action.getTarget());
                startFinalAckTimeout(action.getTarget());
                break;
        }
    }

    private void sendWithClient(String nodeId, GossipAction action) throws IOException {
        GossipClient client = getClient(nodeId);
        if (client == null) {
            return;
        }
        client.sendMessage(action);
    }

    private GossipClient getClient(String nodeId) {
        if (clients.containsKey(nodeId)) {
            return this.clients.get(nodeId);
        }
        return null;
    }

    private GossipAction piggybackMessages(GossipAction action) {
        int updatesLeft = updates.size();
        int msg = 0;
        while (msg < GossipAction.MAX_MESSAGES && updatesLeft > 0) {
            StateUpdate update = updates.pop();
            action.addMessage(msg++, update.message);

            update.incPiggybackCount();
            if (!update.expired()) {
                updates.add(update);
            }
            updatesLeft--;
            msg++;
        }

        return action;
    }

    private void addStateUpdate(GossipAction.MessageType messageType, int incarnationNumber, String target) {
        updates.push(new StateUpdate(new GossipAction.Message(messageType, incarnationNumber, target)));
    }

    public void run() {
        TimerTask pingTask = new TimerTask() {
            int i = 0;

            public void run() {
                String node = (String) clients.keySet().toArray()[i];
                try {
                    sendGossip(GossipAction.ActionType.PING, node);
                    startInitialAckTimeout(node);
                } catch (Exception e) {
                    System.err.println("gossip error");
                    e.printStackTrace();
                }
                if (i < clients.size() - 1) {
                    i++;
                } else {
                    i = 0;
                }
            }
        };
        if (clients.size() > 0) {
            try {
                gossipScheduler.schedule("main", pingTask);
            } catch (InterruptedException e) {
                System.err.println("starting gossip scheduler: " + e);
            }
        }
    }

    public void startInitialAckTimeout(String suspectedNode) throws InterruptedException {
        TimerTask awaitSurrogateAckTask = new TimerTask() {
            public void run() {
                Object[] nodeValues = nodes.values().toArray();
                for (int i = 0; i < subgroupSize; i++) {
                    Node node = (Node) nodeValues[i];
                    if (node.getStatus() != Node.Status.READY || node.getId() == suspectedNode) {
                        continue;
                    }

                    try {
                        sendGossip(GossipAction.ActionType.PING_REQ, node.getId(), suspectedNode);
                    } catch (IOException e) {
                        // TODO: log this or something
                    }
                }
                try {
                    startFinalAckTimeout(suspectedNode);
                } catch (InterruptedException e) {}
            }
        };
        timeoutScheduler.schedule(suspectedNode, awaitSurrogateAckTask);
    }

    public void startFinalAckTimeout(String nodeId) throws InterruptedException {
        TimerTask awaitAckTask = new TimerTask() {
            public void run() {
                if (nodes.containsKey(nodeId)) {
                    Node node = nodes.get(nodeId);
                    node.setStatus(Node.Status.SUSPECT);
                    addStateUpdate(SUSPECT, node.getIncarnation(), nodeId);
                }
            }
        };
        timeoutScheduler.schedule(nodeId, awaitAckTask);
    }

    private interface TaskScheduler {
        void schedule(String id, TimerTask task) throws InterruptedException;
        void cancel(String id);
    }

    public static class LatchScheduler implements TaskScheduler {
        private CountDownLatch inLatch;
        private CountDownLatch outLatch;
        Map<String, Boolean> tasks = new HashMap<>();

        public LatchScheduler() {
            this.inLatch = new CountDownLatch(1);
            this.outLatch = new CountDownLatch(1);
        }

        public void countDown() {
            this.inLatch.countDown();
        }

        public void resetIn() {
            this.inLatch = new CountDownLatch(1);
        }

        public void resetOut() {
            this.outLatch = new CountDownLatch(1);
        }

        public void await() throws InterruptedException {
            outLatch.await();
        }

        public void schedule(String id, TimerTask task) throws InterruptedException {
            tasks.put(id, false);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            inLatch.await();
                            if (tasks.get(id)) { return; }
                            resetIn();
                        } catch (InterruptedException e) {
                            System.out.println("scheduled task was interrupted: " + e);
                        }
                        task.run();
                        outLatch.countDown();
                        if (tasks.get(id)) { return; }
                        resetOut();
                    }
                }
            });
            t.start();
        }

        public void cancel(String id) {
            tasks.put(id, true);
        }
    }

    private static class DelayScheduler implements TaskScheduler {
        long delay;
        Map<String, Timer> timers = new HashMap<>();

        public DelayScheduler(long delay) {
            this.delay = delay;
        }

        public void schedule(String id, TimerTask task) {
            Timer timer = new Timer();
            timers.put(id, timer);
            timer.schedule(task, delay);
            // TODO: the way this is right now if a new task gets scheduled with the same id it'll override the old
            // task which could prevent it from completing on time. Instead, we should just toss the second task
            // because the first task will result in it being fulfilled any way
        }

        public void cancel(String id) {
            if (timers.containsKey(id)) {
                timers.get(id).cancel();
            }
        }
    }

    private static class FixedRateScheduler implements TaskScheduler {
        long delay;
        Map<String, Timer> timers = new HashMap<>();

        public FixedRateScheduler(long delay) {
            this.delay = delay;
        }

        public void schedule(String id, TimerTask task) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(task, delay, delay);
        }

        public void cancel(String id) {
            timers.get(id).cancel();
        }
    }

    private class StateUpdate {
        int piggybackCount = 0;
        GossipAction.Message message;

        public StateUpdate(GossipAction.Message message) {
            this.message = message;
        }

        public synchronized void incPiggybackCount() {
            this.piggybackCount += 1;
        }

        public boolean expired() {
            return piggybackCount < Math.log(subgroupSize);
        }
    }
}
