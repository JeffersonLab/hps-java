package org.hps.analysis.tuple;

import java.util.Arrays;

import org.lcsim.event.EventHeader;

public class FullTruthTupleDriver extends TupleDriver {

    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        addFullMCTridentVariables();
    }
    
    protected void addEventVariables() {
        String[] newVars = new String[] {"run/I", "event/I", "tupleevent/I"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    @Override
    public void process(EventHeader event) {

        fillTruthEventVariables(event);
        fillMCFullTruthVariables(event);

        if (tupleWriter != null) {
            writeTuple();
        }
    }
}
