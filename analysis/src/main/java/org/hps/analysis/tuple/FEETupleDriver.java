package org.hps.analysis.tuple;

import java.util.List;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;

public class FEETupleDriver extends TupleDriver {

    private final String finalStateParticlesColName = "FinalStateParticles";
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
        TIData triggerData = null;
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            for (GenericObject data : event.get(GenericObject.class, "TriggerBank")) {
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    triggerData = new TIData(data);
                }
            }
        }

        //check to see if this event is from the correct trigger (or "all");
        if (triggerData != null && !matchTriggerType(triggerData)) {
            return;
        }

        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, finalStateParticlesColName);

        for (ReconstructedParticle fsp : fspList) {
            if (isGBL != TrackType.isGBL(fsp.getType())) {
                continue;
            }
            tupleMap.clear();
            fillEventVariables(event, triggerData);

            fillParticleVariables(event, fsp, "fsp");

            if (tupleWriter != null) {
                boolean trkCut = tupleMap.get("fspP/D") > tupleTrkPCut * ebeam;
                if (!cutTuple || (trkCut)) {
                    writeTuple();
                }
            }
        }
    }
}
