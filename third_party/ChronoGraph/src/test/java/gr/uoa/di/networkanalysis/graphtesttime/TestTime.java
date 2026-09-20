package gr.uoa.di.networkanalysis.graphtesttime;

import java.util.PrimitiveIterator.OfInt;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import gr.uoa.di.networkanalysis.EvolvingMultiGraph;
import gr.uoa.di.networkanalysis.EvolvingMultiGraph.SuccessorIterator;

public class TestTime {

	private static final Random random = new Random();

	private static final int aggregation = 1;
	private static int k = 2;

	//Flickr
//	private static int firstLabel = 1;
//	private static int lastLabel = 2_302_925;
//	private static final String graphFile = "out.flickr-growth-sorted.gz";
//	private static final String basename = "flickr";
//	private static boolean headers = true;
//	private static String sampleFile = "flickr-sample.txt";

	//Wiki
//	private static int firstLabel = 1;
//	private static int lastLabel = 3819691;
//	private static final String graphFile = "out.edit-enwiki.sorted.gz";
//	private static final String basename = "wiki";
//	private static boolean headers = true;
//	private static String sampleFile = "wiki-sample.txt";

	//Yahoo
//	private static int firstLabel = 1;
//	private static int lastLabel = 40616537;
//	private static final String graphFile = "yahoo-G5-sorted.tsv.gz";
//	private static final String basename = "yahoo";
//	private static boolean headers = false;
//	private static String sampleFile = "flickr-sample-head.txt";
//	private static String sampleFile = "flickr-sample-tail.txt";

	//cbtComm
	private static int firstLabel = 0;
	private static int lastLabel = 9999;
	private static final String graphFile = "cbtComm-sorted.txt.gz";
	private static final String basename = "cbtComm";
	private static boolean headers = false;
	private static String sampleFile = "cbtComm-sample.txt";

	//cbtPow
//	private static int firstLabel = 0;
//	private static int lastLabel = 999997;
//	private static final String graphFile = "/home/panagiotis/eclipse-photon/workspace/evolving-graph-compression/cbtPow-sorted.txt.gz";
//	private static final String basename = "cbtPow";
//	private static boolean headers = false;
//	private static String sampleFile = "cbtPow-sample.txt";

    //cbtWiki-Links-sub
//    private static int firstLabel = 0;
//    private static int lastLabel = 50768028;
//    private static final String graphFile = "/hdd/evolving-graph-compression/datasets/ready/wiki-ready-12-sorted.gz";
//    private static final String basename = "wiki-sub-base";
//    private static boolean headers = false;
//    private static String sampleFile = "wiki-links-sub-sample.txt";

//    private static int firstLabel = 0;
//    private static int lastLabel = 5076802;
//    private static final String graphFile = "/hdd/evolving-graph-compression/datasets/ready/wiki-links-sorted.gz";
//    private static final String basename = "wiki-links-base";
//    private static boolean headers = false;
//    private static String sampleFile = "wiki-links-sample.txt";

//    private static int firstLabel = 0;
//    private static int lastLabel = 103661224;
//    private static final String graphFile = "/hdd/evolving-graph-compression/datasets/ready/yahoo-all-sorted.gz";
//    private static final String basename = "yahoo-all-base";
//    private static boolean headers = false;
//    private static String sampleFile = "yahoo-all-sample.txt";

//    private static int firstLabel = 0;
//    private static int lastLabel = 40616537;
//    private static final String graphFile = "/hdd/evolving-graph-compression/datasets/ready/yahoo-G5-sorted.tsv.gz";
//    private static final String basename = "yahoo-G5-base";
//    private static boolean headers = false;
//    private static String sampleFile = "yahoo-sample-head.txt";

        @Before
        public void createCompressedGraph() throws IOException, InterruptedException {
            ClassLoader classLoader = getClass().getClassLoader();
            String graphFileResourcePath = classLoader.getResource(graphFile).getPath();
            EvolvingMultiGraph emg = new EvolvingMultiGraph(graphFileResourcePath, headers, k, basename, aggregation);
            emg.store();
        }

	@Test
	public void computeFullRetrievalOfNeighborsForRandomNodesTime() throws Exception {

		ClassLoader classLoader = getClass().getClassLoader();
		String graphFileResourcePath = classLoader.getResource(graphFile).getPath();
		
		EvolvingMultiGraph emg = new EvolvingMultiGraph(
				graphFileResourcePath,
				headers,
				k,
				basename,
				aggregation
		);

		emg.load();

		OfInt it = random.ints(1,lastLabel+1).iterator();

		long numIter = 1_000_000;

		long totalSum = 0;
		for(int i = 0; i < numIter; i++) {
			long tic = System.nanoTime();
			SuccessorIterator si = emg.successors(it.nextInt());
			while(true) {
				try {
					si.next();
				}
				catch(NoSuchElementException e) {
					break;
				}
			}
			long toc = System.nanoTime();
			totalSum += toc-tic;
		}

		System.out.println("Average time: "+ ((double)totalSum/numIter));
	}

