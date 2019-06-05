import client.Client;

public class Node {
    private String id;
    private Client client;

    Node(String id) {
        this.id = id;
    }

    Node(String id, Client client) {
        this.id = id;
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public enum Status {
        STARTING, READY, DOWN, REMOVED
    }
}
