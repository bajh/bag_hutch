import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TestFutures {
    
    @Test
    public void testAwaitN() {
        List<CompletableFuture<Integer>> futures = Arrays.asList(
            new CompletableFuture<Integer>(),
            new CompletableFuture<Integer>(),
            new CompletableFuture<Integer>()
        );

        CompletableFuture<List<Either<Integer, Exception>>> result = Futures.awaitN(futures, 2);

        futures.get(1).complete(10);
        futures.get(2).complete(25);

        List<Either<Integer, Exception>> results = result.join();

        assertEquals(Arrays.asList(null, Either.leftValue(10), Either.leftValue(25)), results);

        futures = Arrays.asList(
                new CompletableFuture<Integer>(),
                new CompletableFuture<Integer>(),
                new CompletableFuture<Integer>()
        );

        result = Futures.awaitN(futures, 2);

        futures.get(0).completeExceptionally(new Exception("i died"));
        futures.get(1).complete(10);
        futures.get(2).completeExceptionally(new Exception("\uD83D\uDCA3...\uD83D\uDCA5"));

        results = result.join();

        assertEquals(results.get(0).getRight().getMessage(), "java.lang.Exception: i died");
        assertEquals(Either.leftValue(10), results.get(1));
        assertEquals(results.get(2).getRight().getMessage(), "java.lang.Exception: \uD83D\uDCA3...\uD83D\uDCA5");
    }
}
