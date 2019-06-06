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
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class StripHitAnalysisDriverTest extends TestCase
{
 public void testIt() throws Exception
    {
        System.out.println("Running from: " + Paths.get("").toAbsolutePath());
        String fileName = "http://www.lcsim.org/test/hps-java/singleFullEnergyElectrons_SLIC-v05-00-00_Geant4-v10-01-02_QGSP_BERT_HPS-EngRun2015-Nominal-v2-fieldmap_minINteractions_recon.slcio";
//        String fileName = "http://www.lcsim.org/test/hps-java/e-_1.056GeV_SLIC-v05-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-EngRun2015-Nominal-v2-fieldmap_recon.slcio";
        FileCache cache = new FileCache();
        int nEvents = 10;
        LCSimLoop loop = new LCSimLoop();
        StripHitAnalysisDriver d = new StripHitAnalysisDriver();
        loop.add(d);
        try {
            File inputFile = cache.getCachedFile(new URL(fileName));
            loop.setLCIORecordSource(inputFile);
            loop.loop(nEvents);
            // d.showPlots();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }   

}
