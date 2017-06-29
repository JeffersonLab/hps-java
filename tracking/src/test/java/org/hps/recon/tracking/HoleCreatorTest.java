package org.hps.recon.tracking;

import java.io.File;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

import junit.framework.TestCase;

/**
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu> $Id:
 *         HoleCreatorTest.java, v1 05/30/2017$ Takes reconstructed lcio file as
 *         input Removes hits using HoleCreationDriver, makes new lcio Removes
 *         old versions of old collections and does not re-do tracking
 */

public class HoleCreatorTest extends TestCase {
    static final String testFileName = "ap_prompt_new.slcio";
    private final int nEvents = 10;
    private boolean[] pattern = { false, false, false, false, true, true, false, false, false, false, false, false };

    public void testHoles() throws Exception {
        File inputFile = new File("target/test-output/" + testFileName);
        File outputFile = new TestOutputFile(testFileName.replaceAll(".slcio", "") + "_WithHoles.slcio");
        outputFile.getParentFile().mkdirs();

        DatabaseConditionsManager.getInstance();

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);
        HoleCreationDriver hcd = new HoleCreationDriver(pattern, 0);
        loop.add(hcd);

        loop.add(new LCIODriver(outputFile));
        loop.loop(nEvents, null);
        loop.dispose();
    }

}
