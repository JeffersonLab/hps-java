package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.util.Pair;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.util.Driver;

public class EcalTruthTestDriver extends Driver {
    @Override
    public void process(EventHeader event) {
        List<SimCalorimeterHit> hits = null;
        if(event.hasCollection(SimCalorimeterHit.class, "EcalHits")) {
            hits = event.get(SimCalorimeterHit.class, "EcalHits");
        }
        if(hits == null || hits.isEmpty()) { return; }
        
        
        /*
        List<SimCalorimeterHit> hits = null;
        if(event.hasCollection(SimCalorimeterHit.class, "EcalCalHits")) {
            hits = event.get(SimCalorimeterHit.class, "EcalCalHits");
        } else {
            hits = new ArrayList<SimCalorimeterHit>(0);
        }
        */
        
        System.out.println("Printing truth data for event " + event.getEventNumber() + ".");
        
        /*
        System.out.println("Outputting summary of hit energy contributions by actual contributing particle...");
        for(SimCalorimeterHit hit : hits) {
            System.out.println("\t" + getHitString(hit));
            Map<MCParticle, Double> percentEnergyMap = getPercentEnergyMap(hit);
            for(Map.Entry<MCParticle, Double> entry : percentEnergyMap.entrySet()) {
                System.out.printf("\t\t%5.1f%% :: %s%n", entry.getValue().doubleValue() * 100.0, getParticleString(entry.getKey()));
            }
        }
        System.out.println();
        
        System.out.println("Outputting summary of hit energy contributions by incident contributing particle...");
        for(SimCalorimeterHit hit : hits) {
            System.out.println("\t" + getHitString(hit));
            Map<MCParticle, Double> percentEnergyMap = getIncidentPercentEnergyMap(hit);
            for(Map.Entry<MCParticle, Double> entry : percentEnergyMap.entrySet()) {
                System.out.printf("\t\t%5.1f%% :: %s%n", entry.getValue().doubleValue() * 100.0, getParticleString(entry.getKey()));
            }
        }
        System.out.println();
        */
        
        System.out.println("Outputting summary of particle contributions by hit...");
        Map<MCParticle, Pair<List<SimCalorimeterHit>, List<Double>>> particleContributionMap = getHitContributionMap(hits);
        for(Map.Entry<MCParticle, Pair<List<SimCalorimeterHit>, List<Double>>> entry : particleContributionMap.entrySet()) {
            System.out.println("\t" + getParticleString(entry.getKey()));
            for(int i = 0; i < entry.getValue().getFirstElement().size(); i++) {
                System.out.printf("\t\t%5.1f%% :: %s%n", entry.getValue().getSecondElement().get(i).doubleValue() * 100.0, getHitString(entry.getValue().getFirstElement().get(i)));
            }
        }
        System.out.println("\n\n");
    }
    
    private static final MCParticle getIncidentEcalParticle(MCParticle particle) {
        // The calorimeter face occurs at approximately 1318 mm. We
        // allow a little extra distance for safety.
        final int ecalFace = 1330;
        
        // Check the position of the particle's production vertex. If
        // it is within the calorimeter, get its parent and perform
        // the same test. Repeat until the current particle is not
        // produced within the calorimeter.
        MCParticle curParticle = particle;
        while(true) {
            // Particles are expected to only ever have one parent.
            if(curParticle.getParents().isEmpty()) {
                return curParticle;
            } else if(curParticle.getParents().size() != 1) {
                throw new RuntimeException("Error: Particles are expected to have either 0 or 1 parent(s) - saw "
                        + particle.getParents().size() + ".");
            }
            
            // If the particle was created before the calorimeter
            // face, this is the "final" particle.
            if(curParticle.getOriginZ() < ecalFace) {
                break;
            }
            
            // Otherwise, get the particle's parent and return that.
            // Note that the A' should never be returned, so if the
            // parent is the A', just return the current particle.
            if(curParticle.getParents().get(0).getPDGID() == 622) {
                break;
            } else {
                curParticle = curParticle.getParents().get(0);
            }
        }
        
        // Return the particle
        return curParticle;
    }
    
