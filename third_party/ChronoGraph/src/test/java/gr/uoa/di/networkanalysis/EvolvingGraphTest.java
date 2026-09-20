package gr.uoa.di.networkanalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import java.time.temporal.ChronoUnit;

import org.junit.Assert;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongBigListIterator;
import it.unimi.dsi.io.OutputBitStream;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;
import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.BVGraph;

public class EvolvingGraphTest {

	public long findMinimumTimestamp(String graphFile) throws IOException {
		InputStream fileStream = new FileInputStream(graphFile);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
        BufferedReader buffered = new BufferedReader(decoder);
        
        // Skip the header of the file
        buffered.readLine();
        
        Long minTimestamp = null;
        String line;
        while ((line = buffered.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            long timestamp = Long.parseLong(tokens[3]);
            if(minTimestamp == null || timestamp < minTimestamp) {
            	minTimestamp = timestamp;
            }
        }
        
        buffered.close();
        
        return minTimestamp;
	}
	
	public LocalDate unixEpochToLocalDate(long seconds) {
		
		LocalDateTime ldt = LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);

		return LocalDate.of(ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth());

	}
	
	public long writeTimestampsToFile(int[] neighbors, int outDegree, ArrayList<Long> currentNeighborsTimestamps/*Map<Integer,ArrayList<Long>> currentNeighborsWithTimestamps*/, OutputBitStream obs, Instant minInstant) throws IOException {

		// Returns the number of bits appended to the file
		
		// The first timestamp is written with respect to difference
		// in days from the minimum timestamp in the file
		// The rest are written with respect to difference from the previous in the row
		long ret = 0;
		
		Instant previousNeighborInstant = minInstant;
		
//		for(int i=0; i < outDegree; i++) {
//			ArrayList<Long> secondsList = currentNeighborsWithTimestamps.get(neighbors[i]);
//			for(Long seconds: secondsList) {
//				Instant currentNeighborInstant = Instant.ofEpochSecond(seconds);
//				long periodsBetween = Duration.between(previousNeighborInstant, currentNeighborInstant).toMinutes()/15;
//				periodsBetween = Fast.int2nat(periodsBetween);
//				previousNeighborInstant = currentNeighborInstant;
//	    		//String tmp = Long.toString(daysBetween);
//				ret += obs.writeLongZeta(periodsBetween, 2 /*BVGraph.DEFAULT_ZETA_K*/);
//			}
//		}
		
		for(Long seconds: currentNeighborsTimestamps) {
			Instant currentNeighborInstant = Instant.ofEpochSecond(seconds);
			long periodsBetween = Duration.between(previousNeighborInstant, currentNeighborInstant).toMinutes()/15;
			periodsBetween = Fast.int2nat(periodsBetween);
			previousNeighborInstant = currentNeighborInstant;
    		//String tmp = Long.toString(daysBetween);
			ret += obs.writeLongZeta(periodsBetween, 2 /*BVGraph.DEFAULT_ZETA_K*/);
		}
		
		return ret;
	}
	
