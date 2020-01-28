package org.hps.analysis.alignment.straighttrack;

import java.util.List;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ngraf
 */
public class DetectorBuilderTest extends TestCase {

    public void testIt() {
        DetectorBuilder db = new DetectorBuilder("HPS-PhysicsRun2019-v1-4pt5_fieldOff");

        List<DetectorPlane> planes = db.getTracker("topSlot");
        for (DetectorPlane p : planes) {
            System.out.println(p);
            System.out.println("");
        }

        db.drawDetector();
    }

}