    private static final Map<MCParticle, Pair<List<SimCalorimeterHit>, List<Double>>> getHitContributionMap(Collection<SimCalorimeterHit> hits) {
        Map<MCParticle, Pair<List<SimCalorimeterHit>, List<Double>>> dataMap = new HashMap<MCParticle, Pair<List<SimCalorimeterHit>, List<Double>>>();
        for(SimCalorimeterHit hit : hits) {
            Map<MCParticle, Double> percentEnergyMap = getPercentEnergyMap(hit);
            for(Map.Entry<MCParticle, Double> entry : percentEnergyMap.entrySet()) {
                if(dataMap.containsKey(entry.getKey())) {
                    Pair<List<SimCalorimeterHit>, List<Double>> dataPair = dataMap.get(entry.getKey());
                    dataPair.getFirstElement().add(hit);
                    dataPair.getSecondElement().add(entry.getValue());
                } else {
                    Pair<List<SimCalorimeterHit>, List<Double>> dataPair
                            = new Pair<List<SimCalorimeterHit>, List<Double>>(new ArrayList<SimCalorimeterHit>(), new ArrayList<Double>());
                    dataMap.put(entry.getKey(), dataPair);
                    dataPair.getFirstElement().add(hit);
                    dataPair.getSecondElement().add(entry.getValue());
                }
            }
        }
        
        return dataMap;
    }
    
    private static final Map<MCParticle, Double> getIncidentPercentEnergyMap(SimCalorimeterHit hit) {
        double totalEnergy = 0.0;
        Map<MCParticle, Double> percentEnergyMap = new HashMap<MCParticle, Double>();
        
        for(int i = 0; i < hit.getMCParticleCount(); i++) {
            totalEnergy += hit.getContributedEnergy(i);
            
            MCParticle incidentParticle = getIncidentEcalParticle(hit.getMCParticle(i));
            if(percentEnergyMap.containsKey(incidentParticle)) {
                double curEnergy = percentEnergyMap.get(incidentParticle).doubleValue();
                curEnergy += hit.getContributedEnergy(i);
                percentEnergyMap.put(incidentParticle, Double.valueOf(curEnergy));
            } else {
                percentEnergyMap.put(incidentParticle, Double.valueOf(hit.getContributedEnergy(i)));
            }
        }
        
        return scaleMap(percentEnergyMap, totalEnergy);
    }
    
    private static final Map<MCParticle, Double> getPercentEnergyMap(SimCalorimeterHit hit) {
        double totalEnergy = 0.0;
        Map<MCParticle, Double> percentEnergyMap = new HashMap<MCParticle, Double>();
        
        for(int i = 0; i < hit.getMCParticleCount(); i++) {
            totalEnergy += hit.getContributedEnergy(i);
            if(percentEnergyMap.containsKey(hit.getMCParticle(i))) {
                double curEnergy = percentEnergyMap.get(hit.getMCParticle(i)).doubleValue();
                curEnergy += hit.getContributedEnergy(i);
                percentEnergyMap.put(hit.getMCParticle(i), Double.valueOf(curEnergy));
            } else {
                percentEnergyMap.put(hit.getMCParticle(i), Double.valueOf(hit.getContributedEnergy(i)));
            }
        }
        
        return scaleMap(percentEnergyMap, totalEnergy);
    }
    
    private static final Map<MCParticle, Double> scaleMap(Map<MCParticle, Double> map, double scaleValue) {
        for(Map.Entry<MCParticle, Double> entry : map.entrySet()) {
            entry.setValue(Double.valueOf(entry.getValue().doubleValue() / scaleValue));
        }
        return map;
    }
    
    private static final String getHitString(CalorimeterHit hit) {
        if(hit == null) {
            return "Hit at UNDEFINED with energy NaN GeV at time NaN ns.";
        } else {
            return String.format("Hit at (%3d, %2d) with energy %5.3f GeV at time %.1f ns.", hit.getIdentifierFieldValue("ix"),
                    hit.getIdentifierFieldValue("iy"), hit.getRawEnergy(), hit.getTime());
        }
    }
    
    private static final String getParticleString(MCParticle particle) {
        if(particle == null) {
            return "Particle with PDGID UNDEFINED produced at time t = NaN ns with charge NaN C and momentum NaN GeV.";
        } else {
            String particleName = null;
            int pid = particle.getPDGID();
            if(pid == 11) { particleName = "e-"; }
            else if(pid == -11) { particleName = "e+"; }
            else if(pid == 22) { particleName = "g"; }
            else { particleName = Integer.toString(pid); }
            
            return String.format("Particle of type %3s produced at time t = %5.1f ns and vertex <%6.1f, %6.1f, %6.1f> with charge %2.0f C and momentum %5.3f GeV.",
                    particleName, particle.getProductionTime(), particle.getOriginX(), particle.getOriginY(), particle.getOriginZ(), particle.getCharge(), particle.getEnergy());
        }
    }
}