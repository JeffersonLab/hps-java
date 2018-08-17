package org.hps.analysis.tuple;


import org.lcsim.event.EventHeader;

public class WABTridentFullTupleDriver extends FullTruthTupleMaker {


    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        
        addFullMCTridentVariables();
        addFullMCWabVariables();
    }
    
    @Override
    public void process(EventHeader event) {

        fillTruthEventVariables(event);
        fillMCTridentVariables(event);
        fillMCFullTruthVariables(event);
        fillMCWabVariables(event);

        if (tupleWriter != null) {
            writeTuple();
        }
    }


    @Override
    boolean passesCuts() {
        return true;
    }
}
