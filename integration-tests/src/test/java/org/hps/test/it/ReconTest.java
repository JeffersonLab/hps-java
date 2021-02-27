package org.hps.test.it;

import static java.lang.Math.abs;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.hps.evio.EvioToLcio;
import org.hps.util.test.TestOutputFile;
import org.hps.util.test.TestUtil;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.loop.LCSimLoop;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;
import junit.framework.TestCase;

/**
 * Base class for reconstruction ITs
 *
 * This class downloads a test EVIO input file, runs reconstruction, generates
 * test histograms, and compares them against a reference file.
 *
 * Test-specific parameters such as detector name and test file can be configured
 * through one of the constructors.
 */
public abstract class ReconTest extends TestCase {

    /**
     * The default tolerance level for histogram statistics comparison
     */
    protected static final double DEFAULT_TOLERANCE = 1E-5;

    /**
     * The concrete class of the test, set for convenience
     */
    private Class<? extends ReconTest> testClass;

    /**
     * The name of the detector geometry
     */
    private String detectorName;

    /**
     * The input file from JLAB
     */
    private String testFileName;

    /**
     * The lcsim steering file resource
     */
    private String steering;

    /**
     * The number of events to run (use -1 for unlimited)
     */
    private int nEvents;

    /**
     * A driver instance to generate the local AIDA file
     */
    private RefDriver plotDriver;

    /**
     * The LCIO output file (not set by the user)
     */
    private File lcioOutputFile;

    /**
     * The tolerance for histogram statistics comparison
     */
    private double tolerance;

    /**
     * The max time per event to allow without failing
     * (should be generous to allow for performance differences in various environments)
     */
    private long maxEventTime;

    /**
     * Whether to print out the 1D histogram comparison table
     */
    private boolean printComparisonTable = false;

    /**
     * Whether to print out memory usage statistics after running recon
     */
    private boolean printMemoryUsage = false;

    /**
     * Fully qualified constructor for reconstruction IT
     * @param testClass Concrete class of the test
     * @param detectorName The name of the detector
     * @param testFileName The name of the test input file
     * @param steering The path to the steering resource
     * @param nEvents The number of events to run (-1 for all)
     * @param plotDriver The driver to create the test plots
     * @param tolerance The tolerance level for comparisons with reference plots
     * @param maxEventTime Max allowed time per event in milliseconds, inclusive (-1 for no check)
     * @param printComparisonTable True to print the histogram comparison table
     * @param printMemoryUsage True to print memory usage after running reconstruction
     */
    public ReconTest(Class<? extends ReconTest> testClass,
            String detectorName,
            String testFileName,
            String steering,
            int nEvents,
            RefDriver plotDriver,
            double tolerance,
            long maxEventTime,
            boolean printComparisonTable,
            boolean printMemoryUsage) {

        if (detectorName == null) {
            throw new NullPointerException("detectorName is null");
        }
        this.detectorName = detectorName;

        if (testFileName == null) {
            throw new NullPointerException("testFileName is null");
        }
        this.testFileName = testFileName;

        if (steering == null) {
            throw new NullPointerException("steering is null");
        }
        this.steering = steering;

        this.nEvents = nEvents;

        if (plotDriver == null) {
            throw new NullPointerException("plotDriver is null");
        }
        this.plotDriver = plotDriver;

        if (testClass == null) {
            throw new NullPointerException("testClass is null");
        }
        this.testClass = testClass;

        this.lcioOutputFile = new TestOutputFile(testClass, "recon");

        this.tolerance = tolerance;

        this.maxEventTime = maxEventTime;

        this.printComparisonTable = printComparisonTable;

        this.printMemoryUsage = printMemoryUsage;
    }

    /**
     * Simplified constructor using some defaults
     * @param testClass Concrete class of the test
     * @param detectorName The name of the detector
     * @param testFileName The name of the test input file
     * @param steering The path to the steering resource
     * @param plotDriver The driver to create the test plots
     */
    public ReconTest(Class<? extends ReconTest> testClass,
            String detectorName,
            String testFileName,
            String steering,
            RefDriver plotDriver) {
        this(testClass, detectorName, testFileName,
                steering, -1 /* nevents */, plotDriver, DEFAULT_TOLERANCE,
                -1 /* max time */ , false /* print table */, false /* memory usage */);
    }

