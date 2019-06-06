package data;

public class Context {
    private VectorClock vectorClock;

    public Context(VectorClock vectorClock) {
        this.vectorClock = vectorClock;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public static Context empty() {
        return new Context(new VectorClock());
    }
}
