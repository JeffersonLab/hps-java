package org.hps.users.meeg;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class EcalTruthMatchingDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private IHistogram2D energy2D = aida.histogram2D("recon vs. truth hit E", 200, 0.0, 2.2, 200, 0.0, 2.2);
    private IHistogram2D energyRatio2D = aida.histogram2D("recon energy scale vs. truth E", 200, 0.0, 2.2, 200, 0.0, 2.0);
    private IHistogram1D energy1D = aida.histogram1D("hit recon energy scale", 200, 0, 2.0);
    private IHistogram1D hitTime1D = aida.histogram1D("recon hit time with truth hit match", 100, 0, 400);
    private IHistogram2D hitTime2D = aida.histogram2D("recon hit time vs. truth E", 400, 0.0, 2.2, 100, 0, 400);

    private double triggerTime = 150.0;
    private boolean debug = false;

    public void process(EventHeader event) {

        List<CalorimeterHit> truthHits = event.get(CalorimeterHit.class, "EcalHits");
        List<MCParticle> truthParticles = event.get(MCParticle.class, "MCParticle");
        List<CalorimeterHit> reconHits = event.get(CalorimeterHit.class, "EcalCalHits");
        List<Cluster> reconClusters = event.get(Cluster.class, "EcalClusters");

        for (MCParticle particle : truthParticles) {
            if (particle.getSimulatorStatus().isDecayedInCalorimeter() && particle.getEnergy() > 0.1) {
                if (debug) {
                    System.out.println(particle.getEnergy());
                }
            }
        }

        Map<Long, CalorimeterHit> crystalToReconHit = new HashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : reconHits) {
            CalorimeterHit hitInMap = crystalToReconHit.get(hit.getCellID());
            if (hitInMap == null || Math.abs(hitInMap.getTime() - triggerTime) > Math.abs(hit.getTime() - triggerTime)) {
                crystalToReconHit.put(hit.getCellID(), hit);
            }
        }

        Collections.sort(truthHits, new EnergyComparator());

        for (CalorimeterHit hit : truthHits) {
            CalorimeterHit reconHit = crystalToReconHit.get(hit.getCellID());

            if (debug) {
                System.out.format("truth hit: energy %f, ix %d, iy %d, time %f;\t", hit.getRawEnergy(), hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getTime());
            }
            if (reconHit == null) {
                if (debug) {
                    System.out.format("recon hit: not found\n");
                }
            } else {
                energy2D.fill(hit.getRawEnergy(), reconHit.getCorrectedEnergy());
                energy1D.fill(reconHit.getCorrectedEnergy() / hit.getRawEnergy());
                energyRatio2D.fill(hit.getRawEnergy(), reconHit.getCorrectedEnergy() / hit.getRawEnergy());
                hitTime1D.fill(reconHit.getTime());
                hitTime2D.fill(hit.getRawEnergy(), reconHit.getTime());
                if (debug) {
                    System.out.format("recon hit: energy %f, time %f\n", reconHit.getCorrectedEnergy(), reconHit.getTime());
                }
            }
        }
    }

    private static class EnergyComparator implements Comparator<CalorimeterHit> {

        @Override
        public int compare(CalorimeterHit hit1, CalorimeterHit hit2) {
            return Double.compare(hit1.getRawEnergy(), hit2.getRawEnergy());
        }
    }
}
