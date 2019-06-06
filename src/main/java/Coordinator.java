import java.io.IOException;
import java.security.DigestException;
import java.util.List;
import data.Context;
import data.Record;

import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class Coordinator {
    private String nodeId;
    private int n;
    private int w;
    private int r;
    private Ring ring;

    public Coordinator(String nodeId, int n, int r, int w, Ring ring) {
        this.nodeId = nodeId;
        this.ring = ring;
        this.n = n;
        this.r = r;
        this.w = w;
    }

    // contact n replicas on which data is stored
    // when the results come back, check if we got enough successes and complete future
    public CompletableFuture<Record> get(String key) throws IOException, DigestException {
        List<Node> nodes = ring.getNodes(r, key);
        List<CompletableFuture<Record>> futures = nodes.stream()
                .map((node) -> node.getClient().get(key))
                .collect(Collectors.toList());

        // TODO: retries
        // TODO: timeouts
        // TODO: when a node fails to respond, mark it as down
        // TODO: when a node needs to be updated because its vector clock was overridden by another, update it
        CompletableFuture combinedResult = Futures.awaitN(futures, r).thenApply(resultSummary -> {
            Record record = null;

            if (!resultSummary.successful) {
                throw new RuntimeException("not enough successful reads: " + r + " required");
            }

            for (Either<Record, Exception> result : resultSummary.results) {
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

    public CompletableFuture<Void> put(String key, String value, Context context) throws IOException, DigestException {
        List<Node> nodes = ring.getNodes(n, key);
        List<CompletableFuture<Context>> futures = nodes.stream()
                .map((node) -> node.getClient().put(key, value, context))
                .collect(Collectors.toList());

        return Futures.awaitN(futures, w).thenAccept(resultSummary -> {
            if (!resultSummary.successful) {
                // TODO: unsure if this should be a different type of exception
                throw new RuntimeException("not enough successful writes: " + w + " required");
            }
        });
    }
}
