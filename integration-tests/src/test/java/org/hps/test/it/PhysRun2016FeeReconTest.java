package org.hps.test.it;

public class PhysRun2016FeeReconTest extends ReconTest {

    public PhysRun2016FeeReconTest() {
        super(PhysRun2016FeeReconTest.class,
                "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign",
                "hps_007796_feeskim.evio",
                "/org/hps/steering/recon/legacy_drivers/PhysicsRun2016FullRecon.lcsim",
                5000,
                new PhysRun2016FeeRecon(),
                DEFAULT_TOLERANCE);
    }

    /*
    static final String testFileName = "hps_007796_feeskim.evio";
    static final String fieldmapName = "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign";
    static final String steeringFileName = "/org/hps/steering/recon/legacy_drivers/PhysicsRun2016FullRecon.lcsim";
    private final int nEvents = 5000;
    private String aidaOutputFile = "target/test-output/PhysRun2016FeeReconTest/PhysRun2016FeeReconTest";

    public void testIt() throws Exception {
        File evioInputFile = TestUtil.downloadTestFile(testFileName);
        File outputFile = new TestOutputFile(PhysRun2016FeeReconTest.class, "recon");
        String args[] = {"-r", "-x", steeringFileName, "-d",
            fieldmapName, "-D", "outputFile=" + outputFile.getPath(), "-n", String.format("%d", nEvents),
            evioInputFile.getPath()};
        EvioToLcio.main(args);
        LCSimLoop loop = new LCSimLoop();
        PhysRun2016FeeRecon reconDriver = new PhysRun2016FeeRecon();
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

        File aidaRefFile = TestUtil.downloadRefPlots("PhysRun2016FeeReconTest");

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
                System.out.println("checking entries, means and rms for " + histoName);
                IHistogram1D h1_r = (IHistogram1D) ref.find(histoName);
                IHistogram1D h1_t = (IHistogram1D) tst.find(histoName);
                assertEquals(h1_r.entries(), h1_t.entries());
                assertEquals(h1_r.mean(), h1_t.mean(), tolerance * abs(h1_r.mean()));
                assertEquals(h1_r.rms(), h1_t.rms(), tolerance * abs(h1_r.rms()));
            }
        }
    }
    */
}
