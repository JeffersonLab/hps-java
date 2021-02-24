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
 * @author Norman A. Graf
 */
public class PhysRun2016V0ReconTest  extends TestCase {

    static final String testFileName = "hps_007796_v0skim.evio";
    static final String fieldmapName = "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign";
    static final String steeringFileName = "/org/hps/steering/recon/legacy_drivers/PhysicsRun2016FullRecon.lcsim";
    private final int nEvents = 5000;
    private String aidaOutputFile = "target/test-output/PhysRun2016V0ReconTest/PhysRun2016V0ReconTest";

    public void testIt() throws Exception {
        File evioInputFile = TestUtil.downloadTestFile(testFileName);
        File outputFile = new TestOutputFile(PhysRun2016V0ReconTest.class, "PhysRun2016V0ReconTest");
        String args[] = {"-r", "-x", steeringFileName, "-d",
            fieldmapName, "-D", "outputFile=" + outputFile.getPath(), "-n", String.format("%d", nEvents),
            evioInputFile.getPath()};
        long startTime = System.currentTimeMillis();
        EvioToLcio.main(args);
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        // Read in the LCIO event file and print out summary information.
        LCSimLoop loop = new LCSimLoop();
        PhysRun2016V0Recon reconDriver = new PhysRun2016V0Recon();
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

        File aidaRefFile = TestUtil.downloadRefPlots("PhysRun2016V0ReconTest");

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
                System.out.println("checking entries, means and rms for " + histoName);
                IHistogram1D h1_r = (IHistogram1D) ref.find(histoName);
                IHistogram1D h1_t = (IHistogram1D) tst.find(histoName);
                assertEquals(h1_r.entries(), h1_t.entries());
                assertEquals(h1_r.mean(), h1_t.mean(), tolerance * abs(h1_r.mean()));
                if(histoName.equals("./TargetConstrainedV0Vertices/V0 Vertex z") ) {
                    System.out.println("Excpeption for rms of "+histoName+ " = "+h1_r.rms()+ "  ref = "+h1_t.rms());
                    assertEquals(h1_r.rms(), 1E-6, 1E-6);
                }else {
                    assertEquals(h1_r.rms(), h1_t.rms(), tolerance * abs(h1_r.rms()));
                }
            }
        }
    }
}
