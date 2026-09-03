package org.hps.analysis.tuple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.recon.particle.HpsReconParticleDriver;
import org.hps.util.test.TestUtil;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

/**
 * End-to-end smoke test for the cascade-vertex reconstruction chain: runs a fresh
 * {@code HpsReconParticleDriver} (so V0 candidates are rebuilt with the current
 * full-covariance BilliorVertexer) followed by {@link CascadeVertexTupleDriver} over a
 * real reconstructed 2016 physics-run sample, and checks the resulting flat ntuple.
 */
public class CascadeVertexTupleDriverTest extends TestCase {

    private static final String LCIO_INPUT_FILE_NAME = "hps_005772.0_recon_Rv4657-0-10000.slcio";
    private static final String TUPLE_OUTPUT_FILE_NAME = "target/test-output/cascade_vertex_tuple.txt";
    private static final String CASCADE_COL_NAME = "CascadeVertexCandidates";

    public void testIt() throws Exception {
        File lcioInputFile = TestUtil.downloadTestFile(LCIO_INPUT_FILE_NAME);

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.addConditionsListener(new SvtDetectorSetup());

        loop.add(new org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver());
        loop.add(new org.hps.recon.filtering.EventFlagFilter());

        org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup rthss
                = new org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup();
        rthss.setReadoutCollections(new String[]{"SVTRawTrackerHits"});
        loop.add(rthss);

        HpsReconParticleDriver reconParticleDriver = new HpsReconParticleDriver();
        reconParticleDriver.setEcalClusterCollectionName("EcalClustersCorr");
        reconParticleDriver.setTrackCollectionNames(new String[]{"GBLTracks"});
        reconParticleDriver.setUnconstrainedV0CandidatesColName("UnconstrainedV0CandidatesCascadeTest");
        reconParticleDriver.setFinalStateParticlesColName("FinalStateParticlesCascadeTest");
        reconParticleDriver.setCascadeVertexCandidatesColName(CASCADE_COL_NAME);
        loop.add(reconParticleDriver);

        File tupleOutputFile = new File(TUPLE_OUTPUT_FILE_NAME);
        tupleOutputFile.getParentFile().mkdirs();
        CascadeVertexTupleDriver tupleDriver = new CascadeVertexTupleDriver();
        tupleDriver.setCascadeVertexCandidatesColName(CASCADE_COL_NAME);
        tupleDriver.setTupleFile(TUPLE_OUTPUT_FILE_NAME);
        loop.add(tupleDriver);

        loop.loop(5000);
        loop.dispose();

        checkTupleOutput(tupleOutputFile);
    }

    private void checkTupleOutput(File tupleOutputFile) throws Exception {
        assertTrue("cascade vertex tuple file should exist", tupleOutputFile.exists());

        BufferedReader reader = new BufferedReader(new FileReader(tupleOutputFile));
        String header = reader.readLine();
        assertNotNull("cascade vertex tuple file should have a header line", header);
        String[] headerVars = header.split(":");

        int nRows = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            nRows++;
            String[] fields = line.trim().split("\\s+");
            assertEquals("row should have one value per header variable", headerVars.length, fields.length);

            double chi2 = -1, ndf = -1;
            for (int i = 0; i < headerVars.length; i++) {
                double value = Double.parseDouble(fields[i]);
                assertTrue("field " + headerVars[i] + " should be finite, got " + value,
                        !Double.isNaN(value) && !Double.isInfinite(value));
                if (headerVars[i].equals("cascadeChi2/D")) {
                    chi2 = value;
                }
                if (headerVars[i].equals("cascadeNdf/I")) {
                    ndf = value;
                }
            }
            assertTrue("cascadeChi2 should be non-negative, got " + chi2, chi2 >= 0);
            assertEquals("cascadeNdf should be 1", 1.0, ndf);
        }
        reader.close();

        assertTrue("at least one cascade vertex candidate should have been written", nRows > 0);
    }
}
