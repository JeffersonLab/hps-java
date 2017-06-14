/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.examples;

import java.io.File;
import junit.framework.TestCase;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class FeeAnalysisDriverTest extends TestCase {

    private final int nEvents = -1;
    
    public void testIt() throws Exception {

        File lcioInputFile = new File("E:/hps_data/iss87Testing/run5772_stripOneFee_5000Events_iss87.slcio");
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        FeeAnalysisDriver fee = new FeeAnalysisDriver();
        loop.add(fee);
        loop.loop(nEvents);
        loop.dispose();
    }
}
