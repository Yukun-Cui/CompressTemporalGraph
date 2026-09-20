package gr.uoa.di.networkanalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastMultiByteArrayInputStream;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;
import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

public class EvolvingMultiGraph {

    protected String graphFile;
    protected boolean headers;
    protected int zetaK;
    protected String basename;
    protected long aggregationFactor;

    protected BVMultiGraph graph;
    protected EliasFanoMonotoneLongBigList efindex;
    protected byte[] timestamps;
    protected FastMultiByteArrayInputStream timestampsStream;
    protected long minTimestamp;

    LongArrayList offsetsIndex= null;
    private long currentOffset;

    public EvolvingMultiGraph(String graphFile, boolean headers, int zetaK, String basename, long aggregationFactor) {
        super();
        this.graphFile = graphFile;
        this.headers = headers;
        this.zetaK = zetaK;
        this.basename = basename;
        this.aggregationFactor = aggregationFactor;
    }

    protected long findMinimumTimestamp() {
        try (InputStream fileStream = new FileInputStream(graphFile);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
                BufferedReader buffered = new BufferedReader(decoder);
                ) {
            Long minTimestamp = null;
            minTimestamp = buffered.lines().mapToLong(line -> Long.parseLong(line.split("\t")[3])).min().orElse(0);
            return minTimestamp;
        } catch (IOException e) {
        }
        return 0;
    }

    protected long writeTimestampsToFile(List<Long> currentNeighborsTimestamps, OutputBitStream obs, long minTimestamp) throws IOException {

        // Returns the number of bits appended to the file
        long ret = 0;
        long previousNeighborTimestamp = minTimestamp;

        for(Long seconds: currentNeighborsTimestamps) {
            long periodsBetween = TimestampComparerAggregator.timestampsDifference(previousNeighborTimestamp, seconds, aggregationFactor);
            periodsBetween = Fast.int2nat(periodsBetween);
            previousNeighborTimestamp = seconds;
            ret += obs.writeLongZeta(periodsBetween, zetaK);
        }

        return ret;
    }

