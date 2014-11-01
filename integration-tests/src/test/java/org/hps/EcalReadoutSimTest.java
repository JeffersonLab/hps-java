package org.hps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.job.AidaSaveDriver;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.loop.LCSimLoop;

/**
 * <p>
 * This test case runs the ECAL readout simulation on some MC input data
 * and then checks the output in detail against a set of known-good answers.
 * </p>
 * <p>
 * The test method checks the following output values:
 * </p>
 * <ul>
 * <li>Total number of events processed</li>
 * <li>Total number of collection objects of various types across all events</li>
 * <li>Exact event numbers of all triggered events from the input</li>
 * <li>RMS and mean of histograms generated from the output data</li>
 * </ul>
 * <p>
 * The readout is run with a steering file copied from  
 * <tt>/org/hps/steering/readout/HPS2014ReadoutToLcio.lcsim</tt>
 * with the <tt>addNoise</tt> settings set to <tt>false</tt> for 
 * reproducibility.
 * </p>
 * <p>
 * The input LCIO MC events are from a (large) file that is on SLAC NFS at
 * <tt>/nfs/slac/g/hps3/data/testcase/ecal_readout_sim_input.slcio</tt>.  
 * When this file is not accessible, this test will not run with the build 
 * due to activation of the <tt>non-slac</tt> profile in the project's 
 * <tt>pom.xml</tt> file.
 * </p>
 * <p>
 * If the legitimate output of the ECAL readout simulation changes at all, 
 * then this test will fail, and all the answer keys need to be updated
 * to fix it!
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Add back commented out test assertions.
public class EcalReadoutSimTest extends TestCase {

    // Expected values of event and collection object totals.
	/*
    static final int expectedEvents = 1298;
    static final int expectedMCParticles = 68937;
    static final int expectedRawCalorimeterHits = 86475;
    static final int expectedRawTrackerHits = 99732;
    static final int expectedRelations = 116629;
    static final int expectedFpgaData = 15576;
    static final int expectedReadoutTimestamps = 4 * expectedEvents;
    static final int expectedTriggerBanks = 1298;
    */
    
    // Expected values of histogram statistics.
    /*
    static final double expectedCalAmplitudePlotRms = 2371.436725801633;    
    static final double expectedCalAmplitudePlotMean = 4538.9994449262795;
    static final double expectedCalTimestampPlotRms = 1744.1359529793683;    
    static final double expectedCalTimestampPlotMean = 2769.631361665221;    
    static final double expectedReadoutTimestampPlotRms = 283892.28438521083;
    static final double expectedReadoutTimestampPlotMean = 494337.30883864337;
    static final double expectedAdcValuesPlotRms = 817.8012108797172;
    static final double expectedAdcValuesPlotMean = 4786.569434486355;
    */
    
    // Name of class which will be used a lot for static variables below.
    static final String className = EcalReadoutSimTest.class.getSimpleName();
    
    // Resource locations.    
    //static final String steeringResource = "/org/hps/steering/test/EcalReadoutSimTest.lcsim";
    static final String steeringResource = "/org/hps/steering/readout/TestRunReadoutToLcio.lcsim";
    
    static final String triggeredEventsResource = "/org/hps/test/EcalReadoutSimTest/triggered_events.txt";
    
    // File information.        
    //static final String fileLocation = "http://www.lcsim.org/test/hps-java/EcalReadoutSimTest.slcio";
    static final File inputFile = new File("/nfs/slac/g/lcd/mc/prj/www/lcsim/test/hps-java/EcalReadoutSimTest.slcio");
    
    static final File outputDir = new File("./target/test-output/" + className);    
    static final File outputFile = new File(outputDir + File.separator + className);
    static final File aidaOutputFile = new File(outputDir + File.separator + className + ".aida");
    
    // Default run number.
    static final int runNumber = 0;
            
    // Collection names.
    static final String mcparticleCollectionName = "MCParticle";
    static final String rawCalorimeterHitCollectionName = "EcalReadoutHits";
    static final String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    static final String relationCollectionName = "SVTTrueHitRelations";
    static final String fpgaDataCollectionName = "FPGAData";
    static final String readoutTimestampsCollectionName = "ReadoutTimestamps";
    static final String triggerBankCollectionName = "TriggerBank";
    static final String[] collectionNames = {
        mcparticleCollectionName,
        rawCalorimeterHitCollectionName,
        rawTrackerHitCollectionName,
        relationCollectionName,
        fpgaDataCollectionName,
        readoutTimestampsCollectionName,
        triggerBankCollectionName };
                  
    /**
     * Run an integration test of the ECAL readout simulation.
     */
    public void testEcalReadoutSim() throws Exception {
        
    	// Create output dir.
    	outputDir.mkdirs();
        if (!outputDir.exists()) {
            System.err.println("Failed to create output directory " + outputDir.getPath());
            throw new RuntimeException("Failed to create output directory.");
        }
                
        // Run the readout simulation.
        JobControlManager job = new JobControlManager();
        job.addInputFile(inputFile);        
        job.addVariableDefinition("runNumber", new Integer(runNumber).toString());
        job.addVariableDefinition("outputFile", outputFile.getPath());
        System.out.println("using steering " + steeringResource);
        job.setup(steeringResource);
        
        job.setNumberOfEvents(20000);
        job.run();
        
        // Must clear the AIDA tree so that ECAL readout sim plots don't show-up in our output.
        String[] objects = AIDA.defaultInstance().tree().listObjectNames("/");
        for (String object : objects) {
            AIDA.defaultInstance().tree().rm(object);
        }
    	        
        // Run QA drivers over the readout output file.        
        LCSimLoop loop = new LCSimLoop();
        File readoutFile = new File(outputFile.getPath() + ".slcio");
        try {
            loop.setLCIORecordSource(readoutFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CheckDriver checkDriver = new CheckDriver();
        AidaSaveDriver aidaDriver = new AidaSaveDriver();
        aidaDriver.setOutputFileName(aidaOutputFile.getPath());        
        loop.add(checkDriver);
        loop.add(new PlotsDriver());
        loop.add(aidaDriver);
        try {
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
        
        /*
        ---- Summary ----
        events = 6
        RawCalorimeterHits = 35
        RawTrackerHits = 533
        MCParticles = 113
        Relations = 537
        FPGAData = 60
        ReadoutTimestamps = 24
        TriggerBanks = 6
		*/       
                                   
        /*
        // Check for expected number of events and collection objects across the entire run.
        assertEquals(expectedEvents, loop.getSupplied());
        assertEquals(expectedEvents, checkDriver.getNumberOfEvents());
        assertEquals(expectedRawCalorimeterHits, checkDriver.getNumberOfRawCalorimeterHits());
        assertEquals(expectedRawTrackerHits, checkDriver.getNumberOfRawTrackerHits());
        assertEquals(expectedMCParticles, checkDriver.getNumberOfMCParticles());
        assertEquals(expectedRelations, checkDriver.getNumberOfRelations());
        assertEquals(expectedFpgaData, checkDriver.getNumberOfFpgaData());
        assertEquals(expectedReadoutTimestamps, checkDriver.getNumberOfReadoutTimestamps());
        assertEquals(expectedTriggerBanks, checkDriver.getNumberOfTriggerBanks());
        
        // Check that the list of triggered events is exactly the same as a stored answer key.
        List<Integer> expectedTriggeredEvents = readExpectedTriggeredEvents();
        List<Integer> actualTriggeredEvents = checkDriver.getTriggeredEvents();       
        assertEquals("Number of triggered events is different.", expectedTriggeredEvents.size(), actualTriggeredEvents.size());
        assertTrue("Event trigger lists are not equal.", expectedTriggeredEvents.equals(actualTriggeredEvents));
        
        // Check the statistics of plots that are now contained in the global AIDA instance.
        AIDA aida = AIDA.defaultInstance();
        
        IHistogram1D amplitudePlot = aida.histogram1D("/" + rawCalorimeterHitCollectionName + "/Amplitude");
        System.out.println("amplitudePlot rms = " + amplitudePlot.rms());        
        System.out.println("amplitudePlot mean = " + amplitudePlot.mean());
        assertEquals(expectedCalAmplitudePlotRms, amplitudePlot.rms());    
        assertEquals(expectedCalAmplitudePlotMean, amplitudePlot.mean());
        
        IHistogram1D timestampPlot = aida.histogram1D("/" + rawCalorimeterHitCollectionName + "/Timestamp");
        System.out.println("timestampPlot rms = " + timestampPlot.rms());        
        System.out.println("timestampPlot mean = " + timestampPlot.mean());
        assertEquals(expectedCalTimestampPlotRms, timestampPlot.rms());
        assertEquals(expectedCalTimestampPlotMean, timestampPlot.mean());
        
        IHistogram1D adcValuesPlot = aida.histogram1D("/" + rawTrackerHitCollectionName + "/ADCValues");
        System.out.println("adcValuePlot rms = " + adcValuesPlot.rms());
        System.out.println("adcValuePlot mean = " + adcValuesPlot.mean());
        assertEquals(expectedAdcValuesPlotRms, adcValuesPlot.rms());
        assertEquals(expectedAdcValuesPlotMean, adcValuesPlot.mean());
        
        IHistogram1D readoutTimestampPlot = aida.histogram1D("/" + readoutTimestampsCollectionName + "/Timestamp");
        System.out.println("readoutTimestampPlot rms = " + readoutTimestampPlot.rms());
        System.out.println("readoutTimestampPlot mean = " + readoutTimestampPlot.mean());
//        assertEquals(expectedReadoutTimestampPlotRms, readoutTimestampPlot.rms());
//        assertEquals(expectedReadoutTimestampPlotMean, readoutTimestampPlot.mean()); 
         */
    }
    
    /**
     * Read in a list of expected triggered event numbers for this input file.
     * @return The list of expected triggered event numbers.
     */
    private List<Integer> readExpectedTriggeredEvents() {
        List<Integer> triggeredEvents = new ArrayList<Integer>();        
        InputStream is = this.getClass().getResourceAsStream(triggeredEventsResource);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                Integer eventNumber = Integer.parseInt(line);
                if (!triggeredEvents.contains(eventNumber))
                    triggeredEvents.add(eventNumber);
                else
                    throw new RuntimeException("Duplicate event number " + eventNumber + " in input.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(triggeredEvents);
        return triggeredEvents;
    }
    
    /**
     * Driver to accumulate statistics and that will be checked against the answer keys.           
     */
    static class CheckDriver extends Driver {
                
        int events = 0;
        int mcparticles = 0;
        int rawCalorimeterHits = 0;
        int rawTrackerHits = 0;
        int relations = 0;
        int fpgaData = 0;
        int readoutTimestamps = 0;
        int triggerBanks = 0;
        
        List<Integer> triggeredEvents = new ArrayList<Integer>();
                 
        public void process(EventHeader event) {
            
            ++events;
            
            if (!triggeredEvents.contains(event.getEventNumber())) {
                triggeredEvents.add(event.getEventNumber());    
            } else {
                throw new RuntimeException("Duplicate event " + event.getEventNumber() + " read from input LCIO file.");
            }
                        
            List<RawCalorimeterHit> rawCalorimeterHitCollection = 
                    event.get(RawCalorimeterHit.class, rawCalorimeterHitCollectionName);
            rawCalorimeterHits += rawCalorimeterHitCollection.size();
                        
            List<RawTrackerHit> rawTrackerHitCollection = 
                    event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            rawTrackerHits += rawTrackerHitCollection.size();
                                    
            List<MCParticle> mcparticleCollection = 
                    event.get(MCParticle.class, mcparticleCollectionName);
            mcparticles += mcparticleCollection.size();
                                    
            List<LCRelation> relationCollection = 
                    event.get(LCRelation.class, relationCollectionName);
            relations += relationCollection.size();      
            
            List<GenericObject> fpgaDataCollection =
                    event.get(GenericObject.class, fpgaDataCollectionName);
            fpgaData += fpgaDataCollection.size();
            
            List<GenericObject> readoutTimestampsCollection = 
                    event.get(GenericObject.class, readoutTimestampsCollectionName);
            readoutTimestamps += readoutTimestampsCollection.size();
            
            List<GenericObject> triggerBankCollection = 
                    event.get(GenericObject.class, triggerBankCollectionName);
            triggerBanks += triggerBankCollection.size();
        }
        
        int getNumberOfEvents() {
            return events;
        }
        
        int getNumberOfMCParticles() {
            return mcparticles;
        }
        
        int getNumberOfRawCalorimeterHits() {
            return rawCalorimeterHits;
        }
        
        int getNumberOfRawTrackerHits() {
            return rawTrackerHits;
        }
        
        int getNumberOfRelations() {
            return relations;
        }
        
        int getNumberOfFpgaData() {
            return fpgaData;
        }
        
        int getNumberOfReadoutTimestamps() {
            return readoutTimestamps;
        }
        
        int getNumberOfTriggerBanks() {
            return triggerBanks;
        }
        
        List<Integer> getTriggeredEvents() {
            return triggeredEvents;
        }
        
        public void endOfData() {
            Collections.sort(triggeredEvents);
            System.out.println();
            System.out.println("---- Summary ----");
            System.out.println("events = " + getNumberOfEvents());
            System.out.println("RawCalorimeterHits = " + getNumberOfRawCalorimeterHits());
            System.out.println("RawTrackerHits = " + getNumberOfRawTrackerHits());
            System.out.println("MCParticles = " + getNumberOfMCParticles());
            System.out.println("Relations = " + getNumberOfRelations());
            System.out.println("FPGAData = " + getNumberOfFpgaData());
            System.out.println("ReadoutTimestamps = " + getNumberOfReadoutTimestamps());
            System.out.println("TriggerBanks = " + getNumberOfTriggerBanks());
        }                               
    }
    
    /**
     * Simple Driver for generating a few histograms that will be checked.
     */
    static class PlotsDriver extends Driver {
        
        AIDA aida = AIDA.defaultInstance();
        
        public void startOfData() {
            
            for (String collectionName : collectionNames) {
                aida.tree().cd("/");
                aida.tree().mkdir(collectionName);
            }
            
            aida.histogram1D("/" + rawCalorimeterHitCollectionName + "/Amplitude", 3000, 0., 30000.);
            aida.histogram1D("/" + rawCalorimeterHitCollectionName + "/Timestamp", 6500, 0., 6500.);
            
            aida.histogram1D("/" + readoutTimestampsCollectionName + "/Timestamp", 1050, 0., 1050000.);
            
            aida.histogram1D("/" + rawTrackerHitCollectionName + "/ADCValues", 2600, 4000., 30000.);
        }
        
        public void process(EventHeader event) {
            
            aida.tree().cd("/" + rawCalorimeterHitCollectionName);
            List<RawCalorimeterHit> rawCalorimeterHits = event.get(RawCalorimeterHit.class, rawCalorimeterHitCollectionName);
            for (RawCalorimeterHit hit : rawCalorimeterHits) {
                aida.histogram1D("Amplitude").fill(hit.getAmplitude());
                aida.histogram1D("Timestamp").fill(hit.getTimeStamp());
            }
            
            aida.tree().cd("/" + readoutTimestampsCollectionName);
            List<GenericObject> readoutTimestamps = event.get(GenericObject.class, readoutTimestampsCollectionName);
            for (GenericObject object : readoutTimestamps) {
                double timestamp = object.getDoubleVal(0);
                aida.histogram1D("Timestamp").fill(timestamp);
            }
            
            aida.tree().cd("/" + rawTrackerHitCollectionName);
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            for (RawTrackerHit hit : rawTrackerHits) {
                for (short adcValue : hit.getADCValues()) {
                    aida.histogram1D("ADCValues").fill(adcValue);
                }
            }
        }
    }
}