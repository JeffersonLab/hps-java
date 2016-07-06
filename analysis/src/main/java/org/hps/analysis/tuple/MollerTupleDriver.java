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

public class MollerTupleDriver extends TupleDriver {

    private final String unconstrainedV0CandidatesColName = "UnconstrainedMollerCandidates";

//  track quality cuts
    private final double tupleTrkPCut = 0.9;
    private final double tupleMinSumCut = 0.7;
    private final double tupleMaxSumCut = 1.3;

    public MollerTupleDriver() {
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

            ReconstructedParticle top = uncV0.getParticles().get(ReconParticleDriver.MOLLER_TOP);
            ReconstructedParticle bot = uncV0.getParticles().get(ReconParticleDriver.MOLLER_BOT);
            if (top.getCharge() != -1 || bot.getCharge() != -1) {
                throw new RuntimeException("incorrect charge on v0 daughters");
            }

            Track topTrack = top.getTracks().get(0);
            Track botTrack = bot.getTracks().get(0);

            TrackState topTSTweaked = fillParticleVariables(event, top, "top");
            TrackState botTSTweaked = fillParticleVariables(event, bot, "bot");

            List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
            billiorTracks.add(new BilliorTrack(topTSTweaked, topTrack.getChi2(), topTrack.getNDF()));
            billiorTracks.add(new BilliorTrack(botTSTweaked, botTrack.getChi2(), botTrack.getNDF()));

            double minPositiveIso = Math.min(tupleMap.get("topMinPositiveIso/D"), tupleMap.get("botMinPositiveIso/D"));
            double minNegativeIso = Math.min(Math.abs(tupleMap.get("topMinNegativeIso/D")), Math.abs(tupleMap.get("botMinNegativeIso/D")));
            double minIso = Math.min(minPositiveIso, minNegativeIso);

            fillVertexVariables(event, billiorTracks, top, bot);

            tupleMap.put("minPositiveIso/D", minPositiveIso);
            tupleMap.put("minNegativeIso/D", minNegativeIso);
            tupleMap.put("minIso/D", minIso);

            if (tupleWriter != null) {
                boolean trkCut = tupleMap.get("topP/D") < tupleTrkPCut * ebeam && tupleMap.get("botP/D") < tupleTrkPCut * ebeam;
                boolean sumCut = tupleMap.get("topP/D") + tupleMap.get("botP/D") > tupleMinSumCut * ebeam && tupleMap.get("topP/D") + tupleMap.get("botP/D") < tupleMaxSumCut * ebeam;
                if (!cutTuple || (trkCut && sumCut)) {
                    writeTuple();
                }
            }
        }
    }
}