	public void store() throws IOException {

		String graphFile = "out.edit-enwiki.sorted.gz";
		
		InputStream fileStream = new FileInputStream(graphFile);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
        BufferedReader buffered = new BufferedReader(decoder);
        File tempFile = File.createTempFile("temp", ".txt");
        tempFile.deleteOnExit();

        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        String line = buffered.readLine();
        while ((line = buffered.readLine()) != null) {
            String[] splits = line.split("\\s");
            writer.write(String.format("%s\t%s\n", splits[0], splits[1]));
        }
        writer.close();
        buffered.close();

        String basename = "edit-enwiki";

        ArcListASCIIGraph inputGraph = new ArcListASCIIGraph(new FileInputStream(tempFile), 0);
        BVMultiGraph.store(inputGraph, basename);
        BVMultiGraph bvgraph = BVMultiGraph.load(basename);
        System.out.println(bvgraph.numNodes());
        System.out.println(bvgraph.numArcs());
        
        // Find the minimum timestamp in the file
        long minTimestamp = findMinimumTimestamp(graphFile);
        Instant minInstant = Instant.ofEpochSecond(minTimestamp);
        
        fileStream = new FileInputStream(graphFile);
        gzipStream = new GZIPInputStream(fileStream);
        decoder = new InputStreamReader(gzipStream, "UTF-8");
        buffered = new BufferedReader(decoder);
        
        // Skip the header of the file
        buffered.readLine();
        // The file we will write the results to 
        //writer = new BufferedWriter(new FileWriter("timestamps1.txt"));
        final OutputBitStream obs = new OutputBitStream(new FileOutputStream("timestamps1.txt"), 1024 * 1024);
        // Maintain an index of positions in the file for each node -> timestamps line
        // The number of nodes is known beforehand, set initial capacity accordingly
        LongArrayList offsetsIndex= new LongArrayList();
        long currentOffset = 0L;
        String tmp = Long.toString(minTimestamp);
        currentOffset += obs.writeLong(minTimestamp, 64);
        //writer.write(tmp + "\n");
        // Start reading the file
        int currentNode = 1;
        //Map<Integer,ArrayList<Long>> currentNeighborsWithTimestamps = new HashMap<Integer,ArrayList<Long>>(); // neighbor -> timestamp
        ArrayList<Long> currentNeighborsTimestamps = new ArrayList<Long>();
        while ((line = buffered.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            int node = Integer.parseInt(tokens[0]);
            int neighbor = Integer.parseInt(tokens[1]);
            long timestamp = Long.parseLong(tokens[3]);

            int previous = currentNode;
            
            // If you find a new currentNode in the file, write the results you have so far about the current node.
            if(node != currentNode) {
            	// First get the sequence of neighbors from BVGraph
            	int[] neighbors = bvgraph.successorArray(currentNode);
            	int outDegree = bvgraph.outdegree(currentNode); // successorArray might return extra results, only the [0,outDegree) range is valid
            	offsetsIndex.add(currentOffset);
            	currentOffset += writeTimestampsToFile(neighbors, outDegree, currentNeighborsTimestamps, obs, minInstant);
            	
            	// Prepare the variables for the next currentNode
            	currentNode = node;
            	//currentNeighborsWithTimestamps = new HashMap<Integer,ArrayList<Long>>();
            	currentNeighborsTimestamps = new ArrayList<Long>();
            	//ArrayList<Long> L = new ArrayList<Long>();
            	//L.add(timestamp);
            	currentNeighborsTimestamps.add(timestamp);
            	//currentNeighborsWithTimestamps.put(neighbor, L);
            	
            	// If at least one node was skipped, add that many empty lines to the file and update the index accordingly
                if(node > previous + 1) {
                	for(int i = 0; i < node - previous - 1; i++) {
                		offsetsIndex.add(currentOffset);
                		//writer.append("\n");
                		//obs.writeBit(0);
                		//currentOffset += 1;
                	}
                }
            	
            }
            else {
//            	if(currentNeighborsWithTimestamps.containsKey(neighbor)) {
//            		currentNeighborsWithTimestamps.get(neighbor).add(timestamp);
//            	}
//            	else {
//            		ArrayList<Long> L = new ArrayList<Long>();
//                	L.add(timestamp);
//                	currentNeighborsWithTimestamps.put(neighbor, L);
//            	}
            	currentNeighborsTimestamps.add(timestamp);
            }
        }
        // Write the last node. It was not written because no change in node != currentNode was detected
        int[] neighbors = bvgraph.successorArray(currentNode);
    	int outDegree = bvgraph.outdegree(currentNode);
    	offsetsIndex.add(currentOffset);
    	currentOffset += writeTimestampsToFile(neighbors, outDegree, currentNeighborsTimestamps, obs, minInstant);
        
        
        obs.close();
        buffered.close();
        
        System.out.println(String.format("Index size: %d", offsetsIndex.size()));
        System.out.println(offsetsIndex.getLong(offsetsIndex.size() - 1));

        // Perform compression of the index using EliasFano
        EliasFanoMonotoneLongBigList efmlbl = new EliasFanoMonotoneLongBigList(offsetsIndex);
        FileOutputStream fos = new FileOutputStream("eliasfano");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(efmlbl);
        oos.close();
        fos.close();
        fos = new FileOutputStream("simplelist");
        oos = new ObjectOutputStream(fos);
        oos.writeObject(offsetsIndex);
        oos.close();
        fos.close();
        System.out.println("EliasFano number of bits: " + efmlbl.numBits());
        Assert.assertTrue(efmlbl.size64() < 4 * 8 * offsetsIndex.size());
        Assert.assertEquals(offsetsIndex.size(), efmlbl.size64());
	}
}
