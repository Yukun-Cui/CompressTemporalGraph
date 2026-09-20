package gr.uoa.di.networkanalysis.utils;

import java.time.temporal.ChronoUnit;

public class EpochUtils {

	public static long getDifference(long a, long b, int aggregation, ChronoUnit cu) {
		if (aggregation < 1) {
			aggregation = 1;
		}
		switch (cu) {
		case DAYS:
			return (a - b) / 24 / 60 / 60 / aggregation;
		case HOURS:
			return (a - b) / 60 / 60 / aggregation;
		case MINUTES:
			return (a - b) / 60 / aggregation;
		case SECONDS:
		default:
			return (a - b) / aggregation;
		}
	}

	public static long getAggregationFromTimestamp(long timestamp, int aggregation, ChronoUnit cu) {
		if (aggregation < 1) {
			aggregation = 1;
		}
		switch (cu) {
		case DAYS:
			return timestamp / 24 / 60 / 60 / aggregation;
		case HOURS:
			return timestamp / 60 / 60 / aggregation;
		case MINUTES:
			return timestamp / 60 / aggregation;
		case SECONDS:
		default:
			return timestamp / aggregation;
		}
	}

	public static long getTimestampFromAggregation(long aggregatedTimestamp, int aggregation, ChronoUnit cu) {
		if (aggregation < 1) {
			aggregation = 1;
		}
		switch (cu) {
		case DAYS:
			return aggregatedTimestamp * 24 * 60 * 60 * aggregation;
		case HOURS:
			return aggregatedTimestamp * 60 * 60 * aggregation;
		case MINUTES:
			return aggregatedTimestamp * 60 * aggregation;
		case SECONDS:
		default:
			return aggregatedTimestamp * aggregation;
		}
	}
}
