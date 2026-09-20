package gr.uoa.di.networkanalysis;

import gr.uoa.di.networkanalysis.EvolvingMultiGraph.SuccessorIterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Locale;
import java.util.PrimitiveIterator.OfInt;
import java.util.Queue;
import java.util.Random;

public final class BaselineRunner {

    private static final Random RANDOM = new Random(12345L);
    private static final int AGGREGATION = 1;
    private static final int ZETA_K = 2;

    private BaselineRunner() {}

    private static Path compressRoot() {
        String configured = System.getProperty("tga.compress.root");
        if (configured == null || configured.isEmpty()) {
            configured = System.getenv("TGA_COMPRESS_ROOT");
        }
        if (configured != null && !configured.isEmpty()) {
            final Path candidate = Paths.get(configured).toAbsolutePath().normalize();
            if (Files.isDirectory(candidate.resolve("data"))) {
                return candidate;
            }
            throw new IllegalStateException("Invalid project root: " + candidate);
        }

        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("data"))
                    && Files.isDirectory(current.resolve("gpu_cuda"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "Cannot locate project root; set TGA_COMPRESS_ROOT or -Dtga.compress.root.");
    }

    private static String compressPath(final String first, final String... more) {
        return compressRoot().resolve(Paths.get(first, more)).toString();
    }

    private enum Workload {
        RA,
        BFS,
        CC,
        PR,
        HITS,
        DC,
        GC;

        static Workload parse(final String value) {
            final String normalized = value.toUpperCase(Locale.ROOT)
                    .replace("-", "")
                    .replace("_", "");
            switch (normalized) {
                case "RA":
                case "RANDOMACCESS":
                    return RA;
                case "BFS":
                case "BREADTHFIRSTSEARCH":
                    return BFS;
                case "CC":
                case "CONNECTEDCOMPONENTS":
                    return CC;
                case "PR":
                case "PAGERANK":
                    return PR;
                case "HITS":
                    return HITS;
                case "DC":
                case "DEGREECENTRALITY":
                    return DC;
                case "GC":
                case "GRAPHCOLORING":
                    return GC;
                default:
                    throw new IllegalArgumentException("Unknown workload: " + value);
            }
        }
    }

    private static final class DatasetConfig {
        final String name;
        final Path rawInput;
        final Path basename;
        final int firstLabel;
        final int lastLabel;
        final int arraySize;
        final boolean headers;
        final int activeFromInclusive;
        final int activeToExclusive;
        final int prIterations;
        final int hitsIterations;
        final int gcRounds;
        final int raIterations;

        DatasetConfig(
                final String name,
                final String rawInput,
                final String basename,
                final int firstLabel,
                final int lastLabel,
                final int arraySize,
                final boolean headers,
                final int activeFromInclusive,
                final int activeToExclusive,
                final int prIterations,
                final int hitsIterations,
                final int gcRounds,
                final int raIterations) {
            this.name = name;
            this.rawInput = Paths.get(rawInput);
            this.basename = Paths.get(basename);
            this.firstLabel = firstLabel;
            this.lastLabel = lastLabel;
            this.arraySize = arraySize;
            this.headers = headers;
            this.activeFromInclusive = activeFromInclusive;
            this.activeToExclusive = activeToExclusive;
            this.prIterations = prIterations;
            this.hitsIterations = hitsIterations;
            this.gcRounds = gcRounds;
            this.raIterations = raIterations;
        }
    }

