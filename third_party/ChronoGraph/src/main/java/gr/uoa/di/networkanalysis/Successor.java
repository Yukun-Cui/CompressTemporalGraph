package gr.uoa.di.networkanalysis;

public class Successor {

	protected int neighbor;
	protected long timestamp;
	
	public Successor(int neighbor, long timestamp) {
		super();
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
