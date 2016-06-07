package org.hps.test.it;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.evio.EvioToLcio;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test to run the standard reconstruction on Engineering Run 2015 EVIO data. Full energy electron candidate events were
 * selected from pass1 output. The current test runs the default reconstruction over the evio file then analyzes the
 * output lcio file. The current checks are minimal and need to be improved.
 *
 * @author Norman A Graf
 */
public class EngRun2015ReconTest extends TestCase {
  
    public void testEngRun2015Recon() throws Exception {
        File inputFile = new TestDataUtility().getTestData("run5772_integrationTest.evio");        
        File outputFile = new TestOutputFile("EngRun2015ReconTest");
        String args[] = {"-r", "-x", "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim", "-d",
                "HPS-EngRun2015-Nominal-v3", "-D", "outputFile=" + outputFile.getPath(), "-n", "100",
                inputFile.getPath()};
        System.out.println("Running EngRun2015ReconTest.main ...");
        System.out.println("writing to: " + outputFile.getPath());
        long startTime = System.currentTimeMillis();
        EvioToLcio.main(args);
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        // Read in the LCIO event file and print out summary information.
        System.out.println("Running ReconCheckDriver on output ...");
        LCSimLoop loop = new LCSimLoop();
        //loop.add(new EngRun2015ReconTest.ReconCheckDriver());
        try {
            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }

    static class ReconCheckDriver extends Driver {

        int nFail;
        int nProcessed;

        public void process(EventHeader event) {
            boolean fail = false;
            List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, "FinalStateParticles");
            int nrp = rps.size();
            if (nrp < 1) {
                fail = true;
            }
            assertTrue("Didn't find at least one ReconstructedParticle", nrp > 0);
            int nelectron = 0;
            int npositron = 0;

            ReconstructedParticle electron = null;
            ReconstructedParticle positron = null;
            for (ReconstructedParticle p : rps) {
                if (p.getParticleIDUsed().getPDG() == 11) {
                    electron = p;
                    ++nelectron;
                }
                if (p.getParticleIDUsed().getPDG() == -11) {
                    positron = p;
                    ++npositron;
                }
            }
            if (nelectron != 2) {
                fail = true;
            }
            if (npositron != 0) {
                fail = true;
            }
            if (fail) {
                System.out.println("run " + event.getRunNumber() + " event " + event.getEventNumber());
                System.out.println("found " + nelectron + " electrons and " + npositron + " positrons");
                System.out.println("found " + nrp + " ReconstructedParticles ");
            }
            nProcessed++;
            if (fail) {
                nFail++;
            }
            // TODO add checks on quality of output (chi2, p, E, E/p matching, position matching, etc.)
        }

        public void endOfData() {
            System.out.println(nFail + " of " + nProcessed + " events failed");
            assertEquals("Expected no events to fail", 0, nFail);
        }
    }
}
