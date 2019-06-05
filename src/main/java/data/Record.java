package data;

import java.util.Arrays;

import static data.VectorClock.Causality.*;

public class Record {
    byte[][] versions;
    VectorClock vectorClock;

    public byte[][] getVersions() {
        return this.versions;
    }

    public VectorClock getClock() {
        return vectorClock;
    }

    public Record(byte[][] data, VectorClock vectorClock) {
        this.versions = data;
        this.vectorClock = vectorClock;
    }

    public Record(byte[] data, VectorClock vectorClock) {
        this.versions = new byte[][]{data};
        this.vectorClock = vectorClock;
    }

    public Record(String[] data, VectorClock vectorClock) {
        this.versions = new byte[data.length][];
        for (int i = 0; i < data.length; i++) {
            this.versions[i] = data[i].getBytes();
        }
        this.vectorClock = vectorClock;
    }

    public Record(String data, VectorClock vectorClock) {
        this.versions = new byte[][]{data.getBytes()};
        this.vectorClock = vectorClock;
    }

    public Record combine(Record other) {
        switch (getClock().getCausality(other.getClock())) {
            case EQUAL:
                return this;
            case CAUSED:
                return other;
            case CAUSED_BY:
                return this;
            case UNRELATED:
                byte[][] combinedData = new byte[getVersions().length + other.getVersions().length][];
                System.arraycopy(getVersions(), 0, combinedData, 0, getVersions().length);
                System.arraycopy(other.getVersions(), 0, combinedData, getVersions().length, other.getVersions().length);
                return new Record(combinedData, getClock().merge(other.getClock()));
        }
        return null;
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

        for (byte[] a : getVersions()) {
            boolean foundMatch = false;
            for (byte[] b : r.getVersions()) {
                if (Arrays.equals(a, b)) {
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
