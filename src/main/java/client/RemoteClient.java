package client;

import com.google.gson.Gson;
import data.Context;
import data.Record;
import partitions.Ring;
import server.bag_hutch_responses.GetResponse;
import server.bag_hutch_responses.InfoResponse;
import server.bag_hutch_responses.PutResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class RemoteClient implements Client {
    HttpClient client = HttpClient.newHttpClient();
    String host;

    public RemoteClient(String host) {
        this.host = host;
    }

    public CompletableFuture<Record> get(String key) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + "/internal?key=" + URLEncoder.encode(key)))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                        GetResponse getResponse = new Gson().fromJson(resp.body(), GetResponse.class);
                        Record record = new Record(getResponse.getValues(),
                                new Gson().fromJson(getResponse.getContext(), Context.class).getVectorClock());
                        return record;
                });
    }

    public CompletableFuture<Context> put(String key, String value, Context context) {
        PutRequest putRequest = new PutRequest(key, value, context);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + "/internal"))
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(putRequest)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            PutResponse putResponse = new Gson().fromJson(resp.body(), PutResponse.class);
            return new Gson().fromJson(putResponse.getContext(), Context.class);
        });
    }

    public CompletableFuture<Context> route(String key, String value, Context context) {
        PutRequest putRequest = new PutRequest(key, value, context);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + "/api"))
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(putRequest)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            PutResponse putResponse = new Gson().fromJson(resp.body(), PutResponse.class);
            Context ctx = new Gson().fromJson(putResponse.getContext(), Context.class);
            return ctx;
        });
    }

    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        return "Client{host=" + host + "}";
    }
}


