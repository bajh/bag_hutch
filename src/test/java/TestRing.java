import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRing {
    private static class StaticKeyHasher implements Ring.KeyHasher {
        BigInteger n;

        protected StaticKeyHasher(BigInteger n) {
            this.n = n;
        }

        public BigInteger hashKey(byte[] key) {
            return n;
        }
    }

    // TODO: test that an InvalidQValueException is thrown if q is does not divide evently into 2^129
    @Test
    public void testGetNodes1() throws Exception {
        byte[] keyHash = {0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        Ring.KeyHasher keyHasher = new StaticKeyHasher(new BigInteger(keyHash));
        Node node1 = new Node("node1");
        Node node2 = new Node("node2");
        Node node3 = new Node("node3");
        Node node4 = new Node("node4");
        Node node5 = new Node("node5");
        Node node6 = new Node("node6");
        Node node7 = new Node("node7");
        Node node8 = new Node("node8");

        List<Node> allNodes = new ArrayList(Arrays.asList(
            node1,
            node2,
            node3,
            node4,
            node5,
            node6,
            node7,
            node8
        ));

        Ring ring = new Ring(32, allNodes);
        ring.setKeyHasher(keyHasher);
        List<Node> nodes = ring.getNodes(5, "snuds".getBytes());
        assertEquals(nodes, new ArrayList<Node>(Arrays.asList(node8, node1, node2, node3, node4)));
    }
}
