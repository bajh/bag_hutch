import gossip.Encoder;
import gossip.GossipAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEncoder {
    @Test
    public void testEncoder() throws Exception {
        GossipAction action = new GossipAction(GossipAction.ActionType.PING, "node1",
                "node3", new gossip.GossipAction.Message[]{});

        byte[] encoded = Encoder.encode(action);
        GossipAction decoded = Encoder.decode(encoded);
        assertEquals(action, decoded);

        GossipAction.Message message = new GossipAction.Message(GossipAction.MessageType.ALIVE, 2, "node4");
        action = new GossipAction(GossipAction.ActionType.ACK, "node2", "node3",
                new gossip.GossipAction.Message[]{message});

        encoded = Encoder.encode(action);
        decoded = Encoder.decode(encoded);
        assertEquals(action, decoded);
    }
}
