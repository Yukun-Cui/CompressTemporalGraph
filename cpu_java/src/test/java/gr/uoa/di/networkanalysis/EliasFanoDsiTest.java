package gr.uoa.di.networkanalysis;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;

public class EliasFanoDsiTest {

    @Test
    public void shouldPreserveMonotoneValuesAfterSerialization() throws Exception {
        final LongArrayList values = new LongArrayList(new long[] {10L, 11L, 11L, 25L});
        final EliasFanoMonotoneLongBigList original =
                new EliasFanoMonotoneLongBigList(values);

        final byte[] serialized;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
            output.flush();
            serialized = bytes.toByteArray();
        }

        final EliasFanoMonotoneLongBigList restored;
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(serialized))) {
            restored = (EliasFanoMonotoneLongBigList) input.readObject();
        }

        assertEquals(4L, restored.size64());
        assertEquals(10L, restored.getLong(0));
        assertEquals(11L, restored.getLong(1));
        assertEquals(11L, restored.getLong(2));
        assertEquals(25L, restored.getLong(3));
    }
}
