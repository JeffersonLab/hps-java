package org.hps.analysis.tuple;

import java.util.Arrays;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

public class TridentMCTupleDriver extends MCTupleMaker {
    private final double tupleTrkPCut = 0.9;
    private final double tupleMaxSumCut = 1.3;
    
    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        addVertexVariables();
        addParticleVariables("ele");
        addParticleVariables("pos");
        addMCTridentVariables();

        String[] newVars = new String[]{"minPositiveIso/D", "minNegativeIso/D", "minIso/D"};
        tupleVariables.addAll(Arrays.asList(newVars));

    }

    @Override
    public void process(EventHeader event) {

        setupCollections(event);
        
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            tupleMap.clear();
            boolean isOK = fillBasicTuple(event, triggerData, uncV0, false);
            fillMCTridentVariables(event);
            if (tupleWriter != null && isOK) {
                if (!cutTuple || (passesCuts())) {
                    writeTuple();
                }
            }
        }

    }

    @Override
    boolean passesCuts() {
        boolean trkCut = tupleMap.get("eleP/D") < tupleTrkPCut * ebeam && tupleMap.get("posP/D") < tupleTrkPCut * ebeam;
        boolean sumCut = tupleMap.get("eleP/D") + tupleMap.get("posP/D") < tupleMaxSumCut * ebeam;
        return (trkCut && sumCut);
    }
}
