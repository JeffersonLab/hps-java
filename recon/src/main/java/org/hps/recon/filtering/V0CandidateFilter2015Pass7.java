package org.hps.recon.filtering;

import java.util.List;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;

/**
 * Class to strip off trident candidates for the 2015 Pass7 Reconstruction.
 * pairs1 trigger. SVT bias on. SVT at 0.5 mm. no DAQ errors. demand top/bottom
 * e+e- pair.
 *
 * @author Norman A Graf
 * @version $Id:
 */
public class V0CandidateFilter2015Pass7 extends EventReconFilter {

    private String _V0CandidateCollectionName = "UnconstrainedV0Candidates";

    @Override
    protected void process(EventHeader event) {
        incrementEventProcessed();

        //is there a V0 candidate in this event
        if (!event.hasCollection(ReconstructedParticle.class, _V0CandidateCollectionName)) {
            skipEvent();
        }
        List<ReconstructedParticle> V0Candidates = event.get(ReconstructedParticle.class, _V0CandidateCollectionName);
        if (V0Candidates.size() == 0) {
            skipEvent();
        }

        // only keep pair1 triggers:
        if (!event.hasCollection(GenericObject.class, "TriggerBank")) {
            skipEvent();
        }
        boolean isPairs1 = false;
        for (GenericObject gob : event.get(GenericObject.class, "TriggerBank")) {
            if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) {
                continue;
            }
            TIData tid = new TIData(gob);
            if (tid.isPair1Trigger()) {
                isPairs1 = true;
                break;
            }
        }
        if (!isPairs1) {
            skipEvent();
        }

        // SVT bias on
        int[] flag = event.getIntegerParameters().get("svt_bias_good");
        if (flag == null || flag[0] == 0) {
            skipEvent();
        }

        // SVT is at correct position
        flag = event.getIntegerParameters().get("svt_position_good");
        if (flag == null || flag[0] == 0) {
            skipEvent();
        }

        // SVT latency is correct
        flag = event.getIntegerParameters().get("svt_latency_good");
        if (flag == null || flag[0] == 0) {
            skipEvent();
        }

        int nV0 = 0;
        for (ReconstructedParticle v0 : V0Candidates) {
            ReconstructedParticle electron = v0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = v0.getParticles().get(ReconParticleDriver.POSITRON);
            //double-check on GBL (should never have non-GBL tracks in pass7, but...
            if (!TrackType.isGBL(v0.getType())) { // we only care about GBL vertices
                continue;
            }
            // require e+ and e- to be in opposite hemispheres
            boolean isElectronTop = electron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0;
            boolean isPositronTop = positron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0;

            if (isElectronTop != isPositronTop) {
                nV0++;
            }
        }
        if (nV0 == 0) {
            skipEvent();
        }
        incrementEventPassed();
    }

    /**
     * Name of V0 Candidate ReconstructedParticle Collection Name
     *
     * @param s
     */
    public void setV0CandidateCollectionName(String s) {
        _V0CandidateCollectionName = s;
    }

}
