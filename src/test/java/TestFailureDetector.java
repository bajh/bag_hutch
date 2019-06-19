import gossip.FailureDetector;
import gossip.GossipAction;
import gossip.GossipClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import partitions.Node;

import java.util.*;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

public class TestFailureDetector {

    @Test
    public void testPing() throws Exception {
        // Create mock clients
        SortedMap<String, GossipClient> clients = new TreeMap(Map.ofEntries(
                entry("node1", Mockito.mock(GossipClient.class)),
                entry("node2", Mockito.mock(GossipClient.class)),
                entry("node3", Mockito.mock(GossipClient.class))
        ));

        FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
        FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

        FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
                "node0", 2, clients, new HashMap<>());

        GossipAction action = new GossipAction(GossipAction.ActionType.PING, "node2", "");
        failureDetector.handleAction(action);

        ArgumentCaptor<GossipAction> captor = ArgumentCaptor.forClass(GossipAction.class);
        Mockito.verify(clients.get("node2")).sendMessage(captor.capture());
        assertEquals(new GossipAction(GossipAction.ActionType.ACK, "node0", "node0"), captor.getValue());
    }

    @Test
    public void testSendPing() throws Exception {
        // The gossipScheduler should round robin pings to the nodes in the cluster

        SortedMap<String, GossipClient> clients = new TreeMap(Map.ofEntries(
                entry("node1", Mockito.mock(GossipClient.class)),
                entry("node2", Mockito.mock(GossipClient.class))
        ));

        FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
        FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

        FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
                "node0", 2, clients, new HashMap<>());

        failureDetector.run();
        gossipScheduler.countDown();
        gossipScheduler.await();

        ArgumentCaptor<GossipAction> captor = ArgumentCaptor.forClass(GossipAction.class);
        Mockito.verify(clients.get("node1")).sendMessage(captor.capture());
        assertEquals(new GossipAction(GossipAction.ActionType.PING, "node0", ""), captor.getValue());

        gossipScheduler.countDown();
        gossipScheduler.await();

        captor = ArgumentCaptor.forClass(GossipAction.class);
        Mockito.verify(clients.get("node2")).sendMessage(captor.capture());
        assertEquals(new GossipAction(GossipAction.ActionType.PING, "node0", ""), captor.getValue());

        gossipScheduler.countDown();
        gossipScheduler.await();

        Mockito.verify(clients.get("node1"), times(2)).sendMessage(captor.capture());
        assertEquals(new GossipAction(GossipAction.ActionType.PING, "node0", ""), captor.getValue());
    }

    @Test
    public void testSendUnsuccessfulPing() throws Exception {

        SortedMap<String, GossipClient> clients = new TreeMap(Map.ofEntries(
                entry("node1", Mockito.mock(GossipClient.class)),
                entry("node2", Mockito.mock(GossipClient.class)),
                entry("node3", Mockito.mock(GossipClient.class)),
                entry("node4", Mockito.mock(GossipClient.class))
        ));

        Map<String, Node> nodes = Map.ofEntries(
                entry("node1", new Node("node1")),
                entry("node2", new Node("node2")),
                entry("node3", new Node("node3")),
                entry("node4", new Node("node4"))
        );

        FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
        FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

        FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
                "node0", 2, clients, nodes);

        // First, cause node0 to send a ping to node1 (the first in its list)
        failureDetector.run();
        gossipScheduler.countDown();
        gossipScheduler.await();

        // Wait for the ack timeout to elapse
        timeoutScheduler.countDown();
        timeoutScheduler.await();

        // Ping reqs will have been sent to try to confirm the node is dead
        // At this point, the node should still be alive
        assertEquals(Node.Status.READY, nodes.get("node1").getStatus());

        // Now wait for the ping reqs to timeout too
        timeoutScheduler.countDown();
        timeoutScheduler.await();

        // Now the node should be suspect
        assertEquals(Node.Status.SUSPECT, nodes.get("node1").getStatus());

        // Now the next outgoing message (e.g. the next outgoing ping) should include a piggybacked message indicating node1 is suspicious
        gossipScheduler.countDown();
        gossipScheduler.await();

        ArgumentCaptor<GossipAction> captor = ArgumentCaptor.forClass(GossipAction.class);
        Mockito.verify(clients.get("node2")).sendMessage(captor.capture());
        assertEquals(new GossipAction(GossipAction.ActionType.PING, "node0", "",
                Arrays.asList(new GossipAction.Message(GossipAction.MessageType.SUSPECT, 0, "node1"))), captor.getValue());
    }

    @Test
    public void testSendUnsuccessfulPingFollowedBySuccessfulPingReq() throws Exception {

        SortedMap<String, GossipClient> clients = new TreeMap(Map.ofEntries(
                entry("node1", Mockito.mock(GossipClient.class)),
                entry("node2", Mockito.mock(GossipClient.class)),
                entry("node3", Mockito.mock(GossipClient.class)),
                entry("node4", Mockito.mock(GossipClient.class))
        ));

        Map<String, Node> nodes = Map.ofEntries(
                entry("node1", new Node("node1")),
                entry("node2", new Node("node2")),
                entry("node3", new Node("node3")),
                entry("node4", new Node("node4"))
        );

        FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
        FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

        FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
                "node0", 2, clients, nodes);

        // node0 sends a ping to node1 (the first in its list)
        failureDetector.run();
        gossipScheduler.countDown();
        gossipScheduler.await();

        // Wait for the ack timeout to elapse
        timeoutScheduler.countDown();
        timeoutScheduler.await();

        // Now, ping reqs will have been sent
        // At this point, the node should still be alive
        assertEquals(Node.Status.READY, nodes.get("node1").getStatus());

        // node2 informs us that it was able to reach node1
        GossipAction action = new GossipAction(GossipAction.ActionType.ACK, "node2", "node1");
        failureDetector.handleAction(action);

        // Now wait for the ping reqs to timeout too
        timeoutScheduler.countDown();
        timeoutScheduler.await();

        // The node should still be considered alive despite the initial failure
        assertEquals(Node.Status.READY, nodes.get("node1").getStatus());
    }

    @Test
    public void testSendSuccessfulPing() throws Exception {
        SortedMap<String, GossipClient> clients = new TreeMap(Map.ofEntries(
                entry("node1", Mockito.mock(GossipClient.class)),
                entry("node2", Mockito.mock(GossipClient.class)),
                entry("node3", Mockito.mock(GossipClient.class)),
                entry("node4", Mockito.mock(GossipClient.class))
        ));

        Map<String, Node> nodes = Map.ofEntries(
                entry("node1", new Node("node1")),
                entry("node2", new Node("node2")),
                entry("node3", new Node("node3")),
                entry("node4", new Node("node4"))
        );

        FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
        FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

        FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
                "node0", 2, clients, nodes);

        failureDetector.run();
        gossipScheduler.countDown();
        gossipScheduler.await();

        GossipAction action = new GossipAction(GossipAction.ActionType.ACK, "node1", "node1");
        failureDetector.handleAction(action);

        assertEquals(Node.Status.READY, nodes.get("node1").getStatus());
    }

    @Test
    public void testPingReq() throws Exception {
        SortedMap<String, GossipClient> clients = new TreeMap(Map.ofEntries(
                entry("node1", Mockito.mock(GossipClient.class)),
                entry("node2", Mockito.mock(GossipClient.class)),
                entry("node3", Mockito.mock(GossipClient.class)),
                entry("node4", Mockito.mock(GossipClient.class))
        ));

        Map<String, Node> nodes = Map.ofEntries(
                entry("node1", new Node("node1")),
                entry("node2", new Node("node2")),
                entry("node3", new Node("node3")),
                entry("node4", new Node("node4"))
        );

        FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
        FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

        FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
                "node0", 2, clients, nodes);

        // We get a ping req from node1! It's asking us to check on node2
        GossipAction action = new GossipAction(GossipAction.ActionType.PING_REQ, "node1", "node2");
        failureDetector.handleAction(action);

        // Ping REQ should be sent to node2 now... meanwhile we're waiting on the ack...
        ArgumentCaptor<GossipAction> captor = ArgumentCaptor.forClass(GossipAction.class);
        Mockito.verify(clients.get("node2")).sendMessage(captor.capture());
        assertEquals(new GossipAction(GossipAction.ActionType.PING, "node0", "",
                new ArrayList<>()), captor.getValue());

        // The ack arrives...
        action = new GossipAction(GossipAction.ActionType.ACK, "node2", "node2");
        failureDetector.handleAction(action);

        // Now we let the timeout complete - this should have no effect since we're already received an ack
        timeoutScheduler.countDown();
        timeoutScheduler.await();

        // Our requester node (node1) should have been passed the ack on behalf of node2
        captor = ArgumentCaptor.forClass(GossipAction.class);
        Mockito.verify(clients.get("node1")).sendMessage(captor.capture());
        assertEquals(new GossipAction(GossipAction.ActionType.ACK, "node0", "node2",
                new ArrayList<>()), captor.getValue());

    }
}
