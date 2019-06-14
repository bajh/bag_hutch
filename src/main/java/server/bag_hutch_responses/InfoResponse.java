package server.bag_hutch_responses;

import data.Record;
import partitions.Node;
import store.LogRecord;

import java.util.List;
import java.util.Map;

public class InfoResponse {
    private String nodeId;
    private List<Node> nodes;
    private Map<String, Record> records;
    private List<LogRecord> log;

    public InfoResponse(String nodeId, List<Node> nodes, Map<String, Record> records, List<LogRecord> log) {
        this.setNodeId(nodeId);
        this.setNodes(nodes);
        this.setRecords(records);
        this.setLog(log);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Map<String, Record> getRecords() {
        return records;
    }

    public void setRecords(Map<String, Record> records) {
        this.records = records;
    }

    public List<LogRecord> getLog() {
        return log;
    }

    public void setLog(List<LogRecord> log) {
        this.log = log;
    }
}
