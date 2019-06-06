/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.ztrack;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class PhantomDetectorAnalysisDriverTest extends TestCase {

    public void testIt() throws Exception {
        FileCache cache = new FileCache();
        int nEvents = 1; //-1;
        LCSimLoop loop = new LCSimLoop();
        PhantomDetectorAnalysisDriver d = new PhantomDetectorAnalysisDriver();
        loop.add(d);
        String fileName = "e-_2.3GeV_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2016-Pass2-Phantom-fieldmap_nomsc.slcio";
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/" + fileName));
        loop.setLCIORecordSource(inputFile);
        loop.loop(nEvents);

        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }

}