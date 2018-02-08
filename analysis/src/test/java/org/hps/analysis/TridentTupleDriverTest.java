package org.hps.analysis;

public class TridentTupleDriverTest extends TupleDriverTest {
    @Override
    public void testIt() throws Exception {
         testURLBase = "http://www.lcsim.org/test/hps-java/";
         txtRefFileName = "ntuple_005772_tri_Ref.txt";
         lcioInputFileName = "hps_005772.0_recon_Rv4657-0-10000.slcio";
         txtOutputFileName = "target/test-output/out_tri.txt";

         testTupleDriver = new org.hps.analysis.tuple.TridentTupleDriver();
         ((org.hps.analysis.tuple.TridentTupleDriver)testTupleDriver).setTupleFile(txtOutputFileName);
         ((org.hps.analysis.tuple.TridentTupleDriver)testTupleDriver).setTriggerType("all");
         ((org.hps.analysis.tuple.TridentTupleDriver)testTupleDriver).setIsGBL(true);
         ((org.hps.analysis.tuple.TridentTupleDriver)testTupleDriver).setCutTuple(true);
         
         super.testIt();
    }
}
