/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.usgs.anss.seed;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author geoffc
 */
public class MiniSEEDTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    // Checks that MiniSeed is implemented in such a way that
    // a we can use a TreeSet to preserve order and uniquness.
    // Reads an MS file in to an ArrayList - adds it to a TreeSet
    // backwards and forwards and checks that it is still the same
    // as the input.
    @Test
    public void testMiniSeedSortingAndOrdering() throws Exception {

        String filename = "build/test/classes/gov/usgs/anss/seed/NZPXZ__HHZ10.ms";

        File ms = new File(filename);
        long fileSize = ms.length();
        ArrayList<MiniSeed> blks = new ArrayList<MiniSeed>((int) (fileSize / 512));

        byte[] buf = new byte[512];
        FileInputStream in = new FileInputStream(ms);
        for (long pos = 0; pos < fileSize; pos += 512) {
            if (in.read(buf) == -1) {
                break;
            }
            blks.add(new MiniSeed(buf));
        }

        TreeSet<MiniSeed> sorted = new TreeSet<MiniSeed>();

        for (int i = blks.size() - 1; i > 0; i--) {
            sorted.add(blks.get(i));
        }

        for (int i = 0; i < blks.size(); i++) {
            sorted.add(blks.get(i));
        }

        assertEquals(sorted.size(), blks.size());
        assertCollectionEquals("entire collection", sorted, blks);
    }


    private static void assertCollectionEquals(
            Collection<? extends Comparable> c1,
            Collection<? extends Comparable> c2) {
        assertCollectionEquals(null, c1, c2);
    }


    private static void assertCollectionEquals(String message,
            Collection<? extends Comparable> c1,
            Collection<? extends Comparable> c2) {

        assertEquals(message + ": size mismatch", c1.size(), c2.size());

        Iterator<? extends Comparable> i1 = c1.iterator();
        Iterator<? extends Comparable> i2 = c2.iterator();

        while (i1.hasNext() && i2.hasNext()) {
            assertEquals(message + ": comparison inequality", 0, i1.next().compareTo(i2.next()));
        }
    }
}