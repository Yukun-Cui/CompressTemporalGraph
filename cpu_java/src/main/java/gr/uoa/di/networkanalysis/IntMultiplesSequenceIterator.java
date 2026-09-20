package gr.uoa.di.networkanalysis;

import java.util.Objects;

import it.unimi.dsi.webgraph.LazyIntIterator;

/*
 * Copyright (C) 2003-2019 Paolo Boldi and Sebastiano Vigna
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

/** Returns each value in {@code values} the corresponding number of times. */
public final class IntMultiplesSequenceIterator implements LazyIntIterator {

    private final int[] values;
    private final int[] lengths;
    private int remainingRuns;
    private int currentRun;
    private int currentIndex;

    public IntMultiplesSequenceIterator(final int[] values, final int[] lengths) {
        this(values, lengths, values != null ? values.length : 0);
    }

    public IntMultiplesSequenceIterator(
            final int[] values, final int[] lengths, final int numberOfRuns) {
        this.values = Objects.requireNonNull(values, "values");
        this.lengths = Objects.requireNonNull(lengths, "lengths");
        if (values.length != lengths.length) {
            throw new IllegalArgumentException("values and lengths must have the same size");
        }
        if (numberOfRuns < 0 || numberOfRuns > values.length) {
            throw new IllegalArgumentException("numberOfRuns is outside the array bounds");
        }
        for (int index = 0; index < numberOfRuns; index++) {
            if (lengths[index] < 1) {
                throw new IllegalArgumentException("run lengths must be positive");
            }
        }
        remainingRuns = numberOfRuns;
    }

    private void advance() {
        remainingRuns--;
        if (remainingRuns != 0) {
            currentRun++;
        }
        currentIndex = 0;
    }

    @Override
    public int nextInt() {
        if (remainingRuns == 0) {
            return -1;
        }

        final int next = values[currentRun];
        currentIndex++;
        if (currentIndex == lengths[currentRun]) {
            advance();
        }
        return next;
    }

    @Override
    public int skip(final int numberOfElements) {
        if (numberOfElements < 0) {
            throw new IllegalArgumentException("numberOfElements must not be negative");
        }

        int skipped = 0;
        while (skipped < numberOfElements && remainingRuns != 0) {
            final int remainingInRun = lengths[currentRun] - currentIndex;
            if (numberOfElements - skipped < remainingInRun) {
                currentIndex += numberOfElements - skipped;
                return numberOfElements;
            }
            skipped += remainingInRun;
            advance();
        }
        return skipped;
    }
}
