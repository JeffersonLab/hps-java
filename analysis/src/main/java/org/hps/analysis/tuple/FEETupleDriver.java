package org.hps.analysis.tuple;

import java.util.List;
import org.hps.recon.tracking.TrackType;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

public class FEETupleDriver extends TupleMaker {

    private final String finalStateParticlesColName = "OtherElectrons";
    private final double tupleTrkPCut = 0.7;

    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        addParticleVariables("fsp");
    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            return;
        }
        triggerData = checkTrigger(event);
        if (triggerData == null)
            return;

        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, finalStateParticlesColName);

        for (ReconstructedParticle fsp : fspList) {
            if (isGBL != TrackType.isGBL(fsp.getType())) {
                continue;
            }
            tupleMap.clear();
            fillEventVariables(event, triggerData);

            fillParticleVariables(event, fsp, "fsp", false, false, false);

            if (tupleWriter != null) {
                if (!cutTuple || (passesCuts())) {
                    writeTuple();
                }
            }
        }
    }

    @Override
    boolean passesCuts() {
        return tupleMap.get("fspP/D") > tupleTrkPCut * ebeam;
    }
}
