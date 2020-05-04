/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.lit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.lcsim.geometry.Detector;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author Norman A. Graf
 */
public class SimTrackerHitFitterDriverTest extends TestCase {

    public void testIt() throws Exception {
        System.out.println("Running from: " + Paths.get("").toAbsolutePath());
        String fileName = "http://www.lcsim.org/test/hps-java/hpsForwardFullEnergyElectrons_z-4.3_2.3GeV_top_SLIC-v06-00-01_QGSP_BERT_HPS-PhysicsRun2016-Pass2_nomsc_recon_strip12Hits_1kEvents.slcio";
        FileCache cache = new FileCache();
        int nEvents = 2;
        LCSimLoop loop = new LCSimLoop();
        SimTrackerHitFitterDriver d = new SimTrackerHitFitterDriver();
        loop.add(d);
        try {
            File inputFile = cache.getCachedFile(new URL(fileName));
            loop.setLCIORecordSource(inputFile);
            loop.loop(nEvents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }
}