    /**
     * Get the path to the output AIDA file with no extension
     * @return Path to the output AIDA file with no extension
     */
    private String getAidaOutputPath() {
        return new TestOutputFile(getClass().getSimpleName()).getPath() +
                File.separator + this.getClass().getSimpleName();
    }

    /**
     * Run the EVIO to LCIO tool which will also run the reconstruction
     * using the configured steering file
     */
    protected void runEvioToLcio() {
        System.out.println("Downloading test file: " + testFileName);
        File evioInputFile = TestUtil.downloadTestFile(testFileName);
        String args[] = {
                "-r",
                "-x", steering,
                "-d", detectorName,
                "-D", "outputFile=" + lcioOutputFile.getPath(),
                "-n", String.format("%d", nEvents),
                evioInputFile.getPath()};
        System.out.println("Running EvioToLcio...");
        System.out.println("Command line args: " + String.join(" ", args));
        EvioToLcio.main(args);
        System.out.println("Done running EvioToLcio!");
    }

    /**
     * Create plots based on the current version of the reconstruction for
     * comparison against the reference histograms
     */
    protected void createPlots() {
        System.out.println("Creating test plots: " + getAidaOutputPath() + ".aida");
        LCSimLoop loop = new LCSimLoop();
        plotDriver.setAidaFileName(getAidaOutputPath());
        loop.add(plotDriver);
        try {
            loop.setLCIORecordSource(new File(lcioOutputFile.getPath() + ".slcio"));
            loop.loop(-1, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Done creating test plots!");
    }

    /**
     * Compare histograms between reference and test AIDA files
     */
    protected void comparePlots() {
        System.out.println("Comparing plots...");
        AIDA aida = AIDA.defaultInstance();
        final IAnalysisFactory af = aida.analysisFactory();

        File refFile = TestUtil.downloadRefPlots(testClass.getSimpleName());
        File testFile = new File(getAidaOutputPath() + ".aida");

        System.out.println("Reading in ref AIDA file: " + refFile.getPath());
        ITree refTree;
        try {
            refTree = af.createTreeFactory().create(refFile.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to open ref plots");
        }

        System.out.println("Reading in test AIDA file: " + testFile.getPath());
        ITree testTree;
        try {
            testTree = af.createTreeFactory().create(testFile.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to open test plots");
        }

        if (printComparisonTable) {
            printComparisonTable(refTree, testTree);
        }

        System.out.println("Comparing histograms for: " + this.testClass.getSimpleName());
        compareTrees(refTree, testTree);

        System.out.println("Done comparing histograms!");
    }

    /**
     * Compare reference and test AIDA trees
     * @param refTree The reference AIDA tree
     * @param testTree The test AIDA tree
     */
    protected void compareTrees(ITree refTree, ITree testTree) {
        String[] refNames = refTree.listObjectNames("/", true);
        String[] refTypes = refTree.listObjectTypes("/", true);
        //System.out.println("AIDA ref file object count: " + refNames.length);
        for (int i = 0; i < refNames.length; ++i) {
            String histName = refNames[i];
            String type = refTypes[i];
            if (type.startsWith("IHistogram")) {
                System.out.print(histName + " ... ");
                if (type.equals("IHistogram1D")) {
                    compareHistograms((IHistogram1D) refTree.find(histName),
                            (IHistogram1D) testTree.find(histName));
                } else if (type.equals("IHistogram2D")) {
                    compareHistograms((IHistogram2D) refTree.find(histName),
                            (IHistogram2D) testTree.find(histName));
                }
                System.out.println("ok");
            }
        }
    }

    /**
     * Compare the number of entries, mean and RMS of two 1D histograms
     * and assert they are equal within the configured tolerance
     * @param refHist The reference AIDA tree
     * @param testHist The test AIDA tree
     */
    protected void compareHistograms(IHistogram1D refHist, IHistogram1D testHist) {
        assertEquals("Number of entries is different for: " + refHist.title(), refHist.entries(), testHist.entries());
        assertEquals("Mean is different for: " + refHist.title(), refHist.mean(), testHist.mean(), tolerance * abs(refHist.mean()));
        assertEquals("RMS is different for: " + refHist.title(), refHist.rms(), testHist.rms(), tolerance * abs(refHist.rms()));
    }

    /**
     * Compare the number of entries, mean and RMS of two 2D histograms
     * and assert they are equal within the configured tolerance
     * @param refHist The reference AIDA tree
     * @param testHist The test AIDA tree
     */
    protected void compareHistograms(IHistogram2D refHist, IHistogram2D testHist) {
        assertEquals("Number of entries is different for: " + refHist.title(), refHist.entries(), testHist.entries());
        assertEquals("X Mean is different for: " + refHist.title(), refHist.meanX(), testHist.meanX(), tolerance * abs(refHist.meanX()));
        assertEquals("X RMS is different for: " + refHist.title(), refHist.rmsX(), testHist.rmsX(), tolerance * abs(refHist.rmsX()));
        assertEquals("Y Mean is different for: " + refHist.title(), refHist.meanY(), testHist.meanY(), tolerance * abs(refHist.meanY()));
        assertEquals("Y RMS is different for: " + refHist.title(), refHist.rmsY(), testHist.rmsY(), tolerance * abs(refHist.rmsY()));

    }

    /**
     * Print a comparison table of 1D histograms from reference and test plots
     * @param refTree The reference AIDA tree
     * @param testTree The test AIDA tree
     */
    private void printComparisonTable(ITree refTree, ITree testTree) {
        String[] refNames = refTree.listObjectNames("/", true);
        String[] refTypes = refTree.listObjectTypes("/", true);
        System.out.println("\nHistogram\t\t\tRef Entries\tTest Entries\tRef Mean\tTest Mean\tRef RMS\t\tTest RMS\n");
        for (int i = 0; i < refNames.length; ++i) {
            String histoName = refNames[i];
            if (refTypes[i].equals("IHistogram1D")) {
                IHistogram1D refHist = (IHistogram1D) refTree.find(histoName);
                IHistogram1D testHist = (IHistogram1D) testTree.find(histoName);
                System.out.print(String.format("%-" + 31 + "s", refHist.title()) + ' ');
                System.out.printf("%d\t\t%d\t\t%.8f\t%.8f\t%.8f\t%.8f\n",
                        refHist.entries(), testHist.entries(), refHist.mean(),
                        testHist.mean(), refHist.rms(), testHist.rms());
                System.out.flush();
            }
        }
        System.out.println();
    }

    /**
     * Print memory usage statistics
     */
    private void printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        int mb = 1024 * 1024;
        System.out.println("\nMemory Usage");
        System.out.printf("Total: %d mb\n", runtime.totalMemory() / mb);
        System.out.printf("Free: %d mb\n", runtime.freeMemory() / mb);
        System.out.printf("Max: %d mb\n", runtime.maxMemory() / mb);
        System.out.printf("Used: %d mb\n", (runtime.totalMemory() - runtime.freeMemory()) / mb);
        System.out.println();
    }

    /**
     * Run the full test
     */
    public void testRecon() {

        // Convert from EVIO to LCIO and run the reconstruction, timing it
        long start = System.currentTimeMillis();
        runEvioToLcio();
        long elapsed = System.currentTimeMillis() - start;
        long perEvent = elapsed/nEvents;
        System.out.println("\nReconstruction for " + testClass.getSimpleName() +
                " took " + new DecimalFormat("#0.00").format((double) elapsed/1000.0) + " seconds" +
                " which is " + perEvent + " millis/event");

        // Check if the time per event is more than allowed
        if (maxEventTime > 0) {
            assertTrue("Event processing took too long per event: " + perEvent + " millis",
                    perEvent <= maxEventTime);
        }

        // Print memory usage from running reconstruction
        if (printMemoryUsage) {
            printMemoryUsage();
        }

        // Create the test plots
        createPlots();

        // Compare the test plots to the reference plots
        comparePlots();
    }
}
