package org.hps.test.it;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.hps.evio.EvioToLcio;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalerParameters;
import org.hps.record.scalers.ScalerParametersIndex;
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
                //System.out.println(name + " is empty in event " + event.getEventNumber());
                emptyCollections.put(name, nEmpty);
            }
        }
        
        /**
         * Check scaler parameters from event.
         * 
         * @param event the lcsim event
         */
        private void checkScalerParameters(final EventHeader event) {
            
            // If this is missing an exception will be thrown.
            ScalerParameters scalerParameters = ScalerParameters.read(event);
            
            // Spot check scaler parameters from event after scaler data occurs in the data stream.            
            if (event.getEventNumber() == 25000) {                
                System.out.println();
                System.out.println("scaler parameters for event " + event.getEventNumber() + " ...");
                System.out.println(scalerParameters);
                
                // Check scaler values against answer key.
                for (ScalerParametersIndex index : ScalerParametersIndex.values()) {
                    assertEquals("Wrong " + index.name() + " value.", scalerParameters.getValue(index), SCALER_VALUES[index.ordinal()]);
                }
            }
        }

        /**
         * Process events.
         *
         * @param event the lcsim event
         */
        @Override
        public void process(final EventHeader event) {

            // Find EPICS data.
            final EpicsData epicsData = EpicsData.read(event);
            if (epicsData != null) {
                ++epicsDataCount;
            }

            // Find scaler data.
            final ScalerData scalerData = ScalerData.read(event);
            if (scalerData != null) {
                ++scalerDataCount;
            }

            // Check for presence of required collections and that they are non-empty.
            for (int i = 0; i < COLLECTION_NAMES.length; i++) {
                this.checkCollection(event, COLLECTION_TYPES[i], COLLECTION_NAMES[i]);
            }

            // Check scaler parameters from event.
            checkScalerParameters(event);
            
            ++processedCount;
        }
    }

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
     * The number of empty collections that are allowed.
     */
    private static int[] ALLOWED_EMPTY = new int[] {45, 0, 0, 0};
    
    /**
     * Expected scaler values after first occurrence of scaler collection in the data. 
     */
    private static float[] SCALER_VALUES = {
        205.47786f,
        206.38852f,
        -1.726095f,
        -0.622837f,
        179.9981f
    };
    
    /**
     * The number of EPICS collections that should be found.
     */
    private static int EPICS_DATA_COUNT = 7;
    
    /**
     * The number of scaler data collections that should be found.
     */
    private static int SCALER_DATA_COUNT = 3;
    
    /**
     * The number of events that should be processed.
     */
    private static int PROCESSED_COUNT = 251823;

    /**
     * The default input file (large file at SLAC so the pom.xml file excludes this test on non-SLAC hosts).
     */
    private static final String INPUT_FILE = "/nfs/slac/g/hps3/data/engrun/evio/hps_005772.evio.0";

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
                "-L", "WARNING", "-r", "-x", "/org/hps/steering/test/Dummy.lcsim"};
        System.out.println("Running EvioToLcio on " + INPUT_FILE + " ...");
        EvioToLcio.main(args);
        System.out.println("Done running EvioToLcio!");

        // Read in the LCIO file and run the CheckDriver on it.
        System.out.println("Checking LCIO output ...");        
        final LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(outputFile);
        final CheckDriver checkDriver = new CheckDriver();
        loop.add(checkDriver);
        loop.loop(-1);

        // Check for correct number of events processed by loop.
        System.out.println("Loop processed " + loop.getTotalCountableConsumed() + " events.");        
        assertEquals("Loop processed wrong number of events.", PROCESSED_COUNT, loop.getTotalCountableConsumed());
        
        // Check that the Driver saw the correct number of events.
        System.out.println("CheckDriver processed " + checkDriver.processedCount + " events.");
        assertEquals("Driver saw wrong number of events.", PROCESSED_COUNT, checkDriver.processedCount);

        // Require that the correct number of events with EPICS data were found.
        System.out.println("Found " + checkDriver.epicsDataCount + " events with EPICS data.");
        assertTrue("Missing EPICS data.", checkDriver.epicsDataCount == EPICS_DATA_COUNT);

        // Require that the correct number of events with scaler data were found.
        System.out.println("Found " + checkDriver.scalerDataCount + " events with scaler data.");
        assertTrue("Missing scaler data.", checkDriver.scalerDataCount == SCALER_DATA_COUNT);

        // Check if there were too many empty collections.
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
