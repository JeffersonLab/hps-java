package org.hps.recon.filtering;

import java.io.File;
import java.io.IOException;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.util.test.TestUtil;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class V0CandidateFilter2015Pass7Test extends TestCase {

    public void testIt() throws Exception {
        File inputFile = TestUtil.downloadTestFile("EngRun2015V0ReconTest.slcio");
        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager.getInstance();
        V0CandidateFilter2015Pass7 driver = new V0CandidateFilter2015Pass7();
        loop.add(driver);
        try {
            loop.setLCIORecordSource(inputFile);
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
