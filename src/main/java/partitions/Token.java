package partitions;

import java.math.BigInteger;

public class Token {
    public BigInteger offset;
    private Node node;

    public Token(BigInteger offset, Node node) {
        this.offset = offset;
        this.node = node;
    }

    boolean hasHash(BigInteger hash) {
        // This partition has the hash if the offset is larger than the hash
        return this.offset.compareTo(hash) == 1;
    }

    public Node getNode() {
        return node;
    }
}
