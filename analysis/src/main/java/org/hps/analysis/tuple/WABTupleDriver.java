package org.hps.analysis.tuple;

import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;

public class WABTupleDriver extends TupleDriver {

    private final String finalStateParticlesColName = "FinalStateParticles";
    private final double tupleMinECut = 0.5;
    private final double tupleMaxECut = 1.3;

    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        addParticleVariables("ele");
        addParticleVariables("pho");
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
        List<ReconstructedParticle> eleList = new ArrayList<ReconstructedParticle>();
        List<ReconstructedParticle> phoList = new ArrayList<ReconstructedParticle>();

        for (ReconstructedParticle fsp : fspList) {
            if (fsp.getCharge() == 0) {
                phoList.add(fsp);
            } else {
                if (isGBL == TrackType.isGBL(fsp.getType()) && fsp.getCharge() < 0) {
                    eleList.add(fsp);
                }
            }
        }
        for (ReconstructedParticle ele : eleList) {
            for (ReconstructedParticle pho : phoList) {
                tupleMap.clear();
                fillEventVariables(event, triggerData);

                fillParticleVariables(event, ele, "ele");
                fillParticleVariables(event, pho, "pho");

                if (tupleWriter != null) {
                    boolean eCut = tupleMap.get("eleP/D") + tupleMap.get("phoClE/D") > tupleMinECut * ebeam && tupleMap.get("eleP/D") + tupleMap.get("phoClE/D") < tupleMaxECut * ebeam;
                    if (!cutTuple || (eCut)) {
                        writeTuple();
                    }
                }
            }
        }
    }
}
