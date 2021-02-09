package org.hps.test.it;

import static java.lang.Math.abs;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.hps.evio.EvioToLcio;
import org.hps.util.test.TestOutputFile;
import org.hps.util.test.TestUtil;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import junit.framework.TestCase;

/**
 *
 * @author Norman A. Graf
 */
public class EngRun2015V0ReconTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/calibration";
    static final String testFileName = "hps_005772_v0skim_10k.evio";
    static final String fieldmapName = "HPS-EngRun2015-Nominal-v6-0-fieldmap_v3";
    static final String steeringFileName = "/org/hps/steering/recon/legacy_drivers/EngineeringRun2015FullRecon.lcsim";
    private final int nEvents = 2000;
    private String aidaOutputFile = "target/test-output/EngRun2015V0ReconTest/EngRun2015V0ReconTest";

    public void testIt() throws Exception {
        File evioInputFile = TestUtil.downloadTestFile(testFileName);
        File outputFile = new TestOutputFile(EngRun2015V0ReconTest.class, "EngRun2015V0ReconTest");
        String args[] = {"-r", "-x", steeringFileName, "-d",
            fieldmapName, "-D", "outputFile=" + outputFile.getPath(), "-n", String.format("%d", nEvents),
            evioInputFile.getPath(), "-e", "100"};
        System.out.println("Writing to: " + outputFile.getPath());
        long startTime = System.currentTimeMillis();
        EvioToLcio.main(args);
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        // Read in the LCIO event file and print out summary information.
        LCSimLoop loop = new LCSimLoop();
        EngRun2015V0Recon reconDriver = new EngRun2015V0Recon();
        aidaOutputFile = new TestOutputFile(getClass().getSimpleName()).getPath() + File.separator + this.getClass().getSimpleName();
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
        comparePlots();
        System.out.println("Done!");
    }

       public void comparePlots() throws Exception {
        AIDA aida = AIDA.defaultInstance();
        final IAnalysisFactory af = aida.analysisFactory();

        URL refFileURL = new URL("http://www.lcsim.org/test/hps-java/referencePlots/EngRun2015V0ReconTest/EngRun2015V0ReconTest-ref.aida");
        FileCache cache = new FileCache();
        File aidaRefFile = cache.getCachedFile(refFileURL);

        File aidaTstFile = new File(aidaOutputFile+".aida");

        ITree ref = af.createTreeFactory().create(aidaRefFile.getAbsolutePath());
        ITree tst = af.createTreeFactory().create(aidaTstFile.getAbsolutePath());

        String[] histoNames = ref.listObjectNames(".", true);
        String[] histoTypes = ref.listObjectTypes(".", true);
        System.out.println("comparing " + histoNames.length + " managed objects");
        double tolerance = 5E-3;
        for (int i = 0; i < histoNames.length; ++i) {
            String histoName = histoNames[i];
            if (histoTypes[i].equals("IHistogram1D")) {
                System.out.println("Checking entries, means and rms for " + histoName);
                IHistogram1D h1_r = (IHistogram1D) ref.find(histoName);
                IHistogram1D h1_t = (IHistogram1D) tst.find(histoName);
                //System.out.println("           Found              Expected");
                System.out.println("Entries: "+h1_r.entries()+" "+ h1_t.entries());
                System.out.println("Mean: "+h1_r.mean()+" "+ h1_t.mean());
                System.out.println("RMS "+h1_r.rms()+" "+ h1_t.rms());
            }
        }
        for (int i = 0; i < histoNames.length; ++i) {
            String histoName = histoNames[i];
            if (histoTypes[i].equals("IHistogram1D")) {
                System.out.println("checking entries, means and rms for " + histoName);
                IHistogram1D h1_r = (IHistogram1D) ref.find(histoName);
                IHistogram1D h1_t = (IHistogram1D) tst.find(histoName);
                assertEquals(h1_r.entries(), h1_t.entries());
                if(histoName.equals("./UnconstrainedV0Vertices/V0 Vertex y") ) {
                    System.out.println("Exception for mean of "+histoName+ " = "+h1_r.mean()+ "  ref = "+h1_t.mean());
                    assertEquals(h1_r.mean(), h1_t.mean(), 7E-2 * abs(h1_r.mean()));
                }else {
                    assertEquals(h1_r.mean(), h1_t.mean(), tolerance * abs(h1_r.mean()));
                }
                assertEquals(h1_r.rms(), h1_t.rms(), tolerance * abs(h1_r.rms()));
            }
        }
    }
}
