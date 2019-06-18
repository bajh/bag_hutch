package partitions;

import java.security.DigestException;
import java.util.List;

import client.Client;
import data.Context;
import data.Record;
import util.Either;
import util.Futures;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class Coordinator {
    private String nodeId;
    private int n;
    private int w;
    private int r;
    private Ring ring;
    private Map<String, Client> clients;

    public Coordinator(String nodeId, int n, int r, int w, Ring ring, Map<String, Client> clients) {
        this.nodeId = nodeId;
        this.setRing(ring);
        this.n = n;
        this.r = r;
        this.w = w;
        this.clients = clients;
    }

    private Client getClient(String nodeId) {
        return this.clients.get(nodeId);
    }

    // contact n replicas on which data is stored
    // when the results come back, check if we got enough successes and complete future
    public CompletableFuture<Record> get(String key) throws DigestException {
        List<Node> nodes = ring.getNodes(r, key);
        if (nodes.size() < r) {
            CompletableFuture future = new CompletableFuture();
            future.completeExceptionally(new Exception("not enough healthy nodes to complete read"));
            return future;
        }

        List<CompletableFuture<Record>> futures = nodes.stream()
                .map((node) -> getClient(node.getId()).get(key))
                .collect(Collectors.toList());

        // TODO: retries
        // TODO: timeouts
        // TODO: when a node needs to be updated because its vector clock was overridden by another, update it
        CompletableFuture combinedResult = Futures.awaitN(futures, r).thenApply(resultSummary -> {
            Record record = null;

            if (!resultSummary.successful) {
                throw new RuntimeException("not enough successful reads: " + r + " required");
            }

            for (Either<Record, Exception> result : resultSummary.results) {
                if (result == null) {
                    continue;
                }

                if (result.isLeft) {
                    if (record == null) {
                        record = result.getLeft();
                    } else {
                        record = record.combine(result.getLeft());
                    }

                }
            }

            return record;
        });

        return combinedResult;
    }

    public CompletableFuture<Context> put(String key, String value, Context context) throws DigestException {

        List<Node> nodes = ring.getNodes(n, key);
        if (nodes.size() < w) {
            CompletableFuture future = new CompletableFuture();
            future.completeExceptionally(new Exception("not enough healthy nodes to complete write"));
            return future;
        }

        Node coordinatingNode = getCoordinatingNode(nodes);

        if (coordinatingNode == null) {
            return getClient(nodes.get(0).getId()).route(key, value, context);
        }

        Context preWriteContext = new Context(context.getVectorClock().withNextCounter(nodeId));
        return getClient(coordinatingNode.getId()).put(key, value, preWriteContext).thenCompose(postWriteContext -> {
            List<CompletableFuture<Context>> futures = nodes.stream()
                .filter(node -> !node.getId().equals(nodeId))
                .map(node -> getClient(node.getId()).put(key, value, postWriteContext))
                .collect(Collectors.toList());

            if (n == 1 || w - 1 == 0) {
                CompletableFuture<Context> f = new CompletableFuture<>();
                f.complete(postWriteContext);
                return f;
            }

            return Futures.awaitN(futures, w - 1).thenApply(resultSummary -> {
                if (!resultSummary.successful) {
                    // TODO: unsure if this should be a different type of exception
                    throw new RuntimeException("not enough successful writes: " + w + " required");
                }

                Context nextContext = null;

                for (Either<Context, Exception> result : resultSummary.results) {
                    if (result == null) {
                        continue;
                    }

                    if (result.isLeft) {
                        nextContext = result.getLeft();
                    } else {
                        nextContext = new Context(context.getVectorClock().merge(result.getLeft().getVectorClock()));
                    }
                }

                return nextContext;
            });
        });

    }

    private Node getCoordinatingNode(List<Node> nodes) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    public void setRing(Ring ring) {
        this.ring = ring;
    }
}
