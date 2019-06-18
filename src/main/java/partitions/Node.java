package partitions;

import client.Client;

public class Node {
    private String id;
    private Status status = Status.READY;
    private int incarnation = 0;

    public Node(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getIncarnation() {
        return incarnation;
    }

    public void setIncarnation(int incarnation) {
        this.incarnation = incarnation;
    }

    public enum Status {
        STARTING, READY, SUSPECT, DOWN, REMOVED
    }
}
