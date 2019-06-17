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
                "node0", clients, new HashMap<>());

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
                "node0", clients, new HashMap<>());

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

    // Unsuccessful ping
    @Test
    public void testSendUnsuccessfulPing() throws Exception {
        // The gossipScheduler should round robin pings to the nodes in the cluster

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
                "node0", clients, nodes);

        failureDetector.run();
        gossipScheduler.countDown();
        gossipScheduler.await();

        timeoutScheduler.countDown();
        timeoutScheduler.await();

        assertEquals(Node.Status.SUSPICIOUS, nodes.get("node1").getStatus());
    }


    // Successful ping - no ping_reqs
    @Test
    public void testSendSuccessfulPing() throws Exception {
        // The gossipScheduler should round robin pings to the nodes in the cluster

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
                "node0", clients, nodes);

        failureDetector.run();
        gossipScheduler.countDown();
        gossipScheduler.await();


        GossipAction action = new GossipAction(GossipAction.ActionType.ACK, "node1", "node1");
        failureDetector.handleAction(action);

        assertEquals(Node.Status.READY, nodes.get("node1").getStatus());
    }


    //@Test
    //public void testPingWithMessagesToPiggyback() throws Exception {
    //    // Create mock clients
    //    Map<String, GossipClient> clients = Map.ofEntries(
    //            entry("node1", Mockito.mock(GossipClient.class)),
    //            entry("node2", Mockito.mock(GossipClient.class)),
    //            entry("node3", Mockito.mock(GossipClient.class))
    //    );

    //    FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
    //    FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

    //    FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
    //            "node0", clients);

    //    GossipAction action = new GossipAction(GossipAction.ActionType.PING, "node2", "");
    //    failureDetector.handleAction(action);

    //    ArgumentCaptor<GossipAction> captor = ArgumentCaptor.forClass(GossipAction.class);
    //    Mockito.verify(clients.get("node2")).sendMessage(captor.capture());
    //    assertEquals(new GossipAction(GossipAction.ActionType.ACK, "node0", "node0"), captor.getValue());
    //}

    //@Test
    //public void testPingWithPiggybackedMessages() throws Exception {
    //    // Create mock clients
    //    Map<String, GossipClient> clients = Map.ofEntries(
    //            entry("node1", Mockito.mock(GossipClient.class)),
    //            entry("node2", Mockito.mock(GossipClient.class)),
    //            entry("node3", Mockito.mock(GossipClient.class))
    //    );

    //    FailureDetector.LatchScheduler gossipScheduler = new FailureDetector.LatchScheduler();
    //    FailureDetector.LatchScheduler timeoutScheduler = new FailureDetector.LatchScheduler();

    //    FailureDetector failureDetector = new FailureDetector(gossipScheduler, timeoutScheduler,
    //            "node0", clients);

    //    GossipAction action = new GossipAction(GossipAction.ActionType.PING, "node2", "");
    //    failureDetector.handleAction(action);

    //    ArgumentCaptor<GossipAction> captor = ArgumentCaptor.forClass(GossipAction.class);
    //    Mockito.verify(clients.get("node2")).sendMessage(captor.capture());
    //    assertEquals(new GossipAction(GossipAction.ActionType.ACK, "node0", "node0"), captor.getValue());
    //}
}
