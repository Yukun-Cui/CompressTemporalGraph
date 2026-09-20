package gr.uoa.di.networkanalysis.graphtestsize;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;
import gr.uoa.di.networkanalysis.EvolvingMultiGraph;
import gr.uoa.di.networkanalysis.EvolvingMultiGraph.SuccessorIterator;
import gr.uoa.di.networkanalysis.Successor;

public class TestStore {

	// Flickr
//	private static final String graphFile =  "out.flickr-growth.sorted.gz";
//	private static final String basename =  "flickr";
//	private static final boolean headers = true;
//	private static final int k = 2;
//	private static int aggregation = 24*60*60;

	// Wiki
//	private static final String graphFile =  "out.edit-enwiki.sorted.gz";
//	private static final String basename =  "wiki";
//	private static final boolean headers = true;
//	private static final int k = 2;
//	private static int aggregation = 60*60;

	// Yahoo
//	private static final String graphFile =  "yahoo-G5-sorted.tsv.gz";
//	private static final String basename =  "yahoo";
//	private static final boolean headers = false;
//	private static final int k = 2;
//	private static int aggregation = 15*60;

	// cbtComm
	private static final String graphFile =  "cbtComm-sorted.txt.gz";
	private static final String basename =  "cbtComm";
	private static final boolean headers = false;
	private static final int k = 2;
	private static int aggregation = 1;

	// cbtPow
//	private static final String graphFile =  "cbtPow-sorted.txt.gz";
//	private static final String basename =  "cbtPow";
//	private static final boolean headers = false;
//	private static final int k = 2;
//	private static int aggregation = 1;

	
	@Test
	public void testStore() throws Exception {

		ClassLoader classLoader = getClass().getClassLoader();
		String graphFileResourcePath = classLoader.getResource(graphFile).getPath();

		EvolvingMultiGraph emg = new EvolvingMultiGraph(
				graphFileResourcePath,
				headers,
				k,
				basename,
				aggregation
		);
		
		long t1 = System.nanoTime();
		emg.store();
		long t2 = System.nanoTime();
		System.out.println("Compression took: " + (t2-t1) + " nanoseconds");
	}
	
	@Test
	public void assertEqualsFromOriginalFile() throws Exception {

		String graphFileResourcePath = Resources.getResource(graphFile).getPath();

		EvolvingMultiGraph emg = new EvolvingMultiGraph(
				graphFileResourcePath,
				headers,
				k,
				basename,
				aggregation
		);
		
		emg.load();

		FileInputStream fileStream = new FileInputStream(graphFileResourcePath);
        GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
        InputStreamReader decoder = new InputStreamReader(gzipStream, "UTF-8");
        BufferedReader buffered = new BufferedReader(decoder);

        int current = 0;
        String line;
        if(headers) {
        	buffered.readLine(); // Get rid of headers
		}
        ArrayList<Successor> list = new ArrayList<>();

        while ((line = buffered.readLine()) != null) {
        	String[] tokens = line.split("\\s+");
        	int node = Integer.parseInt(tokens[0]);
            int neighbor = Integer.parseInt(tokens[1]);
            long timestamp = Long.parseLong(tokens[3]);

            if(node == current) {
            	list.add(new Successor(neighbor, timestamp));
            }
            else {
            	// Check the list so far
            	SuccessorIterator it = emg.successors(current);
            	int i = 0;
        		while(true) {
        			try {
        				Successor s = it.next();
        				Assert.assertEquals(s.getTimestamp(), list.get(i).getTimestamp(), aggregation);
        				i++;
        			}
        			catch(NoSuchElementException e) {
        				break;
        			}
        		}

            	list = new ArrayList<Successor>();
            	list.add(new Successor(neighbor, timestamp));
            	current = node;
            }
        }

        SuccessorIterator it = emg.successors(current);
    	int i = 0;
		while(true) {
			try {
				Successor s = it.next();
				Assert.assertEquals(s.getTimestamp(), list.get(i).getTimestamp(), aggregation);
				i++;
			}
			catch(NoSuchElementException e) {
				break;
			}

		}
	}
}
