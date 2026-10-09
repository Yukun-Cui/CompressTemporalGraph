package gr.uoa.di.networkanalysis;

import it.unimi.dsi.io.InputBitStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/** Reports the realized backward-reference chain depth of a stored BV graph. */
public final class ReferenceDepthScanner {

    private ReferenceDepthScanner() {}

    private static final class Result {
        int maximumDepth;
        long referencedNodes;
        long truncatedNodes;
        final long[] depthCounts = new long[16];
    }

    private static Result scan(final String basename) throws Exception {
        final BVMultiGraph graph = BVMultiGraph.load(basename, 2, null);
        final int nodeCount = graph.numNodes();
        final int[] depth = new int[nodeCount];
        final Result result = new Result();

        try (InputBitStream input =
                new InputBitStream(new FileInputStream(basename + BVMultiGraph.GRAPH_EXTENSION))) {
            for (int node = 0; node < nodeCount; node++) {
                final int outdegree = graph.outdegree(node);
                input.position(graph.cachedPointer);
                int reference = -1;
                int extraCount = outdegree;
                int copied = 0;

                if (outdegree > 0 && graph.windowSize() > 0) {
                    reference = graph.readReference(input);
                    if (reference > 0) {
                        result.referencedNodes++;
                        depth[node] = depth[node - reference] + 1;
                        final int blockCount = graph.readBlockCount(input);
                        int total = 0;
                        for (int block = 0; block < blockCount; block++) {
                            final int length = graph.readBlock(input) + (block == 0 ? 0 : 1);
                            total += length;
                            if ((block & 1) == 0) {
                                copied += length;
                            }
                        }
                        if ((blockCount & 1) == 0) {
                            copied += graph.outdegree(node - reference) - total;
                        }
                        extraCount = outdegree - copied;
                    } else {
                        depth[node] = 0;
                    }
                } else {
                    depth[node] = 0;
                }

                if (depth[node] > result.maximumDepth) {
                    result.maximumDepth = depth[node];
                }
                if (depth[node] >= result.depthCounts.length) {
                    result.truncatedNodes++;
                } else {
                    result.depthCounts[depth[node]]++;
                }

                if (extraCount > 0) {
                    final int multipleCount = input.readGamma();
                    for (int i = 0; i < multipleCount; i++) {
                        input.readLongGamma();
                        extraCount -= input.readGamma() + BVMultiGraph.DEFAULT_MIN_MULTIPLE_LENGTH;
                    }
                }
                if (extraCount > 0) {
                    final int intervalCount =
                            graph.minIntervalLength == BVMultiGraph.NO_INTERVALS
                                    ? 0
                                    : input.readGamma();
                    for (int i = 0; i < intervalCount; i++) {
                        input.readLongGamma();
                        extraCount -= input.readGamma() + graph.minIntervalLength;
                    }
                }
                for (int i = 0; i < extraCount; i++) {
                    graph.readResidual(input);
                }
            }
        }
        return result;
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ReferenceDepthScanner <graph-basename>");
        }
        final Result result = scan(args[0]);
        System.out.printf(
                Locale.ROOT,
                "RESULT basename=%s maximum_depth=%d configured_max_ref_count=%d referenced_nodes=%d truncated_nodes=%d depth_counts=0:%d,1:%d,2:%d,3:%d%n",
                args[0],
                result.maximumDepth,
                BVMultiGraph.load(args[0], 2, null).maxRefCount(),
                result.referencedNodes,
                result.truncatedNodes,
                result.depthCounts[0],
                result.depthCounts[1],
                result.depthCounts[2],
                result.depthCounts[3]);
    }
}
