package gr.uoa.di.networkanalysis.example;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gr.uoa.di.networkanalysis.EvolvingMultiGraph;

public class Compress {

    private static final boolean headers = false;
    private static final long factor = 1;
    private static final int k = 2;

    private static int[] aggregations = new int[]{1, 15*60, 24*60*60, 60, 30*60, 60*60, 4*60*60, 2*24*60*60};
    private static String[] aggregationsStr = new String[]{"1", "60", "24*60*60", "15*60", "30*60", "60*60", "4*60*60", "2*24*60*60"};


    public static void main(String[] args) throws IOException, InterruptedException {

        if ("-f".equals(args[0])) {

            String graphFile = args[1];
            String basename = args[2];

            BufferedWriter writer = new BufferedWriter(new FileWriter(basename+"-results.txt"));

            for(int k = 2; k < 8; k++) {
                for(int j = 0; j < aggregations.length; j++) {

                    EvolvingMultiGraph emg = new EvolvingMultiGraph(
                            graphFile,
                            headers,
                            k,
                            basename,
                            aggregations[j]
                            );

                    emg.storeTimestampsAndIndex();

                    writer.append(String.format("k: %d, aggregation: %s, timestamps: %d, index: %d", k, aggregationsStr[j], new File(basename+".timestamps").length(), new File(basename+".efindex").length()));
                    writer.newLine();
                    writer.flush();
                }
            }

            writer.close();

        } else {
            String graphFile = args[1];
            String basename = args[2];

            long tic = System.nanoTime();
            EvolvingMultiGraph emg = new EvolvingMultiGraph(
                    graphFile,
                    headers,
                    k,
                    basename,
                    factor
                    );

            emg.store();
            System.out.println("Test store: "+(System.nanoTime()-tic));

        }
    }


}