	@Test
	public void testIsNeighborFromSample() throws Exception {

		ClassLoader classLoader = getClass().getClassLoader();
		String graphFileResourcePath = classLoader.getResource(graphFile).getPath();
		
		EvolvingMultiGraph emg = new EvolvingMultiGraph(
				graphFileResourcePath,
				headers,
				k,
				basename,
				aggregation
		);

		emg.load();

		String sampleFileResourcePath = classLoader.getResource(sampleFile).getPath();

		BufferedReader reader = new BufferedReader(new FileReader(sampleFileResourcePath));
		String line = null;
		int[] from = new int[1000];
		int[] to = new int[1000];
		int pos = 0;
		while((line = reader.readLine()) != null) {
			String[] splits = line.split("\\s");
			from[pos] = Integer.parseInt(splits[0]);
			to[pos] = Integer.parseInt(splits[1]);
			pos++;
		}
		reader.close();

		long totalSum = 0;
		long trueSum = 0;
		long trueCounter = 0;
		long falseSum = 0;
		long falseCounter = 0;

		for(int i = 0; i < 1000; i++) {
			for(int j = 0; j < from.length; j++) {
				int n1 = from[j];
				int n2 = to[j];
				long tic = System.nanoTime();
				boolean b = emg.isNeighbor(n1,  n2);
				long toc = System.nanoTime();

				totalSum += toc-tic;
				if(b) {
					trueSum += toc-tic;
					trueCounter++;
				}
				else {
					falseSum += toc-tic;
					falseCounter++;
				}
			}
		}

		System.out.println("Total time: "+totalSum);
		System.out.println("True average time: "+ (trueCounter == 0 ? "None" : (double)trueSum/trueCounter));
		System.out.println("False average time: "+ (falseCounter == 0 ? "None" : (double)falseSum/falseCounter));
	}

	@Test
	public void testIsNeighborNoRange() throws Exception {

		ClassLoader classLoader = getClass().getClassLoader();
		String graphFileResourcePath = classLoader.getResource(graphFile).getPath();
		
		EvolvingMultiGraph emg = new EvolvingMultiGraph(
				graphFileResourcePath,
				headers,
				k,
				basename,
				aggregation
		);

		emg.load();

		long numIter = 1_000_000;

		long totalSum = 0;
		long trueSum = 0;
		long trueCounter = 0;
		long falseSum = 0;
		long falseCounter = 0;

		for(int i = 0; i < numIter; i++) {
			int n1 = randInRangeInclusive(firstLabel, lastLabel);
			int n2 = randInRangeInclusive(firstLabel, lastLabel);
			long tic = System.nanoTime();
			boolean b = emg.isNeighbor(n1,  n2);
			long toc = System.nanoTime();

			totalSum += toc-tic;
			if(b) {
				trueSum += toc-tic;
				trueCounter++;
			}
			else {
				falseSum += toc-tic;
				falseCounter++;
			}
		}

		System.out.println("Total time: "+totalSum);
		System.out.println("True average time: "+ (trueCounter == 0 ? "None" : (double)trueSum/trueCounter));
		System.out.println("False average time: "+ (falseCounter == 0 ? "None" : (double)falseSum/falseCounter));
	}

	@Test
	public void testIsNeighborWithRange() throws Exception {

		ClassLoader classLoader = getClass().getClassLoader();
		String graphFileResourcePath = classLoader.getResource(graphFile).getPath();		
		
		EvolvingMultiGraph emg = new EvolvingMultiGraph(
				graphFileResourcePath,
				headers,
				k,
				basename,
				aggregation
		);

		emg.load();

		long minTimestamp = emg.getMinTimestamp();
		long maxTimestamp = System.currentTimeMillis()/1000;

		long numIter = 1_000_000;

		long totalSum = 0;
		long trueSum = 0;
		long trueCounter = 0;
		long falseSum = 0;
		long falseCounter = 0;

		for(int i = 0; i < numIter; i++) {
			int n1 = randInRangeInclusive(firstLabel, lastLabel);
			int n2 = randInRangeInclusive(firstLabel, lastLabel);
			long t1 = randInRangeInclusive((int) minTimestamp, (int) maxTimestamp);
			long t2 = randInRangeInclusive((int) t1, (int) maxTimestamp);
			long tic = System.nanoTime();
			boolean b = emg.isNeighbor( n1, n2, t1, t2);
			long toc = System.nanoTime();

			totalSum += toc-tic;
			if(b) {
				trueSum += toc-tic;
				trueCounter++;
			}
			else {
				falseSum += toc-tic;
				falseCounter++;
			}
		}

		System.out.println("Total time: "+totalSum);
		System.out.println("True average time: "+ (trueCounter == 0 ? "None" : (double)trueSum/trueCounter));
		System.out.println("False average time: "+ (falseCounter == 0 ? "None" : (double)falseSum/falseCounter));
	}

	public int randInRangeInclusive(int x, int y) {
		return random.nextInt((y - x) + 1) + x;
	}
}
