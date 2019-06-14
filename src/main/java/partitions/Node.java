package partitions;

import client.Client;

public class Node {
    private String id;
    private Status status = Status.READY;

    public Node(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public enum Status {
        STARTING, READY, SUSPICIOUS, DOWN, REMOVED
    }
}
