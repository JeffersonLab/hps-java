package org.hps.recon.tracking;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.freehep.record.loop.RecordLoop.Command;
import org.hps.util.CompareHistograms;
import org.lcsim.event.EventHeader;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;


/**
 * 
 * $Id: HelicalTrackHitDriverTest.java,v 1.3 2013/10/25 20:29:03 jeremy Exp $
 */
public class HelicalTrackHitDriverTest extends TestCase {

    File commonOutputFile; 
    File splitOutputFile; 
    
    /**
     * 
     */
    public void setUp() throws Exception {
        System.out.println("setting up test");

        // Get the input file that will be used for the test
        String testURLPath = "http://www.slac.stanford.edu/~phansson/files/hps_java_test/HPSTestRunv3/hps-java-1.7-SNAPSHOT-050113";
        String testFileName = "egs_5.5gev_0.016x0_500mb_recoil_recon_1_hpsTestRunTrackingTest.slcio";
        URL testURL = new URL(testURLPath + "/" + testFileName);
        FileCache fileCache = new FileCache();
        File lcioInputFile = fileCache.getCachedFile(testURL);
        
        // Number of events to run over
        int nEvents = 5000; 

        // Setup the drivers
        RawTrackerHitFitterDriver hitFitter = new RawTrackerHitFitterDriver();
        hitFitter.setFitAlgorithm("Analytic");
        hitFitter.setCorrectT0Shift(true);

        HelicalTrackHitDriver hthDriver = new HelicalTrackHitDriver();
        hthDriver.setMaxSeperation(20.0);
        hthDriver.setTolerance(1.0);
        hthDriver.setLayerGeometryType("Common");
       
        
        ReadoutCleanupDriver cleanupDriver = new ReadoutCleanupDriver();
        String[] collectionNames = { "TrackerHits", "SVTRawTrackerHits", "SVTFittedRawTrackerHits"}; 
        cleanupDriver.setCollectionNames(collectionNames);
        
        commonOutputFile = new TestOutputFile(testFileName.replace(".slcio", "_common.aida"));
        ComparisonPlotsDriver plotsDriver = new ComparisonPlotsDriver(); 
        plotsDriver.setOutputFileName(commonOutputFile);
        
        // Specify the drivers that will run
        LCSimLoop lcsimLoop = new LCSimLoop();
        lcsimLoop.setLCIORecordSource(lcioInputFile);
        lcsimLoop.add(new RawTrackerHitSensorSetup());
        lcsimLoop.add(hitFitter);
        lcsimLoop.add(new DataTrackerHitDriver());
       
        // Process the events using the "Common" layer geometry
        System.out.println("Running with Common geometry");
        lcsimLoop.add(hthDriver);
        lcsimLoop.add(plotsDriver);
        lcsimLoop.add(cleanupDriver);
        lcsimLoop.loop(nEvents, null);
        lcsimLoop.execute(Command.REWIND);
        lcsimLoop.remove(cleanupDriver);
        lcsimLoop.remove(plotsDriver);
        lcsimLoop.remove(hthDriver);

        // Process the events using the "Split" layer geometry
        System.out.println("Running with Split geometry");
        hthDriver.setLayerGeometryType("Split");
        splitOutputFile = new TestOutputFile(testFileName.replace(".slcio", "_split.aida"));
        plotsDriver.setOutputFileName(splitOutputFile);
        lcsimLoop.add(hthDriver);
        lcsimLoop.add(plotsDriver);
        lcsimLoop.add(cleanupDriver);
        lcsimLoop.loop(nEvents, null);
        lcsimLoop.dispose();
    }

    class ComparisonPlotsDriver extends Driver {

        private AIDA aida = AIDA.defaultInstance();
        private IHistogram1D nTopClusters = aida.histogram1D("Number of Top Clusters", 20, 0, 20);
        private IHistogram1D nBotClusters = aida.histogram1D("Number of Bottom Clusters", 20, 0, 20);
        private IHistogram1D nTopStereoHits = aida.histogram1D("Number of Top Stereo Hits", 20, 0, 20);
        private IHistogram1D nBotStereoHits = aida.histogram1D("Number of Bottom Stereo Hits", 20, 0, 20);
        
