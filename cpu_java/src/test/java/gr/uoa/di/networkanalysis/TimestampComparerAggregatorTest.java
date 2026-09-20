package gr.uoa.di.networkanalysis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimestampComparerAggregatorTest {

    @Test
    public void shouldAggregateAndRestoreTimestampBuckets() {
        assertEquals(100L, TimestampComparerAggregator.aggregateMinTimestamp(109L, 10L));
        assertEquals(1L, TimestampComparerAggregator.timestampsDifference(100L, 115L, 10L));
        assertEquals(110L, TimestampComparerAggregator.reverse(100L, 1L, 10L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveAggregationFactor() {
        TimestampComparerAggregator.aggregateMinTimestamp(100L, 0L);
    }
}
