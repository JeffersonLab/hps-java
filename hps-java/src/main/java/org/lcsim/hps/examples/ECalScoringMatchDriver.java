package org.lcsim.hps.examples;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class ECalScoringMatchDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
//    IHistogram1D hitCorEner = aida.histogram1D("CorEnergy", 1000, 0.0, 10);
    IHistogram2D fieldE_edep_2D = aida.histogram2D("total ECal energy deposition vs. MCParticle energy before flange", 200, 0.0, 2.2, 200, 0.0, 2.2);
    IHistogram2D scoringE_edep_2D = aida.histogram2D("total ECal energy deposition vs. MCParticle energy after flange", 200, 0.0, 2.2, 200, 0.0, 2.2);

    @Override
    protected void process(EventHeader event) {
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "EcalHits");
        if (hits == null) {
            throw new RuntimeException("Missing ECal hit collection!");
        }
        List<SimTrackerHit> scoringHits = event.get(SimTrackerHit.class, "TrackerHitsECal");
        if (scoringHits == null) {
            throw new RuntimeException("Missing ECal scoring plane hit collection!");
        }
        List<SimTrackerHit> fieldHits = event.get(SimTrackerHit.class, "TrackerHitsFieldDef");
        if (fieldHits == null) {
            throw new RuntimeException("Missing field boundary scoring plane hit collection!");
        }

        Map<MCParticle, SimTrackerHit> scoringHitMap = new HashMap<MCParticle, SimTrackerHit>();
        Map<MCParticle, SimTrackerHit> fieldHitMap = new HashMap<MCParticle, SimTrackerHit>();
        Map<MCParticle, Double> edepMap = new HashMap<MCParticle, Double>(); //sum of all ECal hit energy contributions from each MCParticle

        for (SimTrackerHit scoringHit : scoringHits) {
            SimTrackerHit keyHit = scoringHitMap.get(scoringHit.getMCParticle());

            if (keyHit == null) {
                scoringHitMap.put(scoringHit.getMCParticle(), scoringHit);
            } else if (scoringHit.getTime() < keyHit.getTime()) { //keep only the earliest hit from each particle
                System.out.println("Multiple scoring hits from same particle");
                scoringHitMap.put(scoringHit.getMCParticle(), scoringHit);
            }
        }
        for (SimTrackerHit fieldHit : fieldHits) {
            if (fieldHit.getIdentifierFieldValue("layer") != 2) { //reject hits at the -Z end of the magnet
                continue;
            }
            SimTrackerHit keyHit = fieldHitMap.get(fieldHit.getMCParticle());

            if (keyHit == null) {
                fieldHitMap.put(fieldHit.getMCParticle(), fieldHit);
            } else if (fieldHit.getTime() < keyHit.getTime()) { //keep only the earliest hit from each particle
                System.out.println("Multiple scoring hits from same particle");
                fieldHitMap.put(fieldHit.getMCParticle(), fieldHit);
            }
        }
        for (SimCalorimeterHit hit : hits) {
            for (int i = 0; i < hit.getMCParticleCount(); i++) {
                MCParticle mcParticle = hit.getMCParticle(i);
                Double edep = edepMap.get(mcParticle);
                if (edep == null) {
                    edep = hit.getContributedEnergy(i);
                } else {
                    edep += hit.getContributedEnergy(i);
                }
                edepMap.put(mcParticle, edep);
            }
        }

        for (MCParticle mcParticle : edepMap.keySet()) {
            if (scoringHitMap.containsKey(mcParticle)) {
                scoringE_edep_2D.fill(norm(scoringHitMap.get(mcParticle).getMomentum()), edepMap.get(mcParticle));
            }
            if (fieldHitMap.containsKey(mcParticle)) {
                fieldE_edep_2D.fill(norm(fieldHitMap.get(mcParticle).getMomentum()), edepMap.get(mcParticle));
            }
        }
    }

    private static double norm(double[] vector) {
        double normsq = 0.0;
        for (int i = 0; i < vector.length; i++) {
            normsq += vector[i] * vector[i];
        }
        return Math.sqrt(normsq);
    }
}
