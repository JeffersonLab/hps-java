package org.hps.users.jeremym;

import java.io.File;

import junit.framework.TestCase;

import org.lcsim.util.loop.LCSimLoop;

/**
 * Basic test of {@link LuminosityAnalysisDriver}.
 *
 * @author Jeremy McCormick, SLAC
 */
public class LuminosityAnalysisDriverTest extends TestCase {

    /**
     * Run the test.
     *
     * @throws Exception if any error occurs
     */
    public void testLuminosityAnalysisDriver() throws Exception {
        final LCSimLoop loop = new LCSimLoop();
        loop.add(new LuminosityAnalysisDriver());
        loop.setLCIORecordSource(new File("/u1/test/hps/recon/hps_005772_data_only.slcio"));
        loop.loop(-1);
    }

}
