package gr.uoa.di.networkanalysis;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/** Exports one big-endian 32-bit outdegree per node. */
public final class ExportOutdegrees {

    private ExportOutdegrees() {}

    private static BVMultiGraph loadGraph(final String basename, final String mode)
            throws IOException {
        switch (mode.toLowerCase(Locale.ROOT)) {
            case "mapped":
                return BVMultiGraph.loadMapped(basename);
            case "standard":
                return BVMultiGraph.load(basename);
            case "sequential":
                return BVMultiGraph.loadSequential(basename);
            default:
                throw new IllegalArgumentException(
                        "Unsupported load mode: " + mode
                                + " (expected mapped|standard|sequential)");
        }
    }

    public static void main(final String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: ExportOutdegrees <graph-basename> <output.bin> "
                            + "[mapped|standard|sequential]");
        }

        final String basename = args[0];
        final Path outputPath = Paths.get(args[1]).toAbsolutePath().normalize();
        final Path outputDirectory = outputPath.getParent();
        if (outputDirectory != null) {
            Files.createDirectories(outputDirectory);
        }
        final String output = outputPath.toString();
        final String mode = args.length == 3 ? args[2] : "sequential";

        final long loadStart = System.nanoTime();
        final BVMultiGraph graph = loadGraph(basename, mode);
        final long loadEnd = System.nanoTime();

        final int numNodes = graph.numNodes();
        System.out.printf("Loaded graph: %s%n", basename);
        System.out.printf("Load mode: %s%n", mode);
        System.out.printf("Nodes: %d%n", numNodes);
        System.out.printf("Load time: %.3f s%n", (loadEnd - loadStart) / 1e9);

        long maxDegree = 0L;
        int maxDegreeNode = -1;
        long degreeSum = 0L;
        final long writeStart = System.nanoTime();

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(output), 1 << 20))) {
            final long progressEvery = 1_000_000L;

            for (int node = 0; node < numNodes; node++) {
                final int degree = graph.outdegree(node);
                out.writeInt(degree);

                degreeSum += degree;
                if (degree > maxDegree) {
                    maxDegree = degree;
                    maxDegreeNode = node;
                }

                final int seen = node + 1;
                if (seen % progressEvery == 0) {
                    final double elapsedSec = (System.nanoTime() - writeStart) / 1e9;
                    System.out.printf(
                            Locale.ROOT,
                            "Progress: %,d / %,d (%.2f%%), elapsed %.1f s%n",
                            seen, numNodes, 100.0 * seen / numNodes, elapsedSec);
                }
            }
        }

        final long writeEnd = System.nanoTime();
        final double averageDegree = numNodes == 0 ? 0.0 : degreeSum / (double) numNodes;
        System.out.printf(Locale.ROOT, "Write time: %.3f s%n", (writeEnd - writeStart) / 1e9);
        System.out.printf(Locale.ROOT, "Average degree: %.6f%n", averageDegree);
        System.out.printf("Max degree: %d at node %d%n", maxDegree, maxDegreeNode);
        System.out.printf("Output: %s%n", output);
    }
}
