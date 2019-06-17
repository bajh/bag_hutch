package gossip;

public interface FailureHandler {
    public void handleAlive(String nodeId);
    public void handleSuspect(String nodeId);
    public void handleFailure(String nodeId);
}
