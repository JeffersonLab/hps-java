package org.hps.test.it;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hps.evio.EvioToLcio;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.test.util.TestOutputFile;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;

/**
 * Basic test of converting EVIO to LCIO using the {@link org.hps.evio.EvioToLcio} command line utility on Engineering
 * Run 2015 data.
 * <p>
 * This test checks the LCIO output for the:</br>
 * <ul>
 * <li>correct number of EPICS data collections</li>
 * <li>correct number of scaler data collections</li>
 * <li>all expected event collections</li>
 * <li>scaler parameters in event header</li>
 * </ul>
 * <p>
 * The test input is the first file of run 5772 in which scaler data appears around every 100k events.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EvioToLcioTest extends TestCase {

    /**
     * Perform basic checks of LCIO output from EVIO conversion and accumulate some statistics that will be used for
     * assertions at end of job.
     */
    class CheckDriver extends Driver {

        /**
         * Map to keep track of number of events with empty collections.
         */
        Map<String, Integer> emptyCollections = new HashMap<String, Integer>();

        /**
         * Number of EPICS data collections found.
         */
        int epicsDataCount = 0;

        /**
         * Number of events processed.
         */
        int processedCount = 0;

        /**
         * Number of scaler data collections found.
         */
        int scalerDataCount = 0;

        /**
         * Check a collection by making sure it is present in the event and incrementing a counter if it is empty.
         *
         * @param event the lcsim event
         * @param type the <code>Class</code> of the collection
         * @param name the name of the collection
         */
        private void checkCollection(final EventHeader event, final Class<?> type, final String name) {
            if (!event.hasCollection(type, name)) {
                throw new RuntimeException("Missing " + name + " collection.");
            }
            if (event.get(type, name).isEmpty()) {
                Integer nEmpty = emptyCollections.get(name);
                if (nEmpty == null) {
                    nEmpty = 0;
                }
                ++nEmpty;
                // System.out.println(name + " is empty in event " + event.getEventNumber());
                emptyCollections.put(name, nEmpty);
            }
        }

        private void checkCollections(final EventHeader event) {
            for (int i = 0; i < COLLECTION_NAMES.length; i++) {
                this.checkCollection(event, COLLECTION_TYPES[i], COLLECTION_NAMES[i]);
            }
        }

        private void checkEpicsData(final EventHeader event) {
            final EpicsData epicsData = EpicsData.read(event);
            if (epicsData != null) {
                if (epicsData.getEpicsHeader() == null) {
                    throw new RuntimeException("The EpicsData header is null.");
                }
                ++epicsDataCount;
            }
        }

        private void checkScalarData(final EventHeader event) {
            final ScalerData scalerData = ScalerData.read(event);
            if (scalerData != null) {
                ++scalerDataCount;
            }
        }

        /**
         * Process events.
         *
         * @param event the lcsim event
         */
        @Override
        public void process(final EventHeader event) {

            // Find and check EPICS data.
            this.checkEpicsData(event);

            // Find scaler data.
            this.checkScalarData(event);

            // Check for presence of required collections and that they are non-empty.
            this.checkCollections(event);

            ++processedCount;
        }
    }

    /**
     * The number of empty collections that are allowed.
     */
    private static int[] ALLOWED_EMPTY = new int[] {45, 0, 0, 0};

    /**
     * Names of collections to check.
     */
    private static String[] COLLECTION_NAMES = new String[] {"EcalReadoutHits", "FADCGenericHits", "SVTRawTrackerHits",
            "TriggerBank"};

    /**
     * Classes of collections.
     */
    private static Class<?>[] COLLECTION_TYPES = new Class<?>[] {RawTrackerHit.class, GenericObject.class,
            RawTrackerHit.class, GenericObject.class};

    /**
     * The number of EPICS collections that should be found.
     */
    private static int EPICS_DATA_COUNT = 7;

    /**
     * The default input file (large file at SLAC so the pom.xml file excludes this test on non-SLAC hosts).
     */
    private static final String INPUT_FILE = "/nfs/slac/g/hps3/data/engrun/evio/hps_005772.evio.0";

    /**
     * The number of events that should be processed.
     */
    private static int PROCESSED_COUNT = 251823;

    /**
     * The number of scaler data collections that should be found.
     */
    private static int SCALER_DATA_COUNT = 3;
   
    /**
     * Run the test.
     *
     * @throws Exception if the test throws an error
     */
    public void testEvioToLcio() throws Exception {

        // LCIO output file.
        final TestOutputFile outputFile = new TestOutputFile(EvioToLcioTest.class, "hps_005772.slcio");

        // Run the command line utility.
        final String[] args = new String[] {"-l", outputFile.getPath(), "-d", "HPS-EngRun2015-Nominal-v1", INPUT_FILE,
                "-r", "-x", "/org/hps/steering/EventMarker.lcsim"};
        System.out.println("Running EvioToLcio on " + INPUT_FILE);
        Logger.getLogger("org.hps.evio").setLevel(Level.WARNING);
        System.out.println("org.hps.evio logging level is " + Logger.getLogger("org.hps.evio").getLevel());
        EvioToLcio cnv = new EvioToLcio();
        cnv.parse(args);
        long start = System.currentTimeMillis();
        cnv.run();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Done running EvioToLcio!");
        System.out.println("conversion to LCIO took " + elapsed + " ms");
        
        // Check that the conversion did not take too long.
        assertTrue("Conversion from EVIO to LCIO took too long.", elapsed < 1000000);

        // Read in the LCIO file and run the CheckDriver on it.
        System.out.println("Checking LCIO output ...");
        final LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(outputFile);
        final CheckDriver checkDriver = new CheckDriver();
        loop.add(checkDriver);
        loop.loop(-1);

        System.out.println("conversion to LCIO took " + elapsed / loop.getTotalConsumed() + " ms/event");

        // Check that correct number of events were processed by the loop.
        System.out.println("LCSim loop processed " + loop.getTotalCountableConsumed() + " events.");
        assertEquals("LCSim loop processed the wrong number of events.", PROCESSED_COUNT, loop.getTotalCountableConsumed());

        // Check that the Driver saw the correct number of events.
        System.out.println("CheckDriver processed " + checkDriver.processedCount + " events.");
        assertEquals("Wrong number of events processed by the check Driver.", PROCESSED_COUNT, checkDriver.processedCount);

        // Check that the correct number of EPICS data collections were written out.
        System.out.println("Found " + checkDriver.epicsDataCount + " events with EPICS data.");
        assertTrue("EPICS data count is wrong.", checkDriver.epicsDataCount == EPICS_DATA_COUNT);

        // Check that the correct number of scaler data collections were written out.
        System.out.println("Found " + checkDriver.scalerDataCount + " events with scaler data.");
        assertTrue("Scaler data count is wrong.", checkDriver.scalerDataCount == SCALER_DATA_COUNT);

        // Check that there were not too many empty collections.  
        for (int i = 0; i < COLLECTION_NAMES.length; i++) {
            final String collection = COLLECTION_NAMES[i];
            final Integer nEmpty = checkDriver.emptyCollections.get(collection);
            if (nEmpty != null) {
                System.out.println(collection + " had " + nEmpty + " empty collections.");
                assertTrue(collection + " had too many empty collections.", nEmpty <= ALLOWED_EMPTY[i]);
            }
        }
    }
}
