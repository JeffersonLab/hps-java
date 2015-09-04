package org.hps.conditions;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.api.BaseConditionsObjectCollection;
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
 * This tests the basic correctness of conditions for an LCIO file generated from Engineering Run data.
 * <p>
 * Currently only ECAL conditions are handled here but SVT should be added once that information is in the production
 * database and there are runs available with Tracker data.
 * <p>
 * This test will need to be updated if the default conditions sets are changed for the Eng Run.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EngRunConditionsTest extends TestCase {

    /**
     * This {@link org.lcsim.util.Driver} performs basic conditions data checks.
     */
    static class ConditionsCheckDriver extends Driver {

        /**
         * Collection ID of calibrations.
         */
        private static final Integer CALIBRATIONS_COLLECTION_ID = 4;

        /**
         * Answer for gain value check of single channel.
         */
        private static final double GAIN_ANSWER = 0.17;

        /**
         * Collection ID of gains.
         */
        private static final Integer GAINS_COLLECTION_ID = 4;

        /**
         * Answer for noise value check of single channel.
         */
        private static final double NOISE_ANSWER = 2.74;

        /**
         * Answer for pedestal value check of single channel.
         */
        private static final double PEDESTAL_ANSWER = 105.78;

        /**
         * Flag if {@link #detectorChanged(Detector)} is activated.
         */
        private boolean detectorChangedCalled = false;

        /**
         * Combined ECAL conditions object.
         */
        private EcalConditions ecalConditions;

        /**
         * Hook when conditions are updated. Performs various checks for test.
         *
         * @param detector the detector object
         */
        @Override
        public void detectorChanged(final Detector detector) {

            assertEquals("Wrong run number.", RUN_NUMBER, DatabaseConditionsManager.getInstance().getRun());

            final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

            final EcalChannelCollection channels = conditionsManager.getCachedConditions(EcalChannelCollection.class,
                    "ecal_channels").getCachedData();
            assertEquals("Wrong number of channels.", CHANNEL_COUNT, channels.size());
            // assertEquals("Wrong channel collection ID.", 2, channels.getConditionsRecord().getCollectionId());
            checkRunNumbers(channels);

            final EcalGainCollection gains = conditionsManager.getCachedConditions(EcalGainCollection.class,
                    "ecal_gains").getCachedData();
            assertEquals("Wrong number of gains.", CHANNEL_COUNT, gains.size());
            // assertEquals("Wrong gains collection ID.", GAINS_COLLECTION_ID,
            // gains.getConditionsRecord().getCollectionId());
            checkRunNumbers(gains);

            final EcalCalibrationCollection calibrations = conditionsManager.getCachedConditions(
                    EcalCalibrationCollection.class, "ecal_calibrations").getCachedData();
            assertEquals("Wrong number of calibrations.", CHANNEL_COUNT, calibrations.size());
            // assertEquals("Wrong calibrations collection ID.", CALIBRATIONS_COLLECTION_ID,
            // calibrations.getConditionsRecord().getCollectionId());
            checkRunNumbers(calibrations);

            // EcalLedCollection leds = conditionsManager.getCollection(EcalLedCollection.class);
            // assertEquals("Wrong number of LEDs.", nChannels, leds.size());
            // assertEquals("Wrong LEDs collection ID.", 2, leds.getConditionsRecord().getCollectionId());
            // checkRunNumbers(leds);

            // EcalTimeShiftCollection timeShifts = conditionsManager.getCollection(EcalTimeShiftCollection.class);
            // assertEquals("Wrong number of timeShifts.", nChannels, timeShifts.size());
            // assertEquals("Wrong LEDs collection ID.", 2, timeShifts.getConditionsRecord().getCollectionId());
            // checkRunNumbers(timeShifts);

            this.ecalConditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions")
                    .getCachedData();
            final Set<EcalChannelConstants> channelConstants = new LinkedHashSet<EcalChannelConstants>();
            for (final EcalChannel channel : this.ecalConditions.getChannelCollection().sorted()) {
                channelConstants.add(this.ecalConditions.getChannelConstants(channel));
            }
            assertEquals("Wrong number of channel constants.", CHANNEL_COUNT, channelConstants.size());

            final EcalChannelConstants channelInfo = channelConstants.iterator().next();
            assertEquals("Wrong pedestal value.", PEDESTAL_ANSWER, channelInfo.getCalibration().getPedestal());
            assertEquals("Wrong noise value.", NOISE_ANSWER, channelInfo.getCalibration().getNoise());
            assertEquals("Wrong gain value.", GAIN_ANSWER, channelInfo.getGain().getGain());

            this.detectorChangedCalled = true;
        }

        /**
         * End of data hook. Checks that {@link #detectorChanged(Detector)} was called.
         */
        @Override
        public void endOfData() {
            if (!this.detectorChangedCalled) {
                throw new RuntimeException("The detectorChanged method was never called.");
            }
        }

        /**
         * Event processing. Performs a few conditions system and geometry checks.
         *
         * @param event the LCSim event
         */
        @Override
        public void process(final EventHeader event) {
            assertEquals("Wrong run number.", RUN_NUMBER, event.getRunNumber());
            if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
                final List<CalorimeterHit> calHits = event.get(CalorimeterHit.class, "EcalCalHits");
                for (final CalorimeterHit hit : calHits) {
                    final EcalCrystal crystal = (EcalCrystal) hit.getDetectorElement();
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

                    final EcalChannel channel = this.ecalConditions.getChannelCollection().findGeometric(
                            hit.getIdentifier().getValue());
                    final EcalChannelConstants constants = this.ecalConditions.getChannelConstants(channel);

                    assertTrue("The crystal gain is invalid.", constants.getGain().getGain() > 0.);
                    assertTrue("The crystal pedestal is invalid.", constants.getCalibration().getPedestal() > 0.);
                    assertTrue("The crystal noise is invalid.", constants.getCalibration().getNoise() > 0.);
                }
            }
        }
    }

    /**
     * Number of ECAL channels.
     */
    private static final int CHANNEL_COUNT = 442;

    /**
     * Number of events to process.
     */
    private static final int EVENT_COUNT = 100;

    /**
     * The run number to use for the test.
     */
    private static final int RUN_NUMBER = 3393;

    /**
     * Data file URL.
     */
    private static final String URL = "http://www.lcsim.org/test/hps-java/hps_003393.0_recon_20141225-0-100.slcio";

    /**
     * Check the run numbers of the conditions records.
     *
     * @param collection the conditions collection
     */
    static void checkRunNumbers(final BaseConditionsObjectCollection<?> collection) {
        // assertTrue("Run start out of range.", collection.getConditionsRecord().getRunStart() >= RUN_START);
        // assertTrue("Run end out of range.", collection.getConditionsRecord().getRunEnd() <= RUN_END);
    }

    /**
     * Test Eng Run conditions.
     *
     * @throws Exception if there is an error (record processing problem)
     */
    public void test() throws Exception {

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        DatabaseConditionsManager.getLogger().setLevel(Level.ALL);
        manager.addTag("pass0");
        manager.setXmlConfig("/org/hps/conditions/config/conditions_database_engrun.xml");

        final FileCache cache = new FileCache();
        final File inputFile = cache.getCachedFile(new URL(URL));

        final LCSimLoop loop = new LCSimLoop();
        loop.add(new EventMarkerDriver());
        loop.add(new ConditionsCheckDriver());
        loop.setLCIORecordSource(inputFile);
        loop.loop(EVENT_COUNT);
    }
}
