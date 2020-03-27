package org.hps.test.it;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.evio.EvioToLcio;
import org.hps.job.JobManager;
import org.lcsim.util.cache.FileCache;
import org.hps.test.util.TestOutputFile;

/**
 *
 * @author Norman A. Graf
 */
public class PhysRun2019MCReconTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/2019/";
    static final String testFileName = "hpsForwardFullEnergyElectrons_z-7.5_top_0_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2019-v2-4pt5.slcio";
    static final String detectorName = "HPS-PhysicsRun2019-v2-4pt5";
    static final String steeringFileName = "/org/hps/steering/recon/PhysicsRun2019MCRecon.lcsim";
    private final int nEvents = 1000;
    private String aidaOutputFile = "target/test-output/PhysRun2019MCReconTest/PhysRun2019MCReconTest";

    /**
     * Run number for conditions system.
     */
    private static final Integer RUN = 10000;

    public void testIt() throws Exception {
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        File slicInputFile = cache.getCachedFile(testURL);
        File outputFile = new TestOutputFile(PhysRun2019MCReconTest.class, "PhysRun2019MCReconTest");
        final JobManager job = new JobManager();
        job.addInputFile(slicInputFile);
        job.addVariableDefinition("detector", detectorName);
        job.addVariableDefinition("outputFile", outputFile.getPath() + ".slcio");
        job.addVariableDefinition("run", RUN.toString());
        job.setup(steeringFileName);
        long startTime = System.currentTimeMillis();
        job.run();
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        System.out.println("recon ran on " + job.getLCSimLoop().getTotalCountableConsumed() + " events");

//        String args[] = {"-r", "-x", steeringFileName, "-d",
//            detectorName, "-D", "outputFile=" + outputFile.getPath(), "-n", String.format("%d", nEvents),
//            slicInputFile.getPath(), "-e", "1"};
//        System.out.println("Running PhysRun2019MCReconTest.main ...");
//        System.out.println("writing to: " + outputFile.getPath());
////        long startTime = System.currentTimeMillis();
////        EvioToLcio.main(args);
////        long endTime = System.currentTimeMillis();
////        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        // Read in the LCIO event file and print out summary information.
//        System.out.println("Running ReconCheckDriver on output ...");
//        LCSimLoop loop = new LCSimLoop();
//        PhysRun2016V0Recon reconDriver = new PhysRun2016V0Recon();
//        aidaOutputFile = new TestUtil.TestOutputFile(getClass().getSimpleName()).getPath() + File.separator + this.getClass().getSimpleName();
//        reconDriver.setAidaFileName(aidaOutputFile);
//        loop.add(reconDriver);
//        try {
//            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
//            loop.loop(-1);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
//        System.out.println("writing aida file to: " + aidaOutputFile);
//        comparePlots();
        System.out.println("Done!");
    }

//    public void comparePlots() throws Exception {
//        AIDA aida = AIDA.defaultInstance();
//        final IAnalysisFactory af = aida.analysisFactory();
//
//        URL refFileURL = new URL("http://www.lcsim.org/test/hps-java/referencePlots/PhysRun2016V0ReconTest/PhysRun2016V0ReconTest-ref.aida");
//        FileCache cache = new FileCache();
//        File aidaRefFile = cache.getCachedFile(refFileURL);
//
//        File aidaTstFile = new File(aidaOutputFile + ".aida");
//
//        ITree ref = af.createTreeFactory().create(aidaRefFile.getAbsolutePath());
//        ITree tst = af.createTreeFactory().create(aidaTstFile.getAbsolutePath());
//
//        String[] histoNames = ref.listObjectNames(".", true);
//        String[] histoTypes = ref.listObjectTypes(".", true);
//        System.out.println("comparing " + histoNames.length + " managed objects");
//        double tolerance = 1E-4;
//        for (int i = 0; i < histoNames.length; ++i) {
//            String histoName = histoNames[i];
//            if (histoTypes[i].equals("IHistogram1D")) {
//                System.out.println("checking entries, means and rms for " + histoName);
//                IHistogram1D h1_r = (IHistogram1D) ref.find(histoName);
//                IHistogram1D h1_t = (IHistogram1D) tst.find(histoName);
//                assertEquals(h1_r.entries(), h1_t.entries());
//                assertEquals(h1_r.mean(), h1_t.mean(), tolerance * abs(h1_r.mean()));
//                if (histoName.equals("./TargetConstrainedV0Vertices/V0 Vertex z")) {
//                    System.out.println("Excpeption for rms of " + histoName + " = " + h1_r.rms() + "  ref = " + h1_t.rms());
//                    assertEquals(h1_r.rms(), 1E-6, 1E-6);
//                } else {
//                    assertEquals(h1_r.rms(), h1_t.rms(), tolerance * abs(h1_r.rms()));
//                }
//            }
//        }
//    }
}
