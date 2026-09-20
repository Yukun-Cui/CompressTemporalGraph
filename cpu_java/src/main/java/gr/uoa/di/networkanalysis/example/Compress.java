package gr.uoa.di.networkanalysis.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import gr.uoa.di.networkanalysis.EvolvingMultiGraph;

/** Standalone compression and timestamp-parameter exploration utility. */
public final class Compress {

    private static final boolean HAS_HEADERS = false;
    private static final long DEFAULT_AGGREGATION_FACTOR = 1;
    private static final int DEFAULT_ZETA_K = 2;
    private static final int[] AGGREGATIONS = {
        1,
        60,
        15 * 60,
        30 * 60,
        60 * 60,
        4 * 60 * 60,
        24 * 60 * 60,
        2 * 24 * 60 * 60
    };
    private static final String[] AGGREGATION_LABELS = {
        "1",
        "60",
        "15*60",
        "30*60",
        "60*60",
        "4*60*60",
        "24*60*60",
        "2*24*60*60"
    };

    private Compress() {}

    public static void main(final String[] args) throws IOException, InterruptedException {
        if (args.length != 3 || !("-f".equals(args[0]) || "-s".equals(args[0]))) {
            throw new IllegalArgumentException(
                    "Usage: Compress <-f|-s> <input-graph> <output-basename>");
        }

        final String graphFile = args[1];
        final Path outputBase = Paths.get(args[2]).toAbsolutePath().normalize();
        final Path outputDirectory = outputBase.getParent();
        if (outputDirectory != null) {
            Files.createDirectories(outputDirectory);
        }
        final String basename = outputBase.toString();

        if ("-f".equals(args[0])) {
            exploreTimestampParameters(graphFile, basename);
        } else {
            storeGraph(graphFile, basename);
        }
    }

    private static void exploreTimestampParameters(
            final String graphFile, final String basename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(basename + "-results.txt"))) {
            for (int zetaK = 2; zetaK < 8; zetaK++) {
                for (int index = 0; index < AGGREGATIONS.length; index++) {
                    final EvolvingMultiGraph graph = new EvolvingMultiGraph(
                            graphFile,
                            HAS_HEADERS,
                            zetaK,
                            basename,
                            AGGREGATIONS[index]);
                    graph.storeTimestampsAndIndex();

                    writer.append(String.format(
                            Locale.ROOT,
                            "k: %d, aggregation: %s, timestamps: %d, index: %d",
                            zetaK,
                            AGGREGATION_LABELS[index],
                            new File(basename + ".timestamps").length(),
                            new File(basename + ".efindex").length()));
                    writer.newLine();
                    writer.flush();
                }
            }
        }
    }

    private static void storeGraph(final String graphFile, final String basename)
            throws IOException, InterruptedException {
        final long start = System.nanoTime();
        final EvolvingMultiGraph graph = new EvolvingMultiGraph(
                graphFile,
                HAS_HEADERS,
                DEFAULT_ZETA_K,
                basename,
                DEFAULT_AGGREGATION_FACTOR);
        graph.store();
        System.out.printf(
                Locale.ROOT,
                "Stored graph in %.3f ms%n",
                (System.nanoTime() - start) / 1_000_000.0);
    }
}
