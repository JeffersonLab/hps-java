package org.hps.recon.tracking.lit;

import java.util.Collections;
import static java.util.Collections.list;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class HpsStripHitTest extends TestCase
{

    public void testIt()
    {
        // create a set of hits to check that natural ordering works
        TreeSet<HpsStripHit> hits = new TreeSet<HpsStripHit>();

        CartesianThreeVector zdir = new CartesianThreeVector(0, 0, 1);
        CartesianThreeVector pos1 = new CartesianThreeVector(0, 0, 1);
        CartesianThreeVector pos2 = new CartesianThreeVector(0, 0, 2);
        CartesianThreeVector pos3 = new CartesianThreeVector(0, 0, 3);

        DetectorPlane p1 = new DetectorPlane("plane1", pos1, zdir, .1, 0.);
        DetectorPlane p2 = new DetectorPlane("plane2", pos2, zdir, .1, 0.);
        DetectorPlane p3 = new DetectorPlane("plane3", pos3, zdir, .1, 0.);

        HpsStripHit h1 = new HpsStripHit(1, .1, p1);
        HpsStripHit h2 = new HpsStripHit(1, .1, p2);
        HpsStripHit h3 = new HpsStripHit(1, .1, p3);

        System.out.println(h2);
        hits.add(h3);
        hits.add(h3);
        hits.add(h1);
        hits.add(h2);

      // checks
        // check that only three hits were added
        assertEquals(hits.size(), 3);
        Iterator<HpsStripHit> it = hits.iterator();
        assertEquals(it.next().plane().name(), "plane1");
        assertEquals(it.next().plane().name(), "plane2");
        assertEquals(it.next().plane().name(), "plane3");
        assertFalse(it.hasNext());

      //reverse sort (needed for smoothing...
       Set reverse = hits.descendingSet();
       assertEquals(reverse.size(), 3);
        Iterator<HpsStripHit> reverseIt = reverse.iterator();
        assertEquals(reverseIt.next().plane().name(), "plane3");
        assertEquals(reverseIt.next().plane().name(), "plane2");
        assertEquals(reverseIt.next().plane().name(), "plane1");
        assertFalse(reverseIt.hasNext());
    }
}
