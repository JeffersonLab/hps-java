package org.hps.conditions;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * <p>
 * This tests the basic correctness of conditions for an LCIO file generated from Engineering Run data.
 * <p>
 * Currently only ECAL conditions are handled here but SVT should be added once that information is in the
 * production database and there are runs available with Tracker data.
 * <p>
 * This test will need to be updated if the default conditions sets are changed for the EngRun.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EngRunConditionsTest extends TestCase {
    
    static String url = "http://www.lcsim.org/test/hps-java/hps_003393.0_recon_20141225-0-100.slcio";
    
    static int runNumber = 3393;
    static int nChannels = 442;
    static int runStart = 2000;
    static int runEnd = 9999;
    
    public void test() throws Exception {
        
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        DatabaseConditionsManager.getLogger().setLevel(Level.ALL);
        manager.setTag("pass0");
        manager.setXmlConfig("/org/hps/conditions/config/conditions_database_engrun.xml");
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(url));
        
        LCSimLoop loop = new LCSimLoop();
        loop.add(new EventMarkerDriver());
        loop.add(new ConditionsCheckDriver());
        loop.setLCIORecordSource(inputFile);
        loop.loop(100);
    }
    
    static void checkRunNumbers(AbstractConditionsObjectCollection<?> collection) {
        assertTrue("Run start out of range.", collection.getConditionsRecord().getRunStart() >= runStart);
        assertTrue("Run end out of range.", collection.getConditionsRecord().getRunEnd() <= runEnd);
    }
    
    static class ConditionsCheckDriver extends Driver {
        
        boolean detectorChangedCalled = false;
        
        EcalConditions ecalConditions;
        
        public void detectorChanged(Detector detector) {
                        
            assertEquals("Wrong run number.", runNumber, DatabaseConditionsManager.getInstance().getRun());
            
            DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();                       
            
            EcalChannelCollection channels = conditionsManager.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();
            assertEquals("Wrong number of channels.", nChannels, channels.size());
            assertEquals("Wrong channel collection ID.", 2, channels.getConditionsRecord().getCollectionId());
            checkRunNumbers(channels);
            
            EcalGainCollection gains = conditionsManager.getCachedConditions(EcalGainCollection.class, "ecal_gains").getCachedData();
            assertEquals("Wrong number of gains.", nChannels, gains.size());
            assertEquals("Wrong gains collection ID.", 4, gains.getConditionsRecord().getCollectionId());
            checkRunNumbers(gains);
            
            EcalCalibrationCollection calibrations = conditionsManager.getCachedConditions(EcalCalibrationCollection.class, "ecal_calibrations").getCachedData();
            assertEquals("Wrong number of calibrations.", nChannels, calibrations.size());
            assertEquals("Wrong calibrations collection ID.", 4, calibrations.getConditionsRecord().getCollectionId());
            checkRunNumbers(calibrations);
            
            //EcalLedCollection leds = conditionsManager.getCollection(EcalLedCollection.class);
            //assertEquals("Wrong number of LEDs.", nChannels, leds.size());
            //assertEquals("Wrong LEDs collection ID.", 2, leds.getConditionsRecord().getCollectionId());
            //checkRunNumbers(leds);
            
            //EcalTimeShiftCollection timeShifts = conditionsManager.getCollection(EcalTimeShiftCollection.class);
            //assertEquals("Wrong number of timeShifts.", nChannels, timeShifts.size());
            //assertEquals("Wrong LEDs collection ID.", 2, timeShifts.getConditionsRecord().getCollectionId());
            //checkRunNumbers(timeShifts);
            
            ecalConditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
            Set<EcalChannelConstants> channelConstants = new LinkedHashSet<EcalChannelConstants>();
            for (EcalChannel channel : ecalConditions.getChannelCollection().sorted()) {
                channelConstants.add(ecalConditions.getChannelConstants(channel));
            }
            assertEquals("Wrong number of channel constants.", nChannels, channelConstants.size());
            
            EcalChannelConstants channelInfo = channelConstants.iterator().next();
            assertEquals("Wrong pedestal value.", 105.78, channelInfo.getCalibration().getPedestal());
            assertEquals("Wrong noise value.", 2.74, channelInfo.getCalibration().getNoise());
            assertEquals("Wrong gain value.", 0.17, channelInfo.getGain().getGain());
                        
            detectorChangedCalled = true;
        }
        
        public void process(EventHeader event) {
            assertEquals("Wrong run number.", runNumber, event.getRunNumber());
            if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
                List<CalorimeterHit> calHits = event.get(CalorimeterHit.class, "EcalCalHits");
                for (CalorimeterHit hit : calHits) {                    
                    EcalCrystal crystal = (EcalCrystal) hit.getDetectorElement();
                    if (crystal == null) {
                        throw new RuntimeException("EcalCrystal is null.");
                    }
                    if (crystal.getIdentifier() == null) {
                        throw new RuntimeException("EcalCrystal ID is null.");
                    }
                    if (hit.getIdentifier() == null) {
                        throw new RuntimeException("The hit ID is null.");
                    }
                    assertEquals("The crystal and hit ID are different.", crystal.getIdentifier(), hit.getIdentifier());
                    
                    EcalChannel channel = ecalConditions.getChannelCollection().findGeometric(hit.getIdentifier().getValue());                    
                    EcalChannelConstants constants = ecalConditions.getChannelConstants(channel);
                    
                    assertTrue("The crystal gain is invalid.", constants.getGain().getGain() > 0.);
                    assertTrue("The crystal pedestal is invalid.", constants.getCalibration().getPedestal() > 0.);
                    assertTrue("The crystal noise is invalid.", constants.getCalibration().getNoise() > 0.);
                }
            }
        }
        
        public void endOfData() {
            if (!detectorChangedCalled) {
                throw new RuntimeException("The detectorChanged method was never called.");
            }
        }
    }
}
