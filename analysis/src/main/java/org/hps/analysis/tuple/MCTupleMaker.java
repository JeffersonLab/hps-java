package org.hps.analysis.tuple;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import java.util.Arrays;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;


public abstract class MCTupleMaker extends TupleMaker {
    

    private int nEcalHit = 3;
    
    public void setNEcalHit(int nEcalHit) {
        this.nEcalHit = nEcalHit;
    }

    protected void addMCTridentVariables() {
        addMCParticleVariables("tri");
        addMCParticleVariables("triEle1");
        addMCParticleVariables("triEle2");
        addMCParticleVariables("triPos");
        addMCPairVariables("triPair1");
        addMCPairVariables("triPair2");
    }
    
    protected void fillMCTridentVariables(EventHeader event) {
        List<MCParticle> MCParticles = event.getMCParticles();

        MCParticle trident = null;

        MCParticle ele1 = null;// highest-energy electron daughter
        MCParticle ele2 = null;// second-highest-energy electron daughter (if any)
        MCParticle pos = null;// highest-energy positron daughter

        List<MCParticle> tridentParticles = null;

        for (MCParticle particle : MCParticles) {
            if (particle.getPDGID() == 622) {
                trident = particle;
                tridentParticles = particle.getDaughters();
                break;
            }
        }
        if (trident == null) {
            return;
        }

        fillMCParticleVariables("tri", trident);

        for (MCParticle particle : tridentParticles) {
            switch (particle.getPDGID()) {
                case -11:
                    if (pos == null || particle.getEnergy() > pos.getEnergy()) {
                        pos = particle;
                    }
                    break;
                case 11:
                    if (ele1 == null || particle.getEnergy() > ele1.getEnergy()) {
                        ele2 = ele1;
                        ele1 = particle;
                    } else if (ele2 == null || particle.getEnergy() > ele2.getEnergy()) {
                        ele2 = particle;
                    }
                    break;
            }
        }

        if (ele1 != null) {
            fillMCParticleVariables("triEle1", ele1);
        }
        if (ele2 != null) {
            fillMCParticleVariables("triEle2", ele2);
        }
        if (pos != null) {
            fillMCParticleVariables("triPos", pos);
        }

        if (pos != null && ele1 != null) {
            fillMCPairVariables("triPair1", ele1, pos);
            if (ele2 != null) {
                fillMCPairVariables("triPair2", ele2, pos);
            }
        }

    }
    
    protected void fillTruthEventVariables(EventHeader event) {
        tupleMap.put("run/I", (double) event.getRunNumber());
        tupleMap.put("event/I", (double) event.getEventNumber());
        //tupleMap.put("tupleevent/I", (double) tupleevent);
        //tupleevent++;
    }
    
