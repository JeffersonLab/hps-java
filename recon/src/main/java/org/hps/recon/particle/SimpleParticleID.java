package org.hps.recon.particle;

import org.lcsim.event.ParticleID;

public class SimpleParticleID implements ParticleID {
    
    int algorithmType = 0; 
    int pdgID = UnknownPDG;
    int type = 0; 
    
    double likelihood = 0.; 
    // TODO: Need to define what other parameters are needed.
    double[] parameters = new double[1]; 

    public SimpleParticleID(){}
    
    public SimpleParticleID(int pdgID, int algorithmType, int type, double likelihood){
        this.pdgID = pdgID; 
        this.algorithmType = algorithmType; 
        this.type = type; 
        this.likelihood = likelihood; 
    }
    
    @Override
    public int getAlgorithmType() {
        return algorithmType;
    }

    @Override
    public double getLikelihood() {
        return likelihood;
    }

    @Override
    public int getPDG() {
        return pdgID;
    }

    @Override
    public double[] getParameters() {
        return parameters;
    }

    @Override
    public int getType() {
        return type;
    }
    
    public void setAlgorithmType(int algorithmType){
        this.algorithmType = algorithmType;
    }
    
    public void setLikelihood(int likelihood){
        this.likelihood = likelihood; 
    }
    
    public void setPDG(int pdgID){
        this.pdgID = pdgID; 
    }
    
    public void setType(int type){
        this.type = type; 
    }
    
    public void setParameters(double[] parameters){
        this.parameters = parameters;
    }

}