    private static DatasetConfig datasetConfig(final String dataset) {
        switch (dataset.toLowerCase(Locale.ROOT)) {
            case "comm":
            case "cbtcomm":
                return new DatasetConfig(
                        "Comm",
                        compressPath("data", "raw", "Comm.raw.txt.gz"),
                        compressPath("data", "compressed", "Comm", "Comm"),
                        0,
                        9999,
                        10000,
                        false,
                        0,
                        10000,
                        100,
                        100,
                        512,
                        10000);
            case "myd":
                return new DatasetConfig(
                        "MyD",
                        compressPath("data", "raw", "MyD.raw.txt.gz"),
                        compressPath("data", "compressed", "MyD", "MyD"),
                        1,
                        999967,
                        1000001,
                        false,
                        0,
                        999968,
                        100,
                        100,
                        512,
                        999968);
            case "flickr":
                return new DatasetConfig(
                        "Flickr",
                        compressPath("data", "raw", "Flickr.raw.txt.gz"),
                        compressPath("data", "compressed", "Flickr", "Flickr"),
                        1,
                        2585568,
                        2585570,
                        false,
                        0,
                        2585569,
                        100,
                        100,
                        512,
                        2585568);
            case "wiki":
                return new DatasetConfig(
                        "Wiki",
                        compressPath("data", "raw", "Wiki.raw.txt.gz"),
                        compressPath("data", "compressed", "Wiki", "Wiki"),
                        1,
                        42640545,
                        42640547,
                        false,
                        0,
                        42640546,
                        10,
                        10,
                        512,
                        42640546);
            case "yahoo":
                return new DatasetConfig(
                        "Yahoo",
                        compressPath("data", "raw", "Yahoo.raw.txt.gz"),
                        compressPath("data", "compressed", "Yahoo", "Yahoo"),
                        0,
                        102867214,
                        102867263,
                        false,
                        0,
                        102867215,
                        2,
                        2,
                        2,
                        102867215);
            case "yahoo-sub":
            case "yahoo_sub":
                return new DatasetConfig(
                        "Yahoo-sub",
                        compressPath("data", "raw", "Yahoo-sub.raw.txt.gz"),
                        compressPath("data", "compressed", "Yahoo-sub", "Yahoo-sub"),
                        2000,
                        102867214,
                        102867263,
                        false,
                        2000,
                        102867215,
                        2,
                        2,
                        2,
                        102865215);
            default:
                throw new IllegalArgumentException("Unknown dataset: " + dataset);
        }
    }

    private static boolean compressedGraphExists(final DatasetConfig config) {
        return Files.exists(Paths.get(config.basename + ".graph"))
                && Files.exists(Paths.get(config.basename + ".offsets"))
                && Files.exists(Paths.get(config.basename + ".properties"))
                && Files.exists(Paths.get(config.basename + ".timestamps"))
                && Files.exists(Paths.get(config.basename + ".efindex"));
    }

    private static void prepareIfNeeded(final DatasetConfig config) throws Exception {
        if (compressedGraphExists(config)) {
            return;
        }
        final Path parent = config.basename.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final EvolvingMultiGraph emg =
                new EvolvingMultiGraph(
                        config.rawInput.toString(),
                        config.headers,
                        ZETA_K,
                        config.basename.toString(),
                        AGGREGATION);
        emg.store();
    }

    private static EvolvingMultiGraph loadGraph(final DatasetConfig config) throws Exception {
        final EvolvingMultiGraph emg =
                new EvolvingMultiGraph(
                        config.rawInput.toString(),
                        config.headers,
                        ZETA_K,
                        config.basename.toString(),
                        AGGREGATION);
        emg.load();
        return emg;
    }

    private static long runRandomAccess(final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        final long start = System.nanoTime();

        if ("MyD".equals(config.name) || "Comm".equals(config.name)) {
            final OfInt iterator =
                    RANDOM.ints(config.firstLabel, config.lastLabel + 1).iterator();
            for (int i = 0; i < config.raIterations; i++) {
                drainSuccessors(emg.successors(iterator.nextInt()));
            }
            return System.nanoTime() - start;
        }

        for (int node = config.activeFromInclusive; node < config.activeToExclusive; node++) {
            drainSuccessors(emg.successors(node));
        }
        return System.nanoTime() - start;
    }

    private static void drainSuccessors(final SuccessorIterator successors) throws Exception {
        try (SuccessorIterator iterator = successors) {
            while (iterator.hasNext()) {
                iterator.next();
            }
        }
    }

