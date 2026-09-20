package gr.uoa.di.networkanalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

import org.junit.Assert;

public class BVMultiGraphTest {

    public void testWebGraphSize() throws IOException {

        String graph = "out.edit-enwiki-multi-graph.gz";

        InputStream fileStream = new FileInputStream(graph);
        //        InputStream fileStream = new FileInputStream("out.flickr-growth.sorted.gz");
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

        String basename = "test";

        ArcListASCIIGraph inputGraph = new ArcListASCIIGraph(new FileInputStream(tempFile), 0);
        BVMultiGraph.store(inputGraph, basename);
        BVMultiGraph bvMultiGraph = BVMultiGraph.load(basename);

//        Assert.assertEquals(52, bvMultiGraph.numNodes());
//        Assert.assertEquals(33, bvMultiGraph.numArcs());

//        System.out.println(Arrays.toString(bvMultiGraph.successorArray(1)));
//        System.out.println(Arrays.toString(bvMultiGraph.successorArray(2)));
//        System.out.println(Arrays.toString(bvMultiGraph.successorArray(3)));
//        System.out.println(Arrays.toString(bvMultiGraph.successorArray(4)));
//        System.out.println("SUC: " + Arrays.toString(bvMultiGraph.successorArray(9)));
//        System.out.println("SUC: " + Arrays.toString(bvMultiGraph.successorArray(10)));

        try (
                InputStream fileStream2 = new FileInputStream(graph);
                InputStream gzipStream2 = new GZIPInputStream(fileStream2);
                Reader decoder2 = new InputStreamReader(gzipStream2, "UTF-8");
                BufferedReader br = new BufferedReader(decoder2)){
            br.readLine();
            int node = -1;
            List<Integer> neighbors = null;
            while((line = br.readLine()) != null) {
                String[] splits = line.trim().split("\\s");
                int nodeA = Integer.parseInt(splits[0]);
                int nodeB = Integer.parseInt(splits[1]);
                if (nodeA != node) {
                    if (node != -1) {
                        if (neighbors.size() < 200) {
                            System.out.println("Node: " + node);
                            LazyIntIterator it = bvMultiGraph.successors(node);
                            int neighbor;
                            List<Integer> neighborsResult = new ArrayList<>();
                            while ((neighbor = it.nextInt()) != -1) {
                                neighborsResult.add(neighbor);
                            }
                            Assert.assertTrue(neighbors.containsAll(neighborsResult));
                            Assert.assertTrue(neighborsResult.containsAll(neighbors));
                        }
                    }
                    node = nodeA;
                    neighbors = new ArrayList<>();
                }
                neighbors.add(nodeB);
            }
        }

    }

    public void testIntervalizeMultiples() {

        int[] extras = {4, 4, 4, 6, 7, 8, 8};
        IntArrayList left = new IntArrayList();
        IntArrayList leftExpected = new IntArrayList();
        leftExpected.add(4);
        leftExpected.add(8);
        IntArrayList len = new IntArrayList();
        IntArrayList lenExpected = new IntArrayList();
        lenExpected.add(3);
        lenExpected.add(2);
        IntArrayList residuals = new IntArrayList();
        IntArrayList residualsExpected = new IntArrayList();
        residualsExpected.add(6);
        residualsExpected.add(7);
        int nMultiples = BVMultiGraph.intervalizeMultiples(extras, extras.length, left, len, residuals);
        System.out.println(Arrays.toString(left.elements()));
        System.out.println(Arrays.toString(len.elements()));
        System.out.println(Arrays.toString(residuals.elements()));
        Assert.assertEquals(2, nMultiples);
        Assert.assertEquals(leftExpected, left);
        Assert.assertEquals(lenExpected, len);
        Assert.assertEquals(residualsExpected, residuals);

    }

}
