package client;

import data.Context;
import data.Record;

import java.util.concurrent.CompletableFuture;

public interface Client {
    public CompletableFuture<Record> get(String key);
    public CompletableFuture<Context> put(String key, String value, Context context);
}
