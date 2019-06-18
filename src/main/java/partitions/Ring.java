package partitions;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import partitions.Node;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Ring {

    private BigInteger maxOffset = BigInteger.valueOf(2).pow(129);

    private List<Token> tokens = new ArrayList<>();

    private KeyHasher keyHasher = new MD5Hasher();

    // q is the number of partitions that the hash space should be divided into
    // q must divide evenly into 2^129
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

    public Ring(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<Node> getNodes(int n, String key) throws DigestException {
        int coordinatorIx = getCoordinatorIx(key.getBytes());

        List<Node> nodes = new ArrayList<>();
        for (int i = coordinatorIx; i - coordinatorIx < n; i++) {
            Node node = tokens.get(i % tokens.size()).getNode();
            if (node.getStatus() != Node.Status.READY) {
                continue;
            }

            nodes.add(node);
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
            } catch (NoSuchAlgorithmException e) {
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
