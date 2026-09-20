package gr.uoa.di.networkanalysis;

public class TimestampComparerAggregator {

	
	public static long timestampsDifference(long t1, long t2, long factor) {
		// TODO: Check for a better way to do this. For min timestamps as well!!!
		return t2/factor-t1/factor;
	}

	public static long reverse(long previous, long difference, long factor) {
		return previous + factor*difference;
	}

	public static long aggregateMinTimestamp(long min, long factor) {
		return (min/factor)*factor;
	}

}
