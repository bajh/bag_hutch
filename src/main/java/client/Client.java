package client;

import data.Record;

import java.util.concurrent.CompletableFuture;

public interface Client {
    public CompletableFuture<Record> get(byte[] key);
    public CompletableFuture<Void> put(byte[] key, String context);
}
