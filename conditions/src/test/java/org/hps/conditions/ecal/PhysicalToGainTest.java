package org.hps.conditions.ecal;

import java.io.File;
import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.hps.conditions.TableConstants;
import org.hps.conditions.TestRunConditionsDriver;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This is a simple example of how to retrieve the gain and noise by physical ID (X,Y) in
 * the ECAL.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * 
 * @version $Id$
 * 
 */
public class PhysicalToGainTest extends TestCase {

    // This test file has a few events from each of the "good runs" of the 2012 Test Run.
    private static final String fileLocation = "http://www.lcsim.org/test/hps-java/ConditionsTest.slcio";

    // Run the test.
    public void test() throws Exception {

        // Cache a data file from the www.
        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(fileLocation));

        // Create the record loop.
        LCSimLoop loop = new LCSimLoop();

        // Configure the loop.
        loop.setLCIORecordSource(testFile);
        TestRunConditionsDriver conditionsDriver = new TestRunConditionsDriver();
        conditionsDriver.setLoadSvtConditions(false);
        loop.add(conditionsDriver);
        loop.add(new PhysicalToGainDriver());

        // Run a few events.
        loop.loop(1, null);
    }

    static class PhysicalToGainDriver extends Driver {

        static final String collectionName = "EcalReadoutHits";
        EcalConditions ecalConditions = null;
        IIdentifierHelper helper = null;
        EcalChannelCollection channels = null;

        public void detectorChanged(Detector detector) {

            // ECAL combined conditions object.
            ecalConditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();

            // List of channels.
            channels = ecalConditions.getChannelCollection();

            // ID helper.
            helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
        }

        public void process(EventHeader event) {
            if (event.hasCollection(RawCalorimeterHit.class, collectionName)) {

                // Get ECAL raw hits from event.
                List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, collectionName);
                for (RawCalorimeterHit hit : hits) {

                    // Get the ECAL channel.
                    EcalChannel channel = findChannel(hit);

                    // Get the channel data.
                    EcalChannelConstants channelData = ecalConditions.getChannelConstants(channel);

                    // Get gain and noise.
                    double gain = channelData.getGain().getGain();
                    double noise = channelData.getCalibration().getNoise();

                    // Debug print the channel data.
                    System.out.println("channel " + channel.getX() + "," + channel.getY() + " has gain " + channelData.getGain().getGain() + " and noise " + channelData.getCalibration().getNoise());
                }
            }
        }

        // Convert physical ID to gain value.
        private EcalChannel findChannel(RawCalorimeterHit hit) {
            // Make an ID object from raw hit ID.
            IIdentifier id = new Identifier(hit.getCellID());

            // Get physical field values.
            int system = helper.getValue(id, "system");
            int x = helper.getValue(id, "ix");
            int y = helper.getValue(id, "iy");

            // Create an ID to search for in channel collection.
            GeometryId geometryId = new GeometryId(helper, new int[] { system, x, y });

            // Find the ECAL channel.
            return channels.findChannel(geometryId);
        }
    }
}
