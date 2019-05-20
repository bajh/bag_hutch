import java.math.BigInteger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Ring {

    private BigInteger maxOffset = BigInteger.valueOf(2).pow(129);
    private List<Token> tokens = new ArrayList<Token>();
    private KeyHasher keyHasher = new MD5Hasher();

    // q is the number of partitions that the hash space should be divided into
    public Ring(int q, List<Node> nodes) throws InvalidQValueException {
        BigInteger bigQ = BigInteger.valueOf(q);
        BigInteger tokenSize = maxOffset.divide(bigQ);
        if (!maxOffset.mod(bigQ).equals(BigInteger.ZERO)) {
            throw new InvalidQValueException(bigQ);
        }

        int i = 0;
         for (BigInteger offset = tokenSize; offset.compareTo(maxOffset) < 1; offset = offset.add(tokenSize)) {
            tokens.add(new Token(offset, nodes.get(i % nodes.size())));
            i++;
        }
    }

    public List<Node> getNodes(int n, byte[] key) throws DigestException {
        int coordinatorIx = getCoordinatorIx(key);

        List<Node> nodes = new ArrayList<Node>();
        for (int i = coordinatorIx; i - coordinatorIx < n; i++) {
            nodes.add(tokens.get(i % tokens.size()).getNode());
        }

        return nodes;
    }

    private int getCoordinatorIx(byte[] key) throws DigestException {
        BigInteger hash = this.keyHasher.hashKey(key);
        for (int i = 0; i < this.tokens.size(); i++) {
            if (this.tokens.get(i).hasHash(hash)) {
                return i;
            }
        }
        return -1;
    }

    public void setKeyHasher(KeyHasher keyHasher) {
        this.keyHasher = keyHasher;
    }

    public static interface KeyHasher {
        BigInteger hashKey(byte []key) throws DigestException;
    }

    public static class MD5Hasher implements KeyHasher {
        public BigInteger hashKey(byte[] key) throws DigestException {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(key);
                byte[] buf = new byte[17];
                // Adding in 0 because BigInteger is in two's complement format
                buf[0] = 0;
                digest.digest(buf, 1, 16);
                return new BigInteger(buf);
            } catch (NoSuchAlgorithmException _) {
                // pass - this cannot happen because we know MD5 is an algorithm
                return BigInteger.ZERO;
            }
        }
    }

    public static class InvalidQValueException extends Exception {
        private BigInteger n;

        public InvalidQValueException(BigInteger n) {
            this.n = n;
        }
    }

}
