package client;

import com.google.gson.Gson;
import data.Context;
import data.Record;
import store.Store;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LocalClient implements Client {

    String nodeId;
    Store store;

    public LocalClient(String nodeId, Store store) {
        this.nodeId = nodeId;
        this.store = store;
    }

    public CompletableFuture<Record> get(String key) {
        CompletableFuture result = new CompletableFuture();

        try {
            String data = store.get(key);
            Record record = new Gson().fromJson(data, Record.class);

            result.complete(record);
        } catch (IOException e) {
            result.completeExceptionally(e);
        }

        return result;
    }

    public CompletableFuture<Context> put(String key, String value, Context context) {
        return get(key).thenApply(previousRecord -> {
            Record newRecord = new Record(value, context.getVectorClock())
                    .combine(previousRecord);
            try {
                store.put(key, new Gson().toJson(newRecord));
                return new Context(newRecord.getClock());
            } catch (Exception e) {
                // TODO: maybe another type of unchecked exception here
                throw new RuntimeException("error saving record: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Context> route(String key, String value, Context context) {
        return put(key, value, context);
    }

    @Override
    public String toString() {
        return "LocalClient{nodeId=" + nodeId + "}";
    }

}
