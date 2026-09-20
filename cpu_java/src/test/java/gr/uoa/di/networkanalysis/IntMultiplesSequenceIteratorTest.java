package gr.uoa.di.networkanalysis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IntMultiplesSequenceIteratorTest {

    @Test
    public void shouldReturnEachValueTheRequestedNumberOfTimes() {
        final IntMultiplesSequenceIterator iterator =
                new IntMultiplesSequenceIterator(new int[] {3, 7}, new int[] {2, 3});

        assertEquals(3, iterator.nextInt());
        assertEquals(3, iterator.nextInt());
        assertEquals(7, iterator.nextInt());
        assertEquals(7, iterator.nextInt());
        assertEquals(7, iterator.nextInt());
        assertEquals(-1, iterator.nextInt());
    }

    @Test
    public void shouldSkipAcrossRuns() {
        final IntMultiplesSequenceIterator iterator =
                new IntMultiplesSequenceIterator(new int[] {3, 7}, new int[] {2, 3});

        assertEquals(3, iterator.skip(3));
        assertEquals(7, iterator.nextInt());
        assertEquals(7, iterator.nextInt());
        assertEquals(-1, iterator.nextInt());
    }
}
