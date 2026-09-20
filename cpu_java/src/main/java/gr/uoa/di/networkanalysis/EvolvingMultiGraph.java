package gr.uoa.di.networkanalysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastMultiByteArrayInputStream;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;
import it.unimi.dsi.webgraph.LazyIntIterator;

/** Stores a temporal multigraph as a WebGraph graph plus a compressed timestamp stream. */
public class EvolvingMultiGraph {

    private static final String EFINDEX_EXTENSION = ".efindex";
    private static final String TIMESTAMPS_EXTENSION = ".timestamps";
    private static final int TIMESTAMP_BUFFER_SIZE = 1024 * 1024;
    private static final int MIN_TIMESTAMP_BITS = Long.SIZE;

    private final String graphFile;
    private final boolean headers;
    private final int zetaK;
    private final String basename;
    private final long aggregationFactor;

    private BVMultiGraph graph;
    private EliasFanoMonotoneLongBigList efindex;
    private byte[] timestamps;
    private FastMultiByteArrayInputStream timestampsStream;
    private long minTimestamp;

    public EvolvingMultiGraph(
            final String graphFile,
            final boolean headers,
            final int zetaK,
            final String basename,
            final long aggregationFactor) {
        this.graphFile = Objects.requireNonNull(graphFile, "graphFile");
        this.headers = headers;
        if (zetaK < 1) {
            throw new IllegalArgumentException("zetaK must be positive");
        }
        this.zetaK = zetaK;
        this.basename = Objects.requireNonNull(basename, "basename");
        if (aggregationFactor < 1) {
            throw new IllegalArgumentException("aggregationFactor must be positive");
        }
        this.aggregationFactor = aggregationFactor;
    }

