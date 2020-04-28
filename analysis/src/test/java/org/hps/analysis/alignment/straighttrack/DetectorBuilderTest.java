package org.hps.analysis.alignment.straighttrack;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class DetectorBuilderTest extends TestCase {

    public void testIt() throws Exception {
        DetectorBuilder db = new DetectorBuilder("HPS-PhysicsRun2019-v1-4pt5");

//        List<DetectorPlane> planes = db.getTracker("topSlot");
//        for (DetectorPlane p : planes) {
//            System.out.println(p);
//            System.out.println("");
//        }

        db.drawDetector();
        db.archiveIt("test");

        // try building detector from file...
        Path path = Paths.get("HPS-PhysicsRun2019-v1-4pt5_20200211_test.txt");
        DetectorBuilder fileDb = new DetectorBuilder(path);
        fileDb.archiveIt("fromFile");
    }

}
