package org.hps.analysis.alignment.straighttrack;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author Norman A. Graf
 */
public class DetectorBuilderTest extends TestCase {

    public void testIt() throws Exception {

        String detectorName = "HPS-PhysicsRun2019-v1-4pt5";
        DetectorBuilder db = new DetectorBuilder(detectorName);

        db.drawDetector();
        String suffix = "test";
        db.archiveIt(suffix);
        // try building detector from file...
        Path path = Paths.get(detectorName + "_" + myDate() + "_" + suffix + ".txt");
        DetectorBuilder fileDb = new DetectorBuilder(path);
        fileDb.archiveIt("fromFile");
    }

    static private String myDate() {
        Calendar cal = new GregorianCalendar();
        Date date = new Date();
        cal.setTime(date);
        DecimalFormat formatter = new DecimalFormat("00");
        String day = formatter.format(cal.get(Calendar.DAY_OF_MONTH));
        String month = formatter.format(cal.get(Calendar.MONTH) + 1);
        return cal.get(Calendar.YEAR) + month + day;
    }

}