    private static long runDegreeCentrality(final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        final int[] degree = new int[config.arraySize];
        final long[] avgTime = new long[config.arraySize];
        final long startTimestamp = 0L;
        final long endTimestamp = 0L;
        final long start = System.nanoTime();
        for (int i = config.activeFromInclusive; i < config.activeToExclusive; i++) {
            try (SuccessorIterator successors = emg.successors(i)) {
                while (successors.hasNext()) {
                    final Successor successor = successors.next();
                    final int neighbor = successor.getNeighbor();
                    final long timestamp = successor.getTimestamp();
                    if (!isTraversableNode(config, neighbor)) {
                        continue;
                    }
                    if (startTimestamp == 0L && endTimestamp == 0L) {
                        degree[i]++;
                        avgTime[i] += (timestamp - avgTime[i]) / degree[i];
                    } else if (startTimestamp <= timestamp && endTimestamp >= timestamp) {
                        degree[i]++;
                        avgTime[i] += (timestamp - avgTime[i]) / degree[i];
                    }
                }
            }
            if (startTimestamp != endTimestamp) {
                degree[i] =
                        (int)
                                (0.85 * degree[i]
                                        + 0.15
                                                * degree[i]
                                                * (avgTime[i] - startTimestamp)
                                                / (endTimestamp - startTimestamp)
                                        + 1);
            }
        }
        return System.nanoTime() - start;
    }

    private static long runBfs(final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        final boolean[] visit = new boolean[config.arraySize];
        final int[] parents = new int[config.arraySize];
        final int[] distance = new int[config.arraySize];
        final long[] time = new long[config.arraySize];
        Arrays.fill(distance, Integer.MAX_VALUE);
        Arrays.fill(time, Long.MAX_VALUE);
        Arrays.fill(parents, -1);

        final Queue<Integer> queue = new ArrayDeque<>();
        final int startNode = Math.max(config.firstLabel, config.activeFromInclusive);
        queue.offer(startNode);
        visit[startNode] = true;
        distance[startNode] = 0;
        time[startNode] = 0L;

        final long start = System.nanoTime();
        while (!queue.isEmpty()) {
            final int currentNode = queue.poll();
            if (!isTraversableNode(config, currentNode)) {
                continue;
            }
            try (SuccessorIterator successors = emg.successors(currentNode)) {
                int previousNeighbor = -1;
                while (successors.hasNext()) {
                    final Successor successor = successors.next();
                    final int neighbor = successor.getNeighbor();
                    final long timestamp = successor.getTimestamp();
                    if (neighbor != previousNeighbor
                            && isTraversableNode(config, neighbor)
                            && time[currentNode] <= time[neighbor]
                            && !visit[neighbor]) {
                        distance[neighbor] = distance[currentNode] + 1;
                        time[neighbor] = timestamp;
                        parents[neighbor] = currentNode;
                        visit[neighbor] = true;
                        queue.offer(neighbor);
                    }
                    previousNeighbor = neighbor;
                }
            }
        }
        return System.nanoTime() - start;
    }

    private static long runGraphColoring(final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        final int myInfinity = Integer.MAX_VALUE;
        final int[] vertexPriority = new int[config.arraySize];
        final int[] randomList = new int[config.arraySize];
        Arrays.fill(vertexPriority, myInfinity);

        final Random random = new Random(123L);
        final Queue<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < config.arraySize; i++) {
            randomList[i] = random.nextInt(256);
            queue.offer(i);
        }

