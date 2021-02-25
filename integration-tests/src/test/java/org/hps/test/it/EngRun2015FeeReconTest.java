package org.hps.test.it;

import static java.lang.Math.abs;

import java.io.File;
import java.io.IOException;

import org.hps.evio.EvioToLcio;
import org.hps.util.test.TestUtil;
import org.hps.util.test.TestOutputFile;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.loop.LCSimLoop;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class EngRun2015FeeReconTest extends TestCase {

    static final String testFileName = "hps_005772_feeskim_10k.evio";
    private final int nEvents = 5000;
    static final String fieldmapName = "HPS-EngRun2015-Nominal-v6-0-fieldmap_v3";
    static final String steeringFileName = "/org/hps/steering/recon/legacy_drivers/EngineeringRun2015FullRecon.lcsim";
    private String aidaOutputFile = "target/test-output/EngRun2015FeeReconTest/EngRun2015FeeReconTest";

    public void testIt() throws Exception {
        File evioInputFile = TestUtil.downloadTestFile(testFileName);
        File outputFile = new TestOutputFile(EngRun2015FeeReconTest.class, "recon");
        String args[] = {"-r", "-x", steeringFileName, "-d",
            fieldmapName, "-D", "outputFile=" + outputFile.getPath(), "-n", String.format("%d", nEvents),
            evioInputFile.getPath()};
        EvioToLcio.main(args);
        LCSimLoop loop = new LCSimLoop();
        EngRun2015FeeRecon reconDriver = new EngRun2015FeeRecon();
        aidaOutputFile = new TestOutputFile(getClass().getSimpleName()).getPath() + File.separator + this.getClass().getSimpleName();
        reconDriver.setAidaFileName(aidaOutputFile);
        loop.add(reconDriver);
        try {
            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        comparePlots();
    }

    public void comparePlots() throws Exception {
        AIDA aida = AIDA.defaultInstance();
        final IAnalysisFactory af = aida.analysisFactory();

        File aidaRefFile = TestUtil.downloadRefPlots("EngRun2015FeeReconTest");

        File aidaTstFile = new File(aidaOutputFile+".aida");

        ITree ref = af.createTreeFactory().create(aidaRefFile.getAbsolutePath());
        ITree tst = af.createTreeFactory().create(aidaTstFile.getAbsolutePath());

        String[] histoNames = ref.listObjectNames();
        String[] histoTypes = ref.listObjectTypes();
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
                assertEquals(h1_r.mean(), h1_t.mean(), tolerance * abs(h1_r.mean()));
                assertEquals(h1_r.rms(), h1_t.rms(), tolerance * abs(h1_r.rms()));
            }
        }
    }
}
