package client;

import data.Record;

import java.util.concurrent.CompletableFuture;

public class HttpClient implements Client {
    public CompletableFuture<Record> get(byte[] key) {
        CompletableFuture result = new CompletableFuture();
        result.completeExceptionally(new Exception("not implemented"));
        return result;
    }

    public CompletableFuture<Void> put(byte[] key, String context) {
        CompletableFuture result = new CompletableFuture();
        result.completeExceptionally(new Exception("not implemented"));
        return result;
    }
}