    private BufferedReader openGraphReader() throws IOException {
        return new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(graphFile)),
                        StandardCharsets.UTF_8));
    }

    private void skipHeader(final InputStream input) throws IOException {
        if (!headers) {
            return;
        }
        int value;
        do {
            value = input.read();
        } while (value != -1 && value != '\n');
    }

    private void skipHeader(final BufferedReader reader) throws IOException {
        if (headers) {
            reader.readLine();
        }
    }

    private static String[] columns(final String line, final long lineNumber) throws IOException {
        final String[] tokens = line.trim().split("\\s+");
        if (tokens.length < 4) {
            throw new IOException("Expected at least four columns at input line " + lineNumber);
        }
        return tokens;
    }

    private long findMinimumTimestamp() throws IOException {
        try (BufferedReader reader = openGraphReader()) {
            skipHeader(reader);
            long lineNumber = headers ? 1 : 0;
            long minimum = Long.MAX_VALUE;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                final String[] tokens = columns(line, lineNumber);
                try {
                    minimum = Math.min(minimum, Long.parseLong(tokens[3]));
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid timestamp at input line " + lineNumber, e);
                }
            }
            if (minimum == Long.MAX_VALUE) {
                throw new IOException("The input graph contains no edges: " + graphFile);
            }
            return minimum;
        }
    }

    protected long writeTimestampsToFile(
            final List<Long> currentNeighborTimestamps,
            final OutputBitStream output,
            final long minimumTimestamp) throws IOException {
        long writtenBits = 0;
        long previousTimestamp = minimumTimestamp;
        for (long timestamp : currentNeighborTimestamps) {
            final long difference = TimestampComparerAggregator.timestampsDifference(
                    previousTimestamp, timestamp, aggregationFactor);
            writtenBits += output.writeLongZeta(Fast.int2nat(difference), zetaK);
            previousTimestamp = timestamp;
        }
        return writtenBits;
    }

    /** Stores the graph structure and timestamp index concurrently. */
    public void store() throws IOException, InterruptedException {
        ensureOutputDirectory();
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Future<Void> graphFuture = executor.submit(() -> {
            storeBVMultiGraph();
            return null;
        });
        final Future<Void> timestampsFuture = executor.submit(() -> {
            storeTimestampsAndIndex();
            return null;
        });
        executor.shutdown();

        try {
            await(graphFuture);
            await(timestampsFuture);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(final Future<?> future) throws IOException, InterruptedException {
        try {
            future.get();
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IOException("Graph storage failed", cause);
        }
    }

    private void ensureOutputDirectory() throws IOException {
        final Path output = Paths.get(basename).toAbsolutePath().normalize();
        final Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    public void storeBVMultiGraph() throws IOException {
        ensureOutputDirectory();
        try (InputStream fileStream = new FileInputStream(graphFile);
                InputStream gzipStream = new GZIPInputStream(fileStream)) {
            skipHeader(gzipStream);
            final ArcListASCIIEvolvingGraph inputGraph =
                    new ArcListASCIIEvolvingGraph(gzipStream, 0);
            BVMultiGraph.store(inputGraph, basename);
        }
    }

    public void storeTimestampsAndIndex() throws IOException {
        ensureOutputDirectory();
        minTimestamp = TimestampComparerAggregator.aggregateMinTimestamp(
                findMinimumTimestamp(), aggregationFactor);

        final LongArrayList offsetsIndex = new LongArrayList();
        long currentOffset;
        int currentNode = -1;
        int maximumNode = -1;
        List<Long> currentTimestamps = new ArrayList<>();

        try (BufferedReader reader = openGraphReader();
                OutputBitStream output = new OutputBitStream(
                        new FileOutputStream(basename + TIMESTAMPS_EXTENSION),
                        TIMESTAMP_BUFFER_SIZE)) {
            skipHeader(reader);
            currentOffset = output.writeLong(minTimestamp, MIN_TIMESTAMP_BITS);
            long lineNumber = headers ? 1 : 0;
            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                final String[] tokens = columns(line, lineNumber);
                final int node;
                final int neighbor;
                final long timestamp;
                try {
                    node = Integer.parseInt(tokens[0]);
                    neighbor = Integer.parseInt(tokens[1]);
                    timestamp = Long.parseLong(tokens[3]);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid numeric value at input line " + lineNumber, e);
                }
                if (node < 0 || neighbor < 0) {
                    throw new IOException("Negative node identifier at input line " + lineNumber);
                }
                if (currentNode > node) {
                    throw new IOException("Source nodes must be sorted at input line " + lineNumber);
                }

                maximumNode = Math.max(maximumNode, Math.max(node, neighbor));
                if (currentNode == -1) {
                    appendOffsets(offsetsIndex, currentOffset, 0, node);
                    currentNode = node;
                } else if (node != currentNode) {
                    offsetsIndex.add(currentOffset);
                    currentOffset += writeTimestampsToFile(
                            currentTimestamps, output, minTimestamp);
                    appendOffsets(offsetsIndex, currentOffset, currentNode + 1, node);
                    currentTimestamps = new ArrayList<>();
                    currentNode = node;
                }
                currentTimestamps.add(timestamp);
            }

            if (currentNode == -1) {
                throw new IOException("The input graph contains no edges: " + graphFile);
            }
            offsetsIndex.add(currentOffset);
            currentOffset += writeTimestampsToFile(currentTimestamps, output, minTimestamp);
            appendOffsets(offsetsIndex, currentOffset, currentNode + 1, maximumNode + 1);
        }

        final EliasFanoMonotoneLongBigList compressedIndex =
                new EliasFanoMonotoneLongBigList(offsetsIndex);
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(basename + EFINDEX_EXTENSION))) {
            output.writeObject(compressedIndex);
        }
    }

    private static void appendOffsets(
            final LongArrayList offsets,
            final long offset,
            final int fromInclusive,
            final int toExclusive) {
        for (int node = fromInclusive; node < toExclusive; node++) {
            offsets.add(offset);
        }
    }

    public void load() throws IOException, ClassNotFoundException {
        graph = BVMultiGraph.load(basename);

        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(basename + EFINDEX_EXTENSION))) {
            efindex = (EliasFanoMonotoneLongBigList) input.readObject();
        }

        try (FileInputStream input = new FileInputStream(basename + TIMESTAMPS_EXTENSION)) {
            final long size = input.getChannel().size();
            if (size <= Integer.MAX_VALUE) {
                timestamps = new byte[(int) size];
                BinIO.loadBytes(input, timestamps);
                timestampsStream = null;
            } else {
                timestampsStream = new FastMultiByteArrayInputStream(input, size);
                timestamps = null;
            }
        }

        try (InputBitStream input = newTimestampInput()) {
            minTimestamp = input.readLong(MIN_TIMESTAMP_BITS);
        }
    }

    private void requireLoaded() {
        if (graph == null || efindex == null || (timestamps == null && timestampsStream == null)) {
            throw new IllegalStateException("Graph is not loaded");
        }
    }

    private InputBitStream newTimestampInput() {
        if (timestamps != null) {
            return new InputBitStream(timestamps);
        }
        if (timestampsStream != null) {
            return new InputBitStream(new FastMultiByteArrayInputStream(timestampsStream));
        }
        throw new IllegalStateException("Timestamp data is not loaded");
    }

    public boolean isNeighbor(final int node, final int neighbor) {
        requireLoaded();
        final LazyIntIterator iterator = graph.successors(node);
        int candidate;
        while ((candidate = iterator.nextInt()) != -1) {
            if (candidate == neighbor) {
                return true;
            }
            if (candidate > neighbor) {
                return false;
            }
        }
        return false;
    }

    public boolean isNeighbor(
            final int node,
            final int neighbor,
            final long fromTimestamp,
            final long toTimestamp) throws IOException {
        requireLoaded();
        final LazyIntIterator iterator = graph.successors(node);
        int candidate;
        int from = -1;
        int position = 0;

        while ((candidate = iterator.nextInt()) != -1) {
            if (candidate == neighbor) {
                from = position++;
                break;
            }
            position++;
        }
        if (from == -1) {
            return false;
        }

        while ((candidate = iterator.nextInt()) == neighbor) {
            position++;
        }
        final int to = position - 1;

        try (InputBitStream input = newTimestampInput()) {
            input.position(efindex.getLong(node));
            long previous = minTimestamp;
            for (int index = 0; index < from; index++) {
                final long difference = Fast.nat2int(input.readLongZeta(zetaK));
                previous = TimestampComparerAggregator.reverse(
                        previous, difference, aggregationFactor);
            }
            for (int index = from; index <= to; index++) {
                final long difference = Fast.nat2int(input.readLongZeta(zetaK));
                final long timestamp = TimestampComparerAggregator.reverse(
                        previous, difference, aggregationFactor);
                if (fromTimestamp <= timestamp && timestamp <= toTimestamp) {
                    return true;
                }
                previous = timestamp;
            }
            return false;
        }
    }

    public SuccessorIterator successors(final int node) throws IOException {
        requireLoaded();
        return new SuccessorIterator(node);
    }

    public final class SuccessorIterator implements Iterator<Successor>, AutoCloseable {

        private static final int UNREAD = Integer.MIN_VALUE;

        private final LazyIntIterator neighborsIterator;
        private final InputBitStream input;
        private long previousTimestamp;
        private int nextNeighbor = UNREAD;
        private boolean exhausted;
        private boolean closed;

        private SuccessorIterator(final int node) throws IOException {
            neighborsIterator = graph.successors(node);
            input = newTimestampInput();
            input.position(efindex.getLong(node));
            previousTimestamp = minTimestamp;
        }

        @Override
        public boolean hasNext() {
            if (!exhausted && nextNeighbor == UNREAD) {
                nextNeighbor = neighborsIterator.nextInt();
                if (nextNeighbor == -1) {
                    exhausted = true;
                    closeUnchecked();
                }
            }
            return !exhausted;
        }

        @Override
        public Successor next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final int neighbor = nextNeighbor;
            nextNeighbor = UNREAD;
            try {
                final long difference = Fast.nat2int(input.readLongZeta(zetaK));
                final long timestamp = TimestampComparerAggregator.reverse(
                        previousTimestamp, difference, aggregationFactor);
                previousTimestamp = timestamp;
                return new Successor(neighbor, timestamp);
            } catch (IOException e) {
                closeSilently();
                throw new UncheckedIOException("Cannot decode timestamp data", e);
            }
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                input.close();
            }
        }

        private void closeUnchecked() {
            try {
                close();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot close timestamp data", e);
            }
        }

        private void closeSilently() {
            try {
                close();
            } catch (IOException ignored) {
                // Preserve the original decoding failure.
            }
        }
    }

    public String getGraphFile() {
        return graphFile;
    }

    public boolean isHeaders() {
        return headers;
    }

    public int getZetaK() {
        return zetaK;
    }

    public String getBasename() {
        return basename;
    }

    public long getAggregationFactor() {
        return aggregationFactor;
    }

    public BVMultiGraph getGraph() {
        return graph;
    }

    public EliasFanoMonotoneLongBigList getEfindex() {
        return efindex;
    }

    public byte[] getTimestamps() {
        return timestamps;
    }

    public long getMinTimestamp() {
        return minTimestamp;
    }
}