        private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        private String stereoHitsCollectionName = "HelicalTrackHits";
        private File outputFile = null; 

        @Override
        protected void process(EventHeader event) {
            super.process(event);
            
            if(!event.hasCollection(SiTrackerHit.class, clusterCollectionName)) return;
            List<SiTrackerHit> clusters = event.get(SiTrackerHit.class, clusterCollectionName);
           
            int numberTopClusters = 0; 
            int numberBotClusters = 0; 
            for(SiTrackerHit cluster : clusters){
                if(cluster.getPositionAsVector().y() > 0)
                    numberTopClusters++; 
                else numberBotClusters++; 
            }
            
            if(!event.hasCollection(HelicalTrackHit.class, stereoHitsCollectionName)) return;
            List<HelicalTrackHit> stereoHits = event.get(HelicalTrackHit.class, stereoHitsCollectionName);
            
            int numberTopStereoHits = 0; 
            int numberBotStereoHits = 0; 
            for(HelicalTrackHit stereoHit : stereoHits){
                if(stereoHit.getPosition()[1] > 0)
                    numberTopStereoHits++; 
                else numberBotStereoHits++; 
            }
            
            nTopClusters.fill(numberTopClusters);
            nBotClusters.fill(numberBotClusters);
            nTopStereoHits.fill(numberTopStereoHits);
            nBotStereoHits.fill(numberBotStereoHits);
            
        }
        
        @Override
        protected void endOfData(){ 
            super.endOfData();
            
            if(outputFile == null){ 
                Logger.getLogger(ComparisonPlotsDriver.class.getName()).log(Level.SEVERE, "Output file was not specified");
                return;
            }
            
            try { 
                aida.saveAs(outputFile);
            } catch(IOException exception){
                Logger.getLogger(ComparisonPlotsDriver.class.getName()).log(Level.SEVERE, "Unable to save to file " + outputFile.getName());
                throw new RuntimeException("Failed to save AIDA file.", exception);
            }
        
            nTopClusters.reset();
            nBotClusters.reset(); 
            nTopStereoHits.reset();
            nBotStereoHits.reset();
        }
        

        public void setOutputFileName(File outputFile) {
            this.outputFile = outputFile; 
        }
    }

    //--- Tests ---//
    //-------------//
    
    /**
     * Test to check if the "Common" layer geometry and the
     * "Split" layer geometry are equivalent
     */
    public void testLayerGeometry() throws IOException, IllegalArgumentException {
        
        IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory(); 
        
        ITree commonTree = analysisFactory.createTreeFactory().create(commonOutputFile.getAbsolutePath());
        ITree splitTree  = analysisFactory.createTreeFactory().create(splitOutputFile.getAbsolutePath());
        
        double ksPvalue = CompareHistograms.getKolmogorovPValue( (IHistogram1D) splitTree.find("Number of Top Clusters"),
                                                                 (IHistogram1D) commonTree.find("Number of Top Clusters")); 
        assertTrue("Number of top clusters is unequal!", ksPvalue > 0.05 );
    
        ksPvalue = CompareHistograms.getKolmogorovPValue((IHistogram1D) commonTree.find("Number of Bottom Clusters"),
                                                         (IHistogram1D) splitTree.find("Number of Bottom Clusters"));
        assertTrue("Number of bottom clusters is unequal!", ksPvalue > 0.05 );
        
        ksPvalue = CompareHistograms.getKolmogorovPValue((IHistogram1D) commonTree.find("Number of Top Stereo Hits"),
                                                         (IHistogram1D) splitTree.find("Number of Top Stereo Hits"));
        assertTrue("Number of top stereo hits is unequal!", ksPvalue > 0.05 );
    
        ksPvalue = CompareHistograms.getKolmogorovPValue((IHistogram1D) commonTree.find("Number of Bottom Stereo Hits"),
                                                         (IHistogram1D) splitTree.find("Number of Bottom Stereo Hits"));
        assertTrue("Number of bottom stereo hits is unequal!", ksPvalue > 0.05 );
    }
}
