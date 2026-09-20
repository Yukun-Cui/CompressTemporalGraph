package gr.uoa.di.networkanalysis.utils;

import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.Test;

public class EpochUtilsTest {

    @Test
    public void shouldComputeDifference() {

        long timestamp = 1162422000L;

        Assert.assertEquals(0, EpochUtils.getDifference(timestamp, timestamp, 1, ChronoUnit.DAYS));
        Assert.assertEquals(0, EpochUtils.getDifference(timestamp, timestamp + 1, 1, ChronoUnit.DAYS));
        Assert.assertEquals(-1, EpochUtils.getDifference(timestamp, timestamp + 86400, 1, ChronoUnit.DAYS));
        Assert.assertEquals(1, EpochUtils.getDifference(timestamp + 86400, timestamp, 1, ChronoUnit.DAYS));
        Assert.assertEquals(0, EpochUtils.getDifference(timestamp + 86400, timestamp, 2, ChronoUnit.DAYS));

        Assert.assertEquals(0, EpochUtils.getDifference(timestamp, timestamp, 1, ChronoUnit.HOURS));
        Assert.assertEquals(0, EpochUtils.getDifference(timestamp, timestamp + 1, 1, ChronoUnit.HOURS));
        Assert.assertEquals(-24, EpochUtils.getDifference(timestamp, timestamp + 86400, 1, ChronoUnit.HOURS));
        Assert.assertEquals(24, EpochUtils.getDifference(timestamp + 86400, timestamp, 1, ChronoUnit.HOURS));
        Assert.assertEquals(12, EpochUtils.getDifference(timestamp + 86400, timestamp, 2, ChronoUnit.HOURS));

        Assert.assertEquals(0, EpochUtils.getDifference(timestamp, timestamp, 1, ChronoUnit.MINUTES));
        Assert.assertEquals(0, EpochUtils.getDifference(timestamp, timestamp + 1, 1, ChronoUnit.MINUTES));
        Assert.assertEquals(-1440, EpochUtils.getDifference(timestamp, timestamp + 86400, 1, ChronoUnit.MINUTES));
        Assert.assertEquals(1440, EpochUtils.getDifference(timestamp + 86400, timestamp, 1, ChronoUnit.MINUTES));
        Assert.assertEquals(720, EpochUtils.getDifference(timestamp + 86400, timestamp, 2, ChronoUnit.MINUTES));

        Assert.assertEquals(0, EpochUtils.getDifference(timestamp, timestamp, 1, ChronoUnit.SECONDS));
        Assert.assertEquals(-1, EpochUtils.getDifference(timestamp, timestamp + 1, 1, ChronoUnit.SECONDS));
        Assert.assertEquals(-86400, EpochUtils.getDifference(timestamp, timestamp + 86400, 1, ChronoUnit.SECONDS));
        Assert.assertEquals(86400, EpochUtils.getDifference(timestamp + 86400, timestamp, 1, ChronoUnit.SECONDS));
        Assert.assertEquals(43200, EpochUtils.getDifference(timestamp + 86400, timestamp, 2, ChronoUnit.SECONDS));

    }

    @Test
    public void shouldComputeAggregation() {

        long timestamp = 1162422000L;

        Assert.assertEquals(13453, EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.DAYS));
        Assert.assertEquals(13453, EpochUtils.getAggregationFromTimestamp(timestamp + 1, 1, ChronoUnit.DAYS));
        Assert.assertEquals(13453 + 1, EpochUtils.getAggregationFromTimestamp(timestamp + 86400, 1, ChronoUnit.DAYS));
        Assert.assertEquals(6726, EpochUtils.getAggregationFromTimestamp(timestamp, 2, ChronoUnit.DAYS));
        Assert.assertEquals(6726, EpochUtils.getAggregationFromTimestamp(timestamp - 86400, 2, ChronoUnit.DAYS));


        Assert.assertEquals(322895, EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.HOURS));
        Assert.assertEquals(322895, EpochUtils.getAggregationFromTimestamp(timestamp + 1, 1, ChronoUnit.HOURS));
        Assert.assertEquals(322895 + 24, EpochUtils.getAggregationFromTimestamp(timestamp + 86400, 1, ChronoUnit.HOURS));
        Assert.assertEquals(161447, EpochUtils.getAggregationFromTimestamp(timestamp, 2, ChronoUnit.HOURS));
        Assert.assertEquals(161447 - 12, EpochUtils.getAggregationFromTimestamp(timestamp - 86400, 2, ChronoUnit.HOURS));


        Assert.assertEquals(19373700, EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.MINUTES));
        Assert.assertEquals(19373700, EpochUtils.getAggregationFromTimestamp(timestamp + 1, 1, ChronoUnit.MINUTES));
        Assert.assertEquals(19373700 + 24 * 60, EpochUtils.getAggregationFromTimestamp(timestamp + 86400, 1, ChronoUnit.MINUTES));
        Assert.assertEquals(9686850, EpochUtils.getAggregationFromTimestamp(timestamp, 2, ChronoUnit.MINUTES));
        Assert.assertEquals(9686850 - 12 * 60, EpochUtils.getAggregationFromTimestamp(timestamp - 86400, 2, ChronoUnit.MINUTES));


        Assert.assertEquals(timestamp, EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.SECONDS));
        Assert.assertEquals(timestamp + 1, EpochUtils.getAggregationFromTimestamp(timestamp + 1, 1, ChronoUnit.SECONDS));
        Assert.assertEquals(timestamp + 86400, EpochUtils.getAggregationFromTimestamp(timestamp + 86400, 1, ChronoUnit.SECONDS));
        Assert.assertEquals(timestamp / 2, EpochUtils.getAggregationFromTimestamp(timestamp, 2, ChronoUnit.SECONDS));
        Assert.assertEquals((timestamp - 86400) / 2, EpochUtils.getAggregationFromTimestamp(timestamp - 86400, 2, ChronoUnit.SECONDS));


    }

    @Test
    public void shouldRestoreTimestamp() {

        long timestamp = 1162422000L;

        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.DAYS), 1, ChronoUnit.DAYS), 24 * 60 * 60);
        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.HOURS), 1, ChronoUnit.HOURS), 60 * 60);
        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.MINUTES), 1, ChronoUnit.MINUTES), 60);
        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 1, ChronoUnit.SECONDS), 1, ChronoUnit.SECONDS));

        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 15, ChronoUnit.DAYS), 15, ChronoUnit.DAYS), 15* 24 * 60 * 60);
        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 15, ChronoUnit.HOURS), 15, ChronoUnit.HOURS), 15* 60 * 60);
        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 15, ChronoUnit.MINUTES), 15, ChronoUnit.MINUTES), 15 * 60);
        Assert.assertEquals(timestamp, EpochUtils.getTimestampFromAggregation(EpochUtils.getAggregationFromTimestamp(timestamp, 15, ChronoUnit.SECONDS), 15, ChronoUnit.SECONDS), 15);


    }

}
