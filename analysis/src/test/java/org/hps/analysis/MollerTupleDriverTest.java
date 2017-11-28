package org.hps.analysis;

public class MollerTupleDriverTest extends TupleDriverTest {
    @Override
    public void testIt() throws Exception {
         testURLBase = "http://www.lcsim.org/test/hps-java/";
         txtRefFileName = "out_moller_Ref.txt";
         lcioInputFileName = "tmp.slcio";
         txtOutputFileName = "target/test-output/out_moller.txt";

         testTupleDriver = new org.hps.analysis.tuple.MollerTupleDriver();
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setTupleFile(txtOutputFileName);
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setTriggerType("all");
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setIsGBL(true);
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setCutTuple(true);
         ((org.hps.analysis.tuple.MollerTupleDriver)testTupleDriver).setBeamPosZ(-5.0);
         
         super.testIt();
    }
}