    protected void addMCEcalVariables(String prefix) {
        String[] newVars = new String[]{
                "ecalhitIx/I","ecalhitIy/I","ecalhitX/D","ecalhitY/D","ecalhitZ/D",
                "ecalhitEnergy/D"};
        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void addMCParticleVariables(String prefix) {
        String[] newVars = new String[] {"StartX/D", "StartY/D", "StartZ/D", "EndX/D", "EndY/D", "EndZ/D", "StartPX/D","StartPY/D", "StartPZ/D", 
                "StartP/D", "M/D", "E/D","pdgid/I","parentID/I","HasTruthMatch/I","NTruthHits/I","NGoodTruthHits/I","NBadTruthHits/I",
                "Purity/D"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void addMCSVTVariables(String prefix, boolean inactive) {
        String[] newVars = null;
        if(!inactive)
            newVars = new String[] {"svthitX/D","svthitY/D","svthitZ/D",
                "svthitPx/D","svthitPy/D","svthitPz/D","thetaX/D","thetaY/D","residualX/D","residualY/D",
                "NTruthParticles/I","IsGoodTruthHit/I"};
        else{
            newVars = new String[] {"svthitX/D","svthitY/D","svthitZ/D",
                "svthitPx/D","svthitPy/D","svthitPz/D","thetaX/D","thetaY/D","residualX/D","residualY/D"};
        }
        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void addEcalTruthVariables(String prefix){
        for(int i = 0; i < nEcalHit; i++){
            String hit = Integer.toString(i);
            addMCEcalVariables(prefix+"Hit"+hit);
        }
    }

    protected void addSVTTruthVariables(String prefix){    
        for(int i = 0; i < nLay*2; i++){
            if(i + 1 > nTrackingLayers*2)
                break;
            String layer = Integer.toString(i+1);
            addMCSVTVariables(prefix+"L"+layer+"t",false);
            addMCSVTVariables(prefix+"L"+layer+"b",false);
            addMCSVTVariables(prefix+"L"+layer+"tIn",true);
            addMCSVTVariables(prefix+"L"+layer+"bIn",true);
        }
    }

    protected void addMCPairVariables(String prefix) {
        String[] newVars = new String[] {"PX/D", "PY/D", "PZ/D", "P/D", "M/D", "E/D"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void fillMCParticleVariables(String prefix, MCParticle particle) {
        // System.out.format("%d %x\n", particle.getGeneratorStatus(), particle.getSimulatorStatus().getValue());
        Hep3Vector start = VecOp.mult(beamAxisRotation, particle.getOrigin());
        Hep3Vector end;
        MCParticle parent;
        try {
            end = VecOp.mult(beamAxisRotation, particle.getEndPoint());
        } catch (RuntimeException e) {
            end = null;
        }
        
        try {
            parent = particle.getParents().get(0);
        } catch (RuntimeException e) {
            parent = null;
        }

        Hep3Vector p = VecOp.mult(beamAxisRotation, particle.getMomentum());

        tupleMap.put(prefix + "StartX/D", start.x());
        tupleMap.put(prefix + "StartY/D", start.y());
        tupleMap.put(prefix + "StartZ/D", start.z());
        if (end != null) {
            tupleMap.put(prefix + "EndX/D", end.x());
            tupleMap.put(prefix + "EndY/D", end.y());
            tupleMap.put(prefix + "EndZ/D", end.z());
        }
        tupleMap.put(prefix + "StartPX/D", p.x());
        tupleMap.put(prefix + "StartPY/D", p.y());
        tupleMap.put(prefix + "StartPZ/D", p.z());
        tupleMap.put(prefix + "StartP/D", p.magnitude());
        tupleMap.put(prefix + "M/D", particle.getMass());
        tupleMap.put(prefix + "E/D", particle.getEnergy());
        tupleMap.put(prefix + "pdgid/I", (double) particle.getPDGID());
        if(parent != null){
            tupleMap.put(prefix + "parentID/I", (double) parent.getPDGID());
        }
    }

    protected void fillMCPairVariables(String prefix, MCParticle ele, MCParticle pos) {
        HepLorentzVector vtx = VecOp.add(ele.asFourVector(), pos.asFourVector());
        Hep3Vector vtxP = VecOp.mult(beamAxisRotation, vtx.v3());
        tupleMap.put(prefix + "PX/D", vtxP.x());
        tupleMap.put(prefix + "PY/D", vtxP.y());
        tupleMap.put(prefix + "PZ/D", vtxP.z());
        tupleMap.put(prefix + "P/D", vtxP.magnitude());
        tupleMap.put(prefix + "M/D", vtx.magnitude());
        tupleMap.put(prefix + "E/D", vtx.t());
    }
    
    
    protected void fillMCWabVariables(EventHeader event) {
        List<MCParticle> MCParticles = event.getMCParticles();

        MCParticle wab = null;

        MCParticle ele1 = null;// conversion electron daughter
        MCParticle ele2 = null;// recoil wab electron
        MCParticle pos = null;// conversion positron daughter

        List<MCParticle> wabParticles = null;

        for (MCParticle particle : MCParticles) {
            if (particle.getPDGID() == 22 && particle.getGeneratorStatus() == 1 && particle.getDaughters().size() == 2) {
                double wabEnergy = particle.getEnergy();
                for(MCParticle p : MCParticles){
                    if(p.getPDGID() != 11 || !p.getParents().isEmpty()) continue;
                    double eleEnergy = p.getEnergy();
                    double energy = wabEnergy + eleEnergy;
                    if(energy < 0.98 * ebeam || energy > 1.02 * ebeam) continue;
                    ele2 = p;
                    wab = particle;
                    wabParticles = wab.getDaughters();
                    break;
                }
            }
        }
        if (wab == null) {
            return;
        }

        fillMCParticleVariables("wab", wab);

        for (MCParticle particle : wabParticles) {
            if(particle.getPDGID() == 11){
                pos = particle;
            }
            if(particle.getPDGID() == -11){
                ele1 = particle;
            }
        }

        if (ele1 != null) {
            fillMCParticleVariables("wabEle1", ele1);
        }
        if (ele2 != null) {
            fillMCParticleVariables("wabEle2", ele2);
        }
        if (pos != null) {
            fillMCParticleVariables("wabPos", pos);
        }
    }
    
 
}