package org.hps.test.it;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.evio.EvioToLcio;
import org.hps.test.util.TestOutputFile;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author Norman A. Graf
 */
public class EngRun2015MollerReconTest extends TestCase {
    
static final String testURLBase = "http://www.lcsim.org/test/hps-java/calibration";
    static final String testFileName = "run5772_Moller_1000Events.evio";
    private final int nEvents = -1;

    public void testIt() throws Exception {
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        File evioInputFile = cache.getCachedFile(testURL);
        File outputFile = new TestOutputFile(EngRun2015MollerReconTest.class, "EngRun2015MollerReconTest");
        String args[] = {"-r", "-x", "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim", "-d",
            "HPS-EngRun2015-Nominal-v6-0-fieldmap", "-D", "outputFile=" + outputFile.getPath(), //"-n", "2000",
            evioInputFile.getPath(), "-e ", "100"};
        System.out.println("Running EngRun2015MollerReconTest.main ...");
        System.out.println("writing to: " + outputFile.getPath());
        long startTime = System.currentTimeMillis();
        EvioToLcio.main(args);
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        // Read in the LCIO event file and print out summary information.
        System.out.println("Running ReconCheckDriver on output ...");
        LCSimLoop loop = new LCSimLoop();
        EngRun2015MollerRecon reconDriver = new EngRun2015MollerRecon();
        String aidaOutputFile = new TestUtil.TestOutputFile(getClass().getSimpleName()).getPath() + File.separator + this.getClass().getSimpleName() + ".aida";
        System.out.println("writing aida file to: "+aidaOutputFile);
        reconDriver.setAidaFileName(aidaOutputFile);
        loop.add(reconDriver);
        try {
            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }
}