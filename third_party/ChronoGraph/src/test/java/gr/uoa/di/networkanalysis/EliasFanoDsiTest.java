package gr.uoa.di.networkanalysis;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Assert;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;

public class EliasFanoDsiTest {

    public void test() throws Exception {
        Random random = new Random(23);
        int number = 0;
        int elements = 100;
        IntArrayList list = new IntArrayList();
        list.add(10);
        list.add(11);
        list.add(11);
        EliasFanoMonotoneLongBigList efmlbl = new EliasFanoMonotoneLongBigList(list);
        FileOutputStream fos = new FileOutputStream("myfile");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(efmlbl);
        oos.close();
        fos.close();
        System.out.println(efmlbl.numBits());
        Assert.assertTrue(efmlbl.size64() < 4 * 8 * elements);
        System.out.println(efmlbl.stream().collect(Collectors.toList()));
        Assert.assertEquals(elements, efmlbl.size64());
    }

}
