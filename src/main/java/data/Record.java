package data;

import static data.VectorClock.Causality.*;

public class Record {
    public String[] versions;
    public VectorClock vectorClock;

    public String[] getVersions() {
        return this.versions;
    }

    public VectorClock getClock() {
        return vectorClock;
    }

    public Record() {
        this.versions = null;
        this.vectorClock = new VectorClock();
    }

    public Record(String data, VectorClock vectorClock) {
        this.versions = new String[]{data};
        this.vectorClock = vectorClock;
    }

    public Record(String[] data, VectorClock vectorClock) {
        this.versions = data;
        this.vectorClock = vectorClock;
    }

    public Record combine(Record other) {
        if (other == null) {
            return this;
        }

        switch (getClock().getCausality(other.getClock())) {
            case EQUAL:
                return this;
            case CAUSED:
                return other;
            case CAUSED_BY:
                return this;
            case UNRELATED:
                String[] combinedData = new String[getVersions().length + other.getVersions().length];
                System.arraycopy(getVersions(), 0, combinedData, 0, getVersions().length);
                System.arraycopy(other.getVersions(), 0, combinedData, getVersions().length, other.getVersions().length);
                return new Record(combinedData, getClock().merge(other.getClock()));
        }
        return null;
    }

    // TODO: I think the fact that this returns a Record maybe implies it's non-destructive
    // so perhaps it should either not use fluid-interface style OR should clone Record
    public Record withNextVectorClock(String nodeId) {
        vectorClock = getClock().withNextCounter(nodeId);
        return this;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer("versions: [");
        for (int i = 0; i < versions.length; i++) {
            result.append("{version " + i + ": ");
            result.append(new String(versions[i]));
            if (i < versions.length - 1) {
                result.append(", ");
            }
        }
        result.append("}, ");
        result.append(" vector clock: [");
        result.append(getClock().toString());
        result.append("]");

        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Record r = (Record) o;
        if (!getClock().equals(((Record) o).getClock())) {
            return false;
        }

        for (String a : getVersions()) {
            boolean foundMatch = false;
            for (String b : r.getVersions()) {
                if (a.equals(b)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }

        return true;
    }
}
