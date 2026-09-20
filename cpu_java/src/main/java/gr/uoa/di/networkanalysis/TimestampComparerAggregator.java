package gr.uoa.di.networkanalysis;

/** Timestamp bucketing helpers used by the compressed timestamp stream. */
public final class TimestampComparerAggregator {

    private TimestampComparerAggregator() {}

    public static long timestampsDifference(
            final long firstTimestamp, final long secondTimestamp, final long factor) {
        requirePositiveFactor(factor);
        return secondTimestamp / factor - firstTimestamp / factor;
    }

    public static long reverse(
            final long previousTimestamp, final long difference, final long factor) {
        requirePositiveFactor(factor);
        return previousTimestamp + factor * difference;
    }

    public static long aggregateMinTimestamp(final long minimumTimestamp, final long factor) {
        requirePositiveFactor(factor);
        return minimumTimestamp / factor * factor;
    }

    private static void requirePositiveFactor(final long factor) {
        if (factor < 1) {
            throw new IllegalArgumentException("factor must be positive");
        }
    }
}
