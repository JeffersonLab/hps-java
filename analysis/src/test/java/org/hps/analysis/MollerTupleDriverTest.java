package org.hps.analysis;

public class MollerTupleDriverTest extends TupleDriverTest {
    @Override
    public void testIt() throws Exception {
         testURLBase = "http://www.lcsim.org/test/hps-java/";
         txtRefFileName = "ntuple_007796_moller_Ref.txt";
         lcioInputFileName = "PhysRun2016MollerReconTest.slcio";
         txtOutputFileName = "target/test-output/out_moller.txt";

         testTupleDriver = new org.hps.analysis.tuple.MollerTupleDriver();
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setTupleFile(txtOutputFileName);
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setTriggerType("all");
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setIsGBL(true);
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setCutTuple(true);
         
         super.testIt();
    }
}
