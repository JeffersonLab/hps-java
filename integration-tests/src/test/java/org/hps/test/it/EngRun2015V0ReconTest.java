package org.hps.test.it;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.abs;
import java.net.URL;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import org.hps.evio.EvioToLcio;
import org.hps.test.util.TestOutputFile;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author Norman A. Graf
 */
public class EngRun2015V0ReconTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/calibration";
    static final String testFileName = "hps_005772_v0skim_10k.evio";
    private final int nEvents = -1;
    private String aidaOutputFile = "target/test-output/EngRun2015V0ReconTest/EngRun2015V0ReconTest.aida";

    public void testIt() throws Exception {
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        File evioInputFile = cache.getCachedFile(testURL);
        File outputFile = new TestOutputFile(EngRun2015V0ReconTest.class, "EngRun2015V0ReconTest");
        String args[] = {"-r", "-x", "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim", "-d",
            "HPS-EngRun2015-Nominal-v6-0-fieldmap", "-D", "outputFile=" + outputFile.getPath(), "-n", "2000",
            evioInputFile.getPath(), "-e", "100"};
        System.out.println("Running EngRun2015V0ReconTest.main ...");
        System.out.println("writing to: " + outputFile.getPath());
        long startTime = System.currentTimeMillis();
        EvioToLcio.main(args);
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        // Read in the LCIO event file and print out summary information.
        System.out.println("Running ReconCheckDriver on output ...");
        LCSimLoop loop = new LCSimLoop();
        EngRun2015V0Recon reconDriver = new EngRun2015V0Recon();
        aidaOutputFile = new TestUtil.TestOutputFile(getClass().getSimpleName()).getPath() + File.separator + this.getClass().getSimpleName() + ".aida";
        reconDriver.setAidaFileName(aidaOutputFile);
        loop.add(reconDriver);
        try {
            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("writing aida file to: " + aidaOutputFile);
        testPlots();
        System.out.println("Done!");
    }
    
       public void testPlots() throws Exception {
        AIDA aida = AIDA.defaultInstance();
        final IAnalysisFactory af = aida.analysisFactory();

        URL refFileURL = new URL("http://www.lcsim.org/test/hps-java/referencePlots/EngRun2015V0ReconTest/EngRun2015V0ReconTest-ref.aida");
        FileCache cache = new FileCache();
        File aidaRefFile = cache.getCachedFile(refFileURL);

        File aidaTstFile = new File(aidaOutputFile);

        ITree ref = af.createTreeFactory().create(aidaRefFile.getAbsolutePath());
        ITree tst = af.createTreeFactory().create(aidaTstFile.getAbsolutePath());

        String[] histoNames = ref.listObjectNames(".", true);
        String[] histoTypes = ref.listObjectTypes(".", true);
        System.out.println("comparing " + histoNames.length + " managed objects");
        double tolerance = 1E-4;
        for (int i = 0; i < histoNames.length; ++i) {
            String histoName = histoNames[i];
            if (histoTypes[i].equals("IHistogram1D")) {
                System.out.println("checking entries, means and rms for " + histoName);
                IHistogram1D h1_r = (IHistogram1D) ref.find(histoName);
                IHistogram1D h1_t = (IHistogram1D) tst.find(histoName);
                assertEquals(h1_r.entries(), h1_t.entries());
                assertEquals(h1_r.mean(), h1_t.mean(), tolerance * abs(h1_r.mean()));
                assertEquals(h1_r.rms(), h1_t.rms(), tolerance * abs(h1_r.rms()));
            }
        }
    }
}
