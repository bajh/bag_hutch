package gossip;

import partitions.Node;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import static gossip.GossipAction.MessageType.ALIVE;
import static partitions.Node.Status.READY;
import static partitions.Node.Status.SUSPECT;

public class FailureDetector extends Thread {
    String nodeId;
    int incarnationNumber = 0;

    // number of other nodes that are sent PING_REQs when we suspect a node is unavailable
    int subgroupSize;

    Map<String, Node> nodes = new HashMap<>();
    SortedMap<String, GossipClient> clients;

    private final BlockingQueue<Event> eventQueue = new LinkedBlockingDeque<>();
    // The keys are the nodes whose liveness is waiting to be confirmed
    // The value is a set of other nodes that want to be informed when the ack arrives
    Map<String, Set<String>> awaitedAcks = new HashMap<>();

    LinkedList<StateUpdate> updates = new LinkedList<>();

    TaskScheduler pingScheduler;
    TaskScheduler timeoutScheduler;

    public FailureDetector(long gossipPeriod, long timeoutPeriod, String nodeId,
                    int subgroupSize, SortedMap<String, GossipClient> clients, List<Node> nodes) {
        this.subgroupSize = subgroupSize;
        this.pingScheduler = new FixedRateScheduler(gossipPeriod);
        this.timeoutScheduler = new DelayScheduler(timeoutPeriod);
        this.nodeId = nodeId;
        this.clients = clients;

        Map<String, Node> nodeMap = new HashMap<>();
        for (Node node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        this.nodes = nodeMap;
    }

    public FailureDetector(TaskScheduler pingScheduler, TaskScheduler timeoutScheduler,
                              String nodeId, int subgroupSize, SortedMap<String, GossipClient> clients, Map<String, Node> nodes) {
        this.subgroupSize = subgroupSize;
        this.pingScheduler = pingScheduler;
        this.timeoutScheduler = timeoutScheduler;
        this.nodeId = nodeId;
        this.clients = clients;
        this.nodes = nodes;
    }

    private interface Event {
        void process();
    }

    class EventConsumer extends Thread {
        public void run() {
            try {
                while (true) {
                    Event event = eventQueue.take();
                    event.process();
                }
            } catch (InterruptedException e) {
                System.err.println("event consumer was interrupted: " + e);
            }
        }
    }

    private class PingReceivedEvent implements Event {
        String senderId;
        List<GossipAction.Message> messages;

        public PingReceivedEvent(String senderId, List<GossipAction.Message> messages) {
            this.senderId = senderId;
            this.messages = messages;
        }

        public void process() {
            processPiggybacks(messages);
        }
    }

    private class PingEvent implements Event {
        String onBehalfOf;
        String target;

        public PingEvent(String onBehalfOf, String target) {
            this.onBehalfOf = onBehalfOf;
            this.target = target;
        }

        public void process() {
            Set waitingNodes = awaitedAcks.getOrDefault(target, new HashSet<>());
            waitingNodes.add(target);
            awaitedAcks.put(target, waitingNodes);
        }
    }

    private class AckEvent implements Event {
        String sender;
        String confirmsLivenessOf;
        List<GossipAction.Message> messages;

        public AckEvent(String sender, String confirmsLivenessOf, List<GossipAction.Message> messages) {
            this.sender = sender;
            this.confirmsLivenessOf = confirmsLivenessOf;
            this.messages = messages;
        }

        public void process() {
            processPiggybacks(messages);
            awaitedAcks.remove(confirmsLivenessOf);
            if (nodes.containsKey(confirmsLivenessOf)) {
                Node node = nodes.get(confirmsLivenessOf);
                if (node.getStatus() != READY) {
                    node.setStatus(READY);
                    addStateUpdate(ALIVE, node.getIncarnation(), confirmsLivenessOf);
                }
            }
        }
    }

    private class TimeoutEvent implements Event {
        String suspect;

        public TimeoutEvent(String suspect) {
            this.suspect = suspect;
        }

        public void process() {
            if (awaitedAcks.containsKey(suspect) && nodes.containsKey(suspect)) {
                Node node = nodes.get(suspect);
                if (node.getStatus() != SUSPECT) {
                    node.setStatus(SUSPECT);
                    addStateUpdate(GossipAction.MessageType.SUSPECT, node.getIncarnation(), suspect);
                }
            }

            awaitedAcks.remove(suspect);
        }
    }

    public void handleAction(GossipAction action) throws IOException, InterruptedException {
        switch (action.actionType) {
            case PING:
                eventQueue.put(new PingReceivedEvent(action.getSenderId(), action.getMessages()));
                sendGossip(GossipAction.ActionType.ACK, nodeId, action.getSenderId());
                break;
            case ACK:
                eventQueue.put(new AckEvent(action.getSenderId(), action.getTarget(), action.getMessages()));
                break;
        }
    }

    public void run() {
        (new EventConsumer()).start();

        TimerTask pingTask = new TimerTask() {
            int i = 0;

            public void run() {
                String node = (String) clients.keySet().toArray()[i];
                try {
                    eventQueue.put(new PingEvent(nodeId, node));
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
                pingScheduler.schedule(pingTask);
            } catch (InterruptedException e) {
                System.err.println("starting gossip scheduler: " + e);
            }
        }
    }

    private void processPiggybacks(List<GossipAction.Message> messages) {
        for (GossipAction.Message message : messages) {
            // if the node is spreading malicious rumors that this node is dead, we must correct the record
            // "I'm not dead yet! I think I could go for a walk!"
            if (message.target.equals(nodeId) && message.messageType == GossipAction.MessageType.SUSPECT) {
                if (message.incarnationNumber == incarnationNumber) {
                    incarnationNumber++;
                    addStateUpdate(GossipAction.MessageType.ALIVE, incarnationNumber, nodeId);
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
                    if (message.incarnationNumber == node.getIncarnation()) {
                        node.setStatus(Node.Status.SUSPECT);
                        updates.push(new StateUpdate(message));
                    }
                    break;
            }
        }
    }

    private void addStateUpdate(GossipAction.MessageType messageType, int incarnationNumber, String target) {
        updates.push(new StateUpdate(new GossipAction.Message(messageType, incarnationNumber, target)));
    }

    public void startInitialAckTimeout(String nodeToTest) throws InterruptedException {
        TimerTask awaitSurrogateAckTask = new TimerTask() {
            public void run() {
                try {
                    eventQueue.put(new TimeoutEvent(nodeToTest));
                } catch (InterruptedException e) {
                    System.err.println("interrupted: " + e);
                }
            }
        };
        timeoutScheduler.schedule(awaitSurrogateAckTask);
    }

    private interface TaskScheduler {
        void schedule(TimerTask task) throws InterruptedException;
    }

    private static class DelayScheduler implements TaskScheduler {
        long delay;
        Timer timer;

        public DelayScheduler(long delay) {
            this.delay = delay;
            this.timer = new Timer();
        }

        public void schedule(TimerTask task) {
            timer.schedule(task, delay);
        }
    }

    private static class FixedRateScheduler implements TaskScheduler {
        long delay;
        Timer timer;

        public FixedRateScheduler(long delay) {
            this.delay = delay;
            this.timer = new Timer();
        }

        public void schedule(TimerTask task) {
            timer.scheduleAtFixedRate(task, delay, delay);
        }
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

        public void schedule(TimerTask task) throws InterruptedException {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            inLatch.await();
                            resetIn();
                        } catch (InterruptedException e) {
                            System.err.println("scheduled task was interrupted: " + e);
                        }
                        task.run();
                        outLatch.countDown();
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

    public void sendGossip(GossipAction.ActionType actionType, String target, String node) throws IOException {
        GossipAction action = piggybackMessages(new GossipAction(actionType, this.nodeId, target));
        sendWithClient(node, action);
    }

    public void sendGossip(GossipAction.ActionType actionType, String node) throws IOException {
        sendGossip(actionType, "", node);
    }

    private GossipAction piggybackMessages(GossipAction action) {
        int updatesLeft = updates.size();
        int msg = 0;
        while (msg < GossipAction.MAX_MESSAGES && updatesLeft > 0) {
            StateUpdate update = updates.pop();
            action.addMessage(msg++, update.message);

            update.incPiggybackCount();
            if (update.expired()) {
                updates.remove(update);
            } else {
                updates.add(update);
            }
            updatesLeft--;
            msg++;
        }

        return action;
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
            return piggybackCount > Math.log(subgroupSize);
        }
    }

    private void sendWithClient(String nodeId, GossipAction action) throws IOException {
        if (!clients.containsKey(nodeId)) {
            return;
        }
        clients.get(nodeId).sendMessage(action);
    }

}
