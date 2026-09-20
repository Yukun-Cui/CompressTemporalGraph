package gr.uoa.di.networkanalysis;

import java.io.IOException;
import java.io.Serializable;

import it.unimi.dsi.webgraph.CompressionFlags;
import it.unimi.dsi.webgraph.ImmutableGraph;

/** EvolvingGraph class. */
public class EvolvingGraph extends ImmutableGraph implements CompressionFlags, Serializable {

    private static final long serialVersionUID = -7246400408981009520L;

    /** Writes the given graph using a given base name.
     *
     * @param graph a graph to be compressed.
     * @param basename a base name.
     * @param windowSize the window size (-1 for the default value).
     * @param maxRefCount the maximum reference count (-1 for the default value).
     * @param minIntervalLength the minimum interval length (-1 for the default value, {@link #NO_INTERVALS} to disable).
     * @param zetaK the parameter used for residual &zeta;-coding, if used (-1 for the default value).
     * @param flags the flag mask.
     * @param numberOfThreads the number of threads to use; if 0 or negative, it will be replaced by {@link Runtime#availableProcessors()}. Note that if
     * {@link ImmutableGraph#numNodes()} is not implemented by {@code graph}, the number of threads will be automatically set to one, possibly logging a warning.
     * @param pl a progress logger to log the state of compression, or <code>null</code> if no logging is required.
     * @throws IOException if some exception is raised while writing the graph.
     */
    //    public static void store(final ImmutableGraph graph, final CharSequence basename, final int windowSize, final int maxRefCount, final int minIntervalLength,
    //            final int zetaK, final int flags, final int numberOfThreads, final ProgressLogger pl) throws IOException {
    //        final EvolvingGraph g = new EvolvingGraph();
    //        if (windowSize != -1) g.windowSize = windowSize;
    //        if (maxRefCount != -1) g.maxRefCount = maxRefCount;
    //        if (minIntervalLength != -1) g.minIntervalLength = minIntervalLength;
    //        if (zetaK != -1) g.zetaK = zetaK;
    //        g.setFlags(flags);
    //        g.storeInternal(graph, basename, numberOfThreads, pl);
    //    }


    @Override
    public int numNodes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean randomAccess() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int outdegree(int x) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ImmutableGraph copy() {
        // TODO Auto-generated method stub
        return null;
    }

}
