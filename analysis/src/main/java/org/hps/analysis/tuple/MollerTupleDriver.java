package org.hps.analysis.tuple;

import java.util.Arrays;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

public class MollerTupleDriver extends TupleMaker {

    private String unconstrainedV0CandidatesColName = "UnconstrainedMollerCandidates";
    private String beamspotConstrainedV0CandidatesColName = "BeamspotConstrainedMollerCandidates";
    private String targetConstrainedV0CandidatesColName = "TargetConstrainedMollerCandidates";

//  track quality cuts
    private final double tupleTrkPCut = 0.9;
    private final double tupleMinSumCut = 0.7;
    private final double tupleMaxSumCut = 1.3;

    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        addVertexVariables();
        addParticleVariables("top");
        addParticleVariables("bot");
        String[] newVars = new String[]{"minPositiveIso/D", "minNegativeIso/D", "minIso/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    @Override
    public void process(EventHeader event) {
        this.setCandidatesColName("MollerCandidates");
        if (!setupCollections(event))
            return;
        
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            tupleMap.clear();
            boolean isOK = fillBasicTuple(event, triggerData, uncV0, true);
            if (tupleWriter != null && isOK) {
                if (!cutTuple || (passesCuts())) {
                    writeTuple();
                }
            }
        }
    }

    @Override
    boolean passesCuts() {
        boolean trkCut = tupleMap.get("topP/D") < tupleTrkPCut * ebeam && tupleMap.get("botP/D") < tupleTrkPCut * ebeam;
        boolean sumCut = tupleMap.get("topP/D") + tupleMap.get("botP/D") > tupleMinSumCut * ebeam && tupleMap.get("topP/D") + tupleMap.get("botP/D") < tupleMaxSumCut * ebeam;
        return (trkCut && sumCut);
    }
}
