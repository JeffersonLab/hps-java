package org.hps.analysis.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

public class TridentTupleDriver extends TupleDriver {

    private final String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";

    private final double tupleTrkPCut = 0.9;
    private final double tupleMaxSumCut = 1.3;

    public TridentTupleDriver() {
        addEventVariables();
        addVertexVariables();
        addParticleVariables("ele");
        addParticleVariables("pos");
        String[] newVars = new String[]{"minPositiveIso/D", "minNegativeIso/D", "minIso/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName)) {
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
        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);

        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            if (isGBL != TrackType.isGBL(uncV0.getType())) {
                continue;
            }
            tupleMap.clear();
            fillEventVariables(event, triggerData);

            ReconstructedParticle electron = uncV0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = uncV0.getParticles().get(ReconParticleDriver.POSITRON);
            if (electron.getCharge() != -1 || positron.getCharge() != 1) {
                throw new RuntimeException("incorrect charge on v0 daughters");
            }

            Track eleTrack = electron.getTracks().get(0);
            Track posTrack = positron.getTracks().get(0);

            TrackState eleTSTweaked = fillParticleVariables(event, electron, "ele");
            TrackState posTSTweaked = fillParticleVariables(event, positron, "pos");

            List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
            billiorTracks.add(new BilliorTrack(eleTSTweaked, eleTrack.getChi2(), eleTrack.getNDF()));
            billiorTracks.add(new BilliorTrack(posTSTweaked, posTrack.getChi2(), posTrack.getNDF()));

            double minPositiveIso = Math.min(tupleMap.get("eleMinPositiveIso/D"), tupleMap.get("posMinPositiveIso/D"));
            double minNegativeIso = Math.max(Math.min(tupleMap.get("eleMinPositiveIso/D"), 9999), Math.min(tupleMap.get("posMinPositiveIso/D"), 9999));
            double minIso = Math.min(Math.abs(minPositiveIso), Math.abs(minNegativeIso));

            fillVertexVariables(event, billiorTracks, electron, positron);

            tupleMap.put("minPositiveIso/D", minPositiveIso);
            tupleMap.put("minNegativeIso/D", minNegativeIso);
            tupleMap.put("minIso/D", minIso);
            if (tupleWriter != null) {
                boolean trkCut = tupleMap.get("eleP/D") < tupleTrkPCut * ebeam && tupleMap.get("posP/D") < tupleTrkPCut * ebeam;
                boolean sumCut = tupleMap.get("eleP/D") + tupleMap.get("posP/D") < tupleMaxSumCut * ebeam;
                if (!cutTuple || (trkCut && sumCut)) {
                    writeTuple();
                }
            }
        }
    }
}
