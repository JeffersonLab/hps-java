package org.hps.analysis;


public class FEETupleDriverTest extends TupleDriverTest {

    @Override
    public void testIt() throws Exception {
         testURLBase = "http://www.lcsim.org/test/hps-java/";
         txtRefFileName = "ntuple_007796_fee_Ref.txt";
         lcioInputFileName = "PhysRun2016FeeReconTest.slcio";
         txtOutputFileName = "target/test-output/out_fee.txt";

         testTupleDriver = new org.hps.analysis.tuple.FEETupleDriver();
         ((org.hps.analysis.tuple.FEETupleDriver)testTupleDriver).setTupleFile(txtOutputFileName);
         ((org.hps.analysis.tuple.FEETupleDriver)testTupleDriver).setTriggerType("all");
         ((org.hps.analysis.tuple.FEETupleDriver)testTupleDriver).setIsGBL(true);
         ((org.hps.analysis.tuple.FEETupleDriver)testTupleDriver).setCutTuple(true);
         
         super.testIt();
    }
}
