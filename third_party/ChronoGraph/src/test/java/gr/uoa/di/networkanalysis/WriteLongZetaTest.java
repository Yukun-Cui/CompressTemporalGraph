package gr.uoa.di.networkanalysis;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Assert;

import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;
import it.unimi.dsi.webgraph.BVGraph;

public class WriteLongZetaTest {

    public void testWriteLongZeta() throws IOException {

        long timestamp = 124;
        long first = 1, second = 2, third = 1, fourth = 198765;

        final OutputBitStream obs = new OutputBitStream(new FileOutputStream("bit-timestamps.txt"), 1024 * 1024);
        obs.writeLong(timestamp, 64);
        obs.writeLongZeta(first, BVGraph.DEFAULT_ZETA_K);
        obs.writeLongZeta(second, BVGraph.DEFAULT_ZETA_K);
        obs.writeLongZeta(third, BVGraph.DEFAULT_ZETA_K);
        obs.writeLongZeta(fourth, BVGraph.DEFAULT_ZETA_K);
        obs.close();

        InputBitStream ibs = new InputBitStream(new FileInputStream("bit-timestamps.txt"), 1024 * 1024);
        long resultTimestamp = ibs.readLong(64);
        long resultFirst = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        long resultSecond = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        long resultThird = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        long resultFourth = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        ibs.close();

        Assert.assertEquals(timestamp, resultTimestamp);
        Assert.assertEquals(first, resultFirst);
        Assert.assertEquals(second, resultSecond);
        Assert.assertEquals(third, resultThird);
        Assert.assertEquals(fourth, resultFourth);

    }


    public void testReadFromMemory() throws IOException {

        long timestamp = 124;
        long first = 1, second = 2, third = 1, fourth = 198765;

        final OutputBitStream obs = new OutputBitStream(new FileOutputStream("bit-timestamps.txt"), 1024 * 1024);
        obs.writeLong(timestamp, 64);
        obs.writeLongZeta(first, BVGraph.DEFAULT_ZETA_K);
        obs.writeLongZeta(second, BVGraph.DEFAULT_ZETA_K);
        obs.writeLongZeta(third, BVGraph.DEFAULT_ZETA_K);
        obs.writeLongZeta(fourth, BVGraph.DEFAULT_ZETA_K);
        obs.close();

        byte timestampsInMemory[] = null;
        final FileInputStream fis = new FileInputStream("bit-timestamps.txt");

        if (fis.getChannel().size() <= Integer.MAX_VALUE) {
            timestampsInMemory = new byte[(int) fis.getChannel().size()];
            BinIO.loadBytes(fis, timestampsInMemory);
            fis.close();
        }

        InputBitStream ibs = new InputBitStream(timestampsInMemory);
        ibs.position(64);
        long resultFirst = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        long resultSecond = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        long resultThird = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        long resultFourth = ibs.readLongZeta(BVGraph.DEFAULT_ZETA_K);
        ibs.position(0);
        long resultTimestamp = ibs.readLong(64);

        Assert.assertEquals(timestamp, resultTimestamp);
        Assert.assertEquals(first, resultFirst);
        Assert.assertEquals(second, resultSecond);
        Assert.assertEquals(third, resultThird);
        Assert.assertEquals(fourth, resultFourth);

        ibs.close();
    }

}
