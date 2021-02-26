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
import hep.aida.ITree;
import junit.framework.TestCase;

/**
 * Base class for reconstruction ITs
 *
 * This class downloads a test EVIO input file, runs reconstruction, generates
 * test plots, and compares the plots against a set of reference plots.
 */
public class ReconTest extends TestCase {

    protected static final double DEFAULT_TOLERANCE = 5E-3;

    private String detectorName;
    private String testFileName;
    private String steering;
    private int nEvents;
    private RefPlotsDriver plotDriver;
    private File outputFile;
    private double tolerance;
    private Class<? extends ReconTest> testClass;

    /**
     * Fully qualified constructor for reconstruction IT
     * @param testClass Concrete class of the test
     * @param detectorName The name of the detector
     * @param testFileName The name of the test input file
     * @param steering The path to the steering resource
     * @param nEvents The number of events to run (-1 for all)
     * @param plotDriver The driver to create the test plots
     * @param tolerance The tolerance level for comparisons with ref plots
     */
    public ReconTest(Class<? extends ReconTest> testClass,
            String detectorName,
            String testFileName,
            String steering,
            int nEvents,
            RefPlotsDriver plotDriver,
            double tolerance) {

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

        this.outputFile = new TestOutputFile(testClass, "recon");

        this.tolerance = tolerance;
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
                "-D", "outputFile=" + outputFile.getPath(),
                "-n", String.format("%d", nEvents),
                evioInputFile.getPath()};
        System.out.println("Running EvioToLcio...");
        System.out.println("Command line args: " + String.join(" ", args));
        EvioToLcio.main(args);
        System.out.println("Done running EvioToLcio!");
    }

    protected void createPlots() {
        System.out.println("Creating plots: " + getAidaOutputPath() + ".aida");
        LCSimLoop loop = new LCSimLoop();
        plotDriver.setAidaFileName(getAidaOutputPath());
        loop.add(plotDriver);
        try {
            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Done creating plots!");
    }

    /**
     * Compare histograms between ref and test AIDA files
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
            throw new RuntimeException("Failed to load ref plots from: " + refFile.getPath());
        }

        System.out.println("Reading in test AIDA file: " + testFile.getPath());
        ITree testTree;
        try {
            testTree = af.createTreeFactory().create(testFile.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test plots from: " + testFile.getPath());
        }

        String[] refNames = refTree.listObjectNames();
        String[] refTypes = refTree.listObjectTypes();
        System.out.println("AIDA ref file object count: " + refNames.length);
        for (int i = 0; i < refNames.length; ++i) {
            String histoName = refNames[i];
            if (refTypes[i].equals("IHistogram1D")) {
                System.out.println("Checking histogram: " + histoName);
                IHistogram1D refHist = (IHistogram1D) refTree.find(histoName);
                IHistogram1D testHist = (IHistogram1D) testTree.find(histoName);
                assertEquals("Number of entries is different", refHist.entries(), testHist.entries());
                assertEquals("Mean is different", refHist.mean(), testHist.mean(), tolerance * abs(refHist.mean()));
                assertEquals("RMS is different", refHist.rms(), testHist.rms(), tolerance * abs(refHist.rms()));
            }
        }
        System.out.println("Done comparing plots!");
    }

    /**
     * Run the full test
     */
    public void testRecon() {

        long start = System.currentTimeMillis();
        runEvioToLcio();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Reconstruction for " + testClass.getSimpleName() +
                " took " + new DecimalFormat("#0.00").format((double) elapsed/1000.0) + " seconds");

        createPlots();

        comparePlots();
    }
}
