package client;

import data.Context;
import data.Record;

import java.util.concurrent.CompletableFuture;

public interface Client {
    public CompletableFuture<Record> get(String key);
    public CompletableFuture<Context> put(String key, String value, Context context);
    // TODO: This isn't quite right, the LocalClient can't really route but the RemoteClient can so should rethink this design
    public CompletableFuture<Context> route(String key, String value, Context context);
}