    public void store() throws IOException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(()-> {storeBVMultiGraph();});
        executorService.execute(()-> {storeTimestampsAndIndex();});
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public void storeBVMultiGraph() {
        try (InputStream fileStream = new FileInputStream(graphFile);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                ) {
            ArcListASCIIEvolvingGraph inputGraph = new ArcListASCIIEvolvingGraph(gzipStream, 0);
            BVMultiGraph.store(inputGraph, basename);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

    }

    public void storeTimestampsAndIndex() {

        // Aggregate the minimum timestamp of the file
        minTimestamp = TimestampComparerAggregator.aggregateMinTimestamp(findMinimumTimestamp(), aggregationFactor);

        try (InputStream fileStream = new FileInputStream(graphFile);
                GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
                InputStreamReader decoder = new InputStreamReader(gzipStream, "UTF-8");
                BufferedReader buffered = new BufferedReader(decoder);) {
            if(headers) {
                buffered.readLine();
            }
            // The file we will write the results to
            final OutputBitStream obs = new OutputBitStream(new FileOutputStream(basename+".timestamps"), 1024 * 1024);
            offsetsIndex= new LongArrayList();
            currentOffset = obs.writeLong(minTimestamp, 64);

            int currentNode = 0;
            ArrayList<Long> currentNeighborsTimestamps = new ArrayList<>();
            String line;

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            while ((line = buffered.readLine()) != null) {
                //String[] tokens = line.split("\\s+");
                String[] tokens = line.split("\t");
                int node = Integer.parseInt(tokens[0]);
                long timestamp = Long.parseLong(tokens[3]);

                int previous = currentNode;

                // If you find a new currentNode in the file, write the results you have so far about the current node.
                if(node != currentNode) {
                    executorService.submit(new TimeStampsWriter(currentNeighborsTimestamps, obs, node, previous));

                    // Prepare the variables for the next currentNode
                    currentNode = node;
                    currentNeighborsTimestamps = new ArrayList<Long>();
                    currentNeighborsTimestamps.add(timestamp);

                }
                else {
                    currentNeighborsTimestamps.add(timestamp);
                }
            }
            // Write the last node. It was not written because no change in node != currentNode was detected
            executorService.submit(new TimeStampsWriter(currentNeighborsTimestamps, obs, 0, 0));

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

            obs.close();
            buffered.close();

            // Perform compression of the index using EliasFano
            EliasFanoMonotoneLongBigList efmlbl = new EliasFanoMonotoneLongBigList(offsetsIndex);
            FileOutputStream fos = new FileOutputStream(basename+".efindex");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(efmlbl);
            oos.close();
            fos.close();

            offsetsIndex = null;
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }

    }

    public void load() throws Exception {
        // Graph
        graph = BVMultiGraph.load(basename);
        // EliasFano index
        FileInputStream fis = new FileInputStream(basename+".efindex");
        ObjectInputStream ois = new ObjectInputStream(fis);
        efindex = (EliasFanoMonotoneLongBigList) ois.readObject();
        ois.close();
        // Timestamps
        fis = new FileInputStream(basename+".timestamps");
        if(fis.getChannel().size() <= Integer.MAX_VALUE) {
            timestamps = new byte[(int) fis.getChannel().size()];
            BinIO.loadBytes(fis, timestamps);
            fis.close();
            InputBitStream ibs = new InputBitStream(timestamps);
            minTimestamp = ibs.readLong(64);
            ibs.close();
        } else {
            timestampsStream = new FastMultiByteArrayInputStream(fis, fis.getChannel().size());
            InputBitStream ibs = new InputBitStream(timestampsStream);
            minTimestamp = ibs.readLong(64);
            ibs.close();
        }

    }

    public boolean isNeighbor(int node, int neighbor) {
        LazyIntIterator it = graph.successors(node);
        int n = -1;
        while((n = it.nextInt()) != -1) {
			if(n == neighbor) return true;
			else if(n > neighbor) return false;
        }

        return false;
    }

    public boolean isNeighbor(int node, int neighbor, long t1, long t2) throws Exception {
        LazyIntIterator it = graph.successors(node);
        int n = -1, from = -1, to = -1, pos = 0;
        long t;
        // Find  the starting position
        while((n = it.nextInt()) != -1) {
            if(n == neighbor) {
                from = pos++;
                break;
            }
        }
        // Return false if it was not found at least once
        if(from == -1) {
            return false;
        }
        // Find the ending position
        while((n = it.nextInt()) == neighbor) {
            pos++;
        }
        to = pos-1;

        InputBitStream ibs;
        if (timestamps != null) {
            ibs = new InputBitStream(timestamps);
        } else {
            ibs = new InputBitStream(new FastMultiByteArrayInputStream(timestampsStream));
        }

        ibs.position(efindex.getLong(node));
        // Skip everything up to from
        long previous = minTimestamp;
        for(int i =0; i < from; i++) {
            t = Fast.nat2int(ibs.readLongZeta(zetaK));
            t = TimestampComparerAggregator.reverse(previous, t, aggregationFactor);
            previous = t;
        }
        // Scan all the timestamps in the range from->to
        for(int i = from; i <= to; i++) {
            t = Fast.nat2int(ibs.readLongZeta(zetaK));
            t = TimestampComparerAggregator.reverse(previous, t, aggregationFactor);
            if(t1 <= t && t <= t2) return true;
            previous = t;
        }

        ibs.close();

        return false;
    }

    public SuccessorIterator successors(int node) throws Exception {
        return new SuccessorIterator(node);
    }

    public class SuccessorIterator implements Iterator<Successor> {

        LazyIntIterator neighborsIterator;
        InputBitStream ibs;
        long previous;

        public SuccessorIterator(int node) throws Exception {
            neighborsIterator = graph.successors(node);
            if (timestamps != null) {
                ibs = new InputBitStream(timestamps);
            } else {
                ibs = new InputBitStream(new FastMultiByteArrayInputStream(timestampsStream));
            }
            long position = efindex.getLong(node);
            ibs.position(position);
            previous = minTimestamp;
        }

        @Override
        public boolean hasNext() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Successor next() throws NoSuchElementException {
            int neighbor = neighborsIterator.nextInt();
            if(neighbor == -1) {
                throw new NoSuchElementException();
            }
            long t;
            try {
                t = Fast.nat2int(ibs.readLongZeta(zetaK));
                t = TimestampComparerAggregator.reverse(previous, t, aggregationFactor);
                previous = t;
            }
            catch(IOException e) {
                throw new NoSuchElementException(e.toString());
            }
            return new Successor(neighbor, t);
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

    class TimeStampsWriter implements Runnable {


        private List<Long> currentNeighborsTimestamps;
        private OutputBitStream obs;
        private int node;
        private int previous;

        public TimeStampsWriter(List<Long> currentNeighborsTimestamps, OutputBitStream obs, int node, int previous) {
            this.currentNeighborsTimestamps = currentNeighborsTimestamps;
            this.obs = obs;
            this.node = node;
            this.previous = previous;
        }

        @Override
        public void run() {
            offsetsIndex.add(currentOffset);
            try {
                currentOffset += writeTimestampsToFile(currentNeighborsTimestamps, obs, minTimestamp);
            } catch (IOException e) {
            }
            // If at least one node was skipped, add the current offset to the index for each node in-between
            if(node > previous + 1) {
                for(int i = 0; i < node - previous - 1; i++) {
                    offsetsIndex.add(currentOffset);
                }
            }
        }

    }


}
