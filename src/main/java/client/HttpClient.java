package client;

import data.Context;
import data.Record;

import java.util.concurrent.CompletableFuture;

public class HttpClient implements Client {
    public CompletableFuture<Record> get(String key) {
        CompletableFuture result = new CompletableFuture();
        result.completeExceptionally(new Exception("not implemented"));
        return result;
    }

    public CompletableFuture<Context> put(String key, String value, Context context) {
        CompletableFuture result = new CompletableFuture();
        result.completeExceptionally(new Exception("not implemented"));
        return result;
    }
}


