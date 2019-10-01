/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.alignment.straighttrack;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class StraightTrackAlignmentDriverTest extends TestCase {

    public void testIt() throws Exception {
        FileCache cache = new FileCache();
        int nEvents = -1;
        LCSimLoop loop = new LCSimLoop();
        loop.add(new StraightTrackAlignmentDriver());
        String fileName = "hps_010101.evio.00000_skim.slcio";
//        String fileName = "hps_010101.evio.00000_skimBottomReReco_FieldOff_9Events.slcio";
//        String fileName = "mu-_1.056GeV_slic-3.1.5_geant4-v9r6p1_QGSP_BERT_HPS-EngRun2015-Nominal-v1_fieldOff_++_reco.slcio";
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/fieldoff/" + fileName));
        loop.setLCIORecordSource(inputFile);
        loop.loop(nEvents);

        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }

}
