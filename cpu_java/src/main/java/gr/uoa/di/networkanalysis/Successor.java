package gr.uoa.di.networkanalysis;

/** A temporal outgoing edge. */
public final class Successor {

    private final int neighbor;
    private final long timestamp;

    public Successor(final int neighbor, final long timestamp) {
        this.neighbor = neighbor;
        this.timestamp = timestamp;
    }

    public int getNeighbor() {
        return neighbor;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
