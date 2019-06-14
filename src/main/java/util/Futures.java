package util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.concurrent.CompletableFuture;

public class Futures {
    public static <T> CompletableFuture<ResultSummary<T>> awaitN(List<CompletableFuture<T>> futures, int firstN) {
        AtomicInteger nSuccessful = new AtomicInteger(0);
        int allowedErrors = futures.size() - firstN;
        AtomicInteger errorsLeft = new AtomicInteger(allowedErrors);
        List<Either<T, Exception>> results = new ArrayList<Either<T, Exception>>();
        while (results.size() < futures.size()) results.add(null);

        CompletableFuture<ResultSummary<T>> result = new CompletableFuture<ResultSummary<T>>();

        IntStream.range(0, futures.size()).forEach(i -> {
            futures.get(i).thenAccept(t -> {
                results.set(i, Either.leftValue(t));
                if (nSuccessful.incrementAndGet() >= firstN) {
                    result.complete(new ResultSummary(true, results));
                }
            }).exceptionally(e -> {
                results.set(i, Either.rightValue(e));
                if (errorsLeft.decrementAndGet() == 0) {
                    // TODO: think about this more, ideally this would result in the future completing exceptionally
                    // But if I complete with an exception, there isn't a way to give the caller access to the specific
                    // errors that took place because Exceptions can't hold generic types
                    // For now, the caller will have to just remember how many completions they asked for and check whether
                    // there are that many results
                    result.complete(new ResultSummary(false, results));
                }
                return null;
            });
        });

        return result;
    }

    public static class ResultSummary<T> {
        public boolean successful;
        public List<Either<T, Exception>> results;

        public ResultSummary(boolean successful, List<Either<T, Exception>> results) {
            this.successful = successful;
            this.results = results;
        }
    }

}
