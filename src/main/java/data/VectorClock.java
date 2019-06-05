package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class VectorClock {
    private Map<String, Integer> nodeCounters;

    public VectorClock(Entry... nodeEntries) {
        this.nodeCounters = Map.ofEntries(nodeEntries);
    }

    public VectorClock(Map<String, Integer> nodeCounters) {
        this.nodeCounters = nodeCounters;
    }

    public Map<String, Integer> getNodeCounters() {
        return nodeCounters;
    }

    public VectorClock merge(VectorClock other) {
        Map<String, Integer> mergedCounters = new HashMap<String, Integer>();

        for (Entry entry : getNodeCounters().entrySet()) {
            if (!mergedCounters.containsKey(entry.getKey()) || mergedCounters.get(entry.getKey()) < (Integer) entry.getValue()) {
                mergedCounters.put((String) entry.getKey(), (Integer) entry.getValue());
            }
        }
        for (Entry entry : other.getNodeCounters().entrySet()) {
            if (!mergedCounters.containsKey(entry.getKey()) || mergedCounters.get(entry.getKey()) < (Integer) entry.getValue()) {
                mergedCounters.put((String) entry.getKey(), (Integer) entry.getValue());
            }
        }

        return new VectorClock(mergedCounters);
    }

    public Causality getCausality(VectorClock other) {
        boolean foundGreaterThan = false;
        boolean foundLessThan = false;

        Map<String, Integer> counters = getNodeCounters();
        Map<String, Integer> otherCounters = other.getNodeCounters();

        for (Entry entry : counters.entrySet()) {
            if (!otherCounters.containsKey(entry.getKey())) {
                foundGreaterThan = true;
                break;
            }
            if ((Integer) entry.getValue() > otherCounters.get(entry.getKey())) {
                foundGreaterThan = true;
                break;
            }
        }

        for (Entry entry : otherCounters.entrySet()) {
            if (!counters.containsKey(entry.getKey())) {
                foundLessThan = true;
                break;
            }
            if ((Integer) entry.getValue() > counters.get(entry.getKey())) {
                foundLessThan = true;
                break;
            }
        }

        if (foundGreaterThan && foundLessThan) {
            return Causality.UNRELATED;
        }

        if (foundGreaterThan) {
            return Causality.CAUSED_BY;
        }

        if (foundLessThan) {
            return Causality.CAUSED;
        }

        return Causality.EQUAL;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");

        int i = 0;
        for (Entry entry : getNodeCounters().entrySet()) {
            buf.append(entry.getKey() + ": " + entry.getValue());
            if (i < getNodeCounters().size() - 1) {
                buf.append(", ");
            }
            i++;
        }
        buf.append("}");
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VectorClock vc = (VectorClock) o;
        if (!getNodeCounters().equals(vc.getNodeCounters())) {
            return false;
        }
        return true;
    }

    public enum Causality {
        EQUAL, CAUSED, CAUSED_BY, UNRELATED
    }
}