        int currentRound = 0;
        final long start = System.nanoTime();
        while (currentRound < config.gcRounds) {
            final int size = queue.size();
            for (int i = 0; i < size; i++) {
                final int vertexId = queue.poll();
                if (!isTraversableNode(config, vertexId)
                        || vertexPriority[vertexId] != myInfinity) {
                    continue;
                }
                final int localRandom = randomList[vertexId];
                boolean foundLarger = false;
                try (SuccessorIterator successors = emg.successors(vertexId)) {
                    int previousNeighbor = -1;
                    while (successors.hasNext()) {
                        final Successor successor = successors.next();
                        final int neighbor = successor.getNeighbor();
                        if (neighbor != previousNeighbor
                                && isTraversableNode(config, neighbor)
                                && ((randomList[neighbor] > localRandom)
                                                || (randomList[neighbor] == localRandom
                                                        && neighbor < vertexId))
                                && vertexPriority[neighbor] >= currentRound) {
                            foundLarger = true;
                            break;
                        }
                        previousNeighbor = neighbor;
                    }
                }
                if (!foundLarger) {
                    vertexPriority[vertexId] = currentRound;
                } else {
                    queue.offer(vertexId);
                }
            }
            currentRound++;
        }
        return System.nanoTime() - start;
    }

    private static long runPageRank(final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        double[] rank = new double[config.arraySize];
        double[] newRank = new double[config.arraySize];
        final int from = config.activeFromInclusive;
        final int to = Math.min(config.activeToExclusive, config.arraySize);
        Arrays.fill(rank, from, to, 1.0 / (to - from));
        final double dampingFactor = 0.85;
        final long start = System.nanoTime();
        for (int iteration = 0; iteration < config.prIterations; iteration++) {
            Arrays.fill(newRank, 0.0);
            for (int u = from; u < to; u++) {
                newRank[u] = (1.0 - dampingFactor) / (to - from);
            }
            for (int u = from; u < to; u++) {
                final int outdegree = emg.getGraph().outdegree(u);
                if (outdegree == 0) {
                    continue;
                }
                try (SuccessorIterator successors = emg.successors(u)) {
                    int previousNeighbor = -1;
                    while (successors.hasNext()) {
                        final Successor successor = successors.next();
                        final int neighbor = successor.getNeighbor();
                        final long timestamp = successor.getTimestamp();
                        if (neighbor != previousNeighbor
                                && neighbor >= from
                                && neighbor < to) {
                            newRank[neighbor] +=
                                    dampingFactor
                                            * (1.0 - Math.exp(-0.1 * timestamp))
                                            * rank[u]
                                            / outdegree;
                        }
                        previousNeighbor = neighbor;
                    }
                }
            }
            final double[] previousRank = rank;
            rank = newRank;
            newRank = previousRank;
        }
        return System.nanoTime() - start;
    }

    private static long runHits(final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        final double[] hub = new double[config.arraySize];
        final double[] authority = new double[config.arraySize];
        final int from = config.activeFromInclusive;
        final int to = Math.min(config.activeToExclusive, config.arraySize);
        Arrays.fill(hub, from, to, 1.0);
        Arrays.fill(authority, from, to, 1.0);
        final long start = System.nanoTime();
        for (int iteration = 0; iteration < config.hitsIterations; iteration++) {
            Arrays.fill(hub, from, to, 0.0);
            for (int u = from; u < to; u++) {
                try (SuccessorIterator successors = emg.successors(u)) {
                    int previousNeighbor = -1;
                    while (successors.hasNext()) {
                        final Successor successor = successors.next();
                        final int neighbor = successor.getNeighbor();
                        if (neighbor != previousNeighbor
                                && neighbor >= from
                                && neighbor < to) {
                            hub[u] += authority[neighbor];
                        }
                        previousNeighbor = neighbor;
                    }
                }
            }

            double norm = 0.0;
            for (int i = from; i < to; i++) {
                norm += hub[i] * hub[i];
            }
            norm = Math.sqrt(norm);
            if (norm != 0.0) {
                for (int i = from; i < to; i++) {
                    hub[i] /= norm;
                }
            }

            Arrays.fill(authority, from, to, 0.0);
            for (int u = from; u < to; u++) {
                try (SuccessorIterator successors = emg.successors(u)) {
                    int previousNeighbor = -1;
                    while (successors.hasNext()) {
                        final Successor successor = successors.next();
                        final int neighbor = successor.getNeighbor();
                        if (neighbor != previousNeighbor
                                && neighbor >= from
                                && neighbor < to) {
                            authority[neighbor] += hub[u];
                        }
                        previousNeighbor = neighbor;
                    }
                }
            }

            norm = 0.0;
            for (int i = from; i < to; i++) {
                norm += authority[i] * authority[i];
            }
            norm = Math.sqrt(norm);
            if (norm != 0.0) {
                for (int i = from; i < to; i++) {
                    authority[i] /= norm;
                }
            }
        }
        return System.nanoTime() - start;
    }

    private static long runConnectedComponents(final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        final boolean[] visited = new boolean[config.arraySize];
        final int[] component = new int[config.arraySize];
        final int[] parents = new int[config.arraySize];
        final int[] distance = new int[config.arraySize];
        final long[] time = new long[config.arraySize];
        Arrays.fill(component, -1);
        Arrays.fill(distance, Integer.MAX_VALUE);
        Arrays.fill(time, Long.MAX_VALUE);
        Arrays.fill(parents, -1);

        int componentId = 0;
        final long start = System.nanoTime();
        for (int startNode = config.activeFromInclusive; startNode < config.activeToExclusive; startNode++) {
            if (startNode >= config.arraySize || visited[startNode]) {
                continue;
            }

            final Queue<Integer> queue = new ArrayDeque<>();
            queue.offer(startNode);
            visited[startNode] = true;
            component[startNode] = componentId;
            distance[startNode] = 0;
            time[startNode] = 0L;

            while (!queue.isEmpty()) {
                final int currentNode = queue.poll();
                if (!isTraversableNode(config, currentNode)) {
                    continue;
                }
                try (SuccessorIterator successors = emg.successors(currentNode)) {
                    int previousNeighbor = -1;
                    while (successors.hasNext()) {
                        final Successor successor = successors.next();
                        final int neighbor = successor.getNeighbor();
                        final long timestamp = successor.getTimestamp();
                        if (neighbor != previousNeighbor
                                && isTraversableNode(config, neighbor)
                                && time[currentNode] <= time[neighbor]
                                && !visited[neighbor]) {
                            distance[neighbor] = distance[currentNode] + 1;
                            time[neighbor] = timestamp;
                            parents[neighbor] = currentNode;
                            visited[neighbor] = true;
                            component[neighbor] = componentId;
                            queue.offer(neighbor);
                        }
                        previousNeighbor = neighbor;
                    }
                }
            }
            componentId++;
        }
        return System.nanoTime() - start;
    }

    private static boolean isTraversableNode(final DatasetConfig config, final int node) {
        return node >= config.activeFromInclusive
                && node < config.activeToExclusive
                && node < config.arraySize;
    }

    private static long runWorkload(
            final Workload workload, final EvolvingMultiGraph emg, final DatasetConfig config)
            throws Exception {
        switch (workload) {
            case RA:
                return runRandomAccess(emg, config);
            case BFS:
                return runBfs(emg, config);
            case CC:
                return runConnectedComponents(emg, config);
            case PR:
                return runPageRank(emg, config);
            case HITS:
                return runHits(emg, config);
            case DC:
                return runDegreeCentrality(emg, config);
            case GC:
                return runGraphColoring(emg, config);
            default:
                throw new IllegalArgumentException("Unsupported workload: " + workload);
        }
    }

    public static void main(final String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: BaselineRunner <dataset> <workload> [--prepare-only]");
        }

        final DatasetConfig config = datasetConfig(args[0]);
        final Workload workload = Workload.parse(args[1]);
        final boolean prepareOnly;
        if (args.length == 3) {
            if (!"--prepare-only".equals(args[2])) {
                throw new IllegalArgumentException("Unknown option: " + args[2]);
            }
            prepareOnly = true;
        } else {
            prepareOnly = false;
        }

        prepareIfNeeded(config);
        if (prepareOnly) {
            System.out.printf(
                    Locale.ROOT,
                    "PREPARED dataset=%s basename=%s active_from=%d active_to=%d%n",
                    config.name,
                    config.basename,
                    config.activeFromInclusive,
                    config.activeToExclusive);
            return;
        }

        final EvolvingMultiGraph emg = loadGraph(config);
        final long elapsedNs = runWorkload(workload, emg, config);
        System.out.printf(
                Locale.ROOT,
                "RESULT dataset=%s workload=%s elapsed_ns=%d elapsed_ms=%.3f basename=%s active_from=%d active_to=%d%n",
                config.name,
                workload.name(),
                elapsedNs,
                elapsedNs / 1_000_000.0,
                config.basename,
                config.activeFromInclusive,
                config.activeToExclusive);
    }
}
