package org.hps.analysis.tuple;

import org.lcsim.event.EventHeader;

public class TridentTruthTupleDriver extends MCTupleMaker {

    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addMCTridentVariables();
    }

    @Override
    public void process(EventHeader event) {

        fillMCTridentVariables(event);

        if (tupleWriter != null) {
            writeTuple();
        }
    }

    @Override
    boolean passesCuts() {
        return true;
    }
    

}
