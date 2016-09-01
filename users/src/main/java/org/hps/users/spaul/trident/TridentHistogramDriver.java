package org.hps.users.spaul.trident;

import java.util.List;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IProfile1D;

import org.hps.recon.tracking.TrackType;
import org.hps.users.spaul.StyleUtil;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TridentHistogramDriver extends Driver{

    private String[] tridentCollections =  new String[]{
            "TargetConstrainedV0Vertices",
            "UnconstrainedV0Vertices",
            "BeamspotConstrainedV0Vertices",
    };


    @Override
    public void process(EventHeader event){
        for(int i = 0; i< tridentCollections.length; i++){
            List<Vertex> tridents = event.get(Vertex.class, tridentCollections[i]);
            
            for(Vertex v : tridents){
            	
            		
                if(!passesCuts(v))
                    continue;
                ReconstructedParticle part = v.getAssociatedParticle();
                ReconstructedParticle plus;
                ReconstructedParticle minus;
                if(part.getParticles().get(0).getCharge()>0){
                    plus = part.getParticles().get(0);
                    minus = part.getParticles().get(1);
                }else{
                    plus = part.getParticles().get(1);
                    minus = part.getParticles().get(0);
                }

                double pypz = part.getMomentum().y()/part.getMomentum().z();
                double pxpz = part.getMomentum().x()/part.getMomentum().z();
                //double pypz = (top.getMomentum().y()+bottom.getMomentum().y())/(top.getMomentum().z()+bottom.getMomentum().z());
                //double pxpz = (top.getMomentum().x()+bottom.getMomentum().x())/(top.getMomentum().z()+bottom.getMomentum().z());

                hpypz[i].fill(pypz);
                hpxpz[i].fill(pxpz);


                double diff = plus.getMomentum().z()-minus.getMomentum().z();
                double sum = part.getMomentum().z();
                double mass = part.getMass();

                

                this.pz_diff[i].fill(diff);
                this.pz_sum[i].fill(sum);
                this.mass[i].fill(mass);
                
                double energy = v.getAssociatedParticle().getEnergy();
                this.mass_vs_energy[i].fill(mass, energy);
                this.mass_vs_pz[i].fill(mass, part.getMomentum().z());
                
                timediff[i].fill(plus.getClusters().get(0).getCalorimeterHits().get(0).getTime()
                		-minus.getClusters().get(0).getCalorimeterHits().get(0).getTime());
                this.chi2Vtx[i].fill(v.getChi2());
                this.chi2TrkPlus[i].fill(plus.getTracks().get(0).getChi2());
                this.chi2TrkMinus[i].fill(minus.getTracks().get(0).getChi2());
                
                this.vtx_x[i].fill(v.getPosition().x());
                this.vtx_y[i].fill(v.getPosition().y());
                this.vtx_xy[i].fill(v.getPosition().x(), v.getPosition().y());
                
                this.pz_vs_pz[i].fill(minus.getMomentum().z(), plus.getMomentum().z());
            }
        }
    }

    
    double _maxVtxChi2 = 100;
    double _maxTrkChi2 = 150;
    double _maxMass = .2;
    double _minMass = 0;
    double _minVtxPz = 0.0;
    double _maxVtxPz = 3.0;
    
    double _maxTrkPz = 1.9;
    double _maxPzDiff = 4.0;
    double _timeDiffCut = 1.2;
    boolean passesCuts(Vertex vertex){
        ReconstructedParticle part = vertex.getAssociatedParticle();
        
        ReconstructedParticle p1 = part.getParticles().get(0);
        ReconstructedParticle p2 = part.getParticles().get(1);
        
        
        //first make sure the track type is GBL
        if(!TrackType.isGBL(part.getType()))
            return false;
        // make sure both tracks are matched to clusters
        if(p1.getClusters().size() == 0)
            return false;
        if(p2.getClusters().size() == 0)
            return false;

        Cluster c1 = p1.getClusters().get(0);
        Cluster c2 = p2.getClusters().get(0);
        
        
        //make sure the clusters are on opposite sides of detector.  
        if(c1.getPosition()[1]*c2.getPosition()[1] >0)
        	return false;
        
        //plot the time difference (top minus bottom) versus the energy sum
        double dt = c1.getCalorimeterHits().get(0).getTime()
		-c2.getCalorimeterHits().get(0).getTime();
        timediff_vs_esum[0].fill(Math.signum(c1.getPosition()[1])*dt,
        	part.getEnergy());
        
        //and that they are from the same beam bunch
        
        if(Math.abs(dt)>_timeDiffCut)
        	return false;
        
        
        //make sure the total momentum is a reasonable range.   
        if(part.getMomentum().z() > _maxVtxPz || part.getMomentum().z() < _minVtxPz)
            return false;
        
        // mass within a proper window.
        if(part.getMass() > _maxMass || part.getMass() < _minMass)
            return false;
        
        //fee momentum cut
        if(p1.getMomentum().z() > 1.9 || p2.getMomentum().z() > 1.9)
        	return false;
        
        //oppositely charged particles.
        if(part.getParticles().get(0).getCharge()
        		+ part.getParticles().get(1).getCharge() != 0)
            return false;

        // make sure the chi^2 of the vertex fit is reasonable 
        if(vertex.getChi2() > _maxVtxChi2)
            return false;
        
        //and also the chi^2 of the individual tracks are reasonable as well.
        if(part.getParticles().get(0).getTracks().get(0).getChi2() > _maxTrkChi2)
            return false;
        if(part.getParticles().get(1).getTracks().get(0).getChi2() > _maxTrkChi2)
            return false;
        return true;
    }

    IHistogram1D hpypz[], hpxpz[], pz_diff[], pz_sum[], mass[];
    
    IHistogram1D chi2TrkPlus[];
    IHistogram1D chi2TrkMinus[];
    IHistogram1D chi2Vtx[];
    
    IHistogram1D vtx_x[], vtx_y[], timediff[];

    
	private IHistogram2D[] mass_vs_energy;
	private IHistogram2D[] vtx_xy;
	private IHistogram2D[] pz_vs_pz;
	private IHistogram2D[] mass_vs_pz;
	private IHistogram2D[] timediff_vs_esum;



    public double getMaxVtxChi2() {
        return _maxVtxChi2;
    }


    public void setMaxVtxChi2(double _maxVtxChi2) {
        this._maxVtxChi2 = _maxVtxChi2;
    }


    public double getMaxTrkChi2() {
        return _maxTrkChi2;
    }


    public void setMaxTrkChi2(double _maxTrkChi2) {
        this._maxTrkChi2 = _maxTrkChi2;
    }


    public double getMaxMass() {
        return _maxMass;
    }


    public void setMaxMass(double _maxMass) {
        this._maxMass = _maxMass;
    }


    public double getMinMass() {
        return _minMass;
    }


    public void setMinMass(double _minMass) {
        this._minMass = _minMass;
    }


    public double getMinVtxPz() {
        return _minVtxPz;
    }


    public void setMinVtxPz(double _minPz) {
        this._minVtxPz = _minPz;
    }


    public double getMaxVtxPz() {
        return _maxVtxPz;
    }

    public void setMaxVtxPz(double _maxPz) {
        this._maxVtxPz = _maxPz;
    }

    


    //IHistogram1D pypz_tophighE, pxpz_tophighE;
    //IHistogram1D pypz_bottomhighE, pxpz_bottomhighE;
    @Override
    public void startOfData(){
        AIDA aida = AIDA.defaultInstance();
        hpypz = new IHistogram1D[3];
        hpxpz = new IHistogram1D[3];
        
        
        pz_diff= new IHistogram1D[3];

        pz_sum= new IHistogram1D[3];


        

        vtx_x= new IHistogram1D[3];
        vtx_y= new IHistogram1D[3];
        
        vtx_xy = new IHistogram2D[3]; 
        
        mass= new IHistogram1D[3];
        timediff= new IHistogram1D[3];
        timediff_vs_esum = new IHistogram2D[3];
        mass_vs_energy = new IHistogram2D[3];
        mass_vs_pz = new IHistogram2D[3];
        chi2Vtx = new IHistogram1D[3];
        chi2TrkPlus = new IHistogram1D[3];
        chi2TrkMinus = new IHistogram1D[3];
        pz_vs_pz = new IHistogram2D[3]; 
        for(int i = 0; i< 3; i++){
            
            hpypz[i] = aida.histogram1D(tridentCollections[i]+"/"+"pypz", 80, -.020,.020);
            hpxpz[i] = aida.histogram1D(tridentCollections[i]+"/"+"pxpz", 80, .010,.050);

            vtx_x[i] = aida.histogram1D(tridentCollections[i]+"/"+"vtx x", 200, -2, 2);
            vtx_y[i] = aida.histogram1D(tridentCollections[i]+"/"+"vtx y", 200, -2, 2);


            vtx_xy[i] = aida.histogram2D(tridentCollections[i]+"/"+"vtx xy", 200, -2, 2, 200, -2, 2);
        
            pz_diff[i] = aida.histogram1D(tridentCollections[i]+"/"+"pz pos - ele",120, -_maxPzDiff, _maxPzDiff);

            pz_sum[i] = aida.histogram1D(tridentCollections[i]+"/"+"pz sum", 100, _minVtxPz, _maxVtxPz);
           
            //vtx_x[i] = aida.histogram1D(tridentCollections[i]+"/"+"vtx x", 60, -1, 1);
            //vtx_y[i] = aida.histogram1D(tridentCollections[i]+"/"+"vtx y", 60, -1, 1);
            
            //for 0-200 MeV, this yields 0.1 MeV mass bins
            mass[i] = aida.histogram1D(tridentCollections[i]+"/"+"mass", 2000, _minMass, _maxMass); 
            //
            mass_vs_energy[i] = aida.histogram2D(tridentCollections[i]+"/"+"mass vs energy sum", 2000, _minMass, _maxMass, 100, 0, 5); 
            mass_vs_pz[i] = aida.histogram2D(tridentCollections[i]+"/"+"mass vs pz sum", 2000, _minMass, _maxMass, 100, 0, 5); 
            
            
            timediff[i] = aida.histogram1D(tridentCollections[i]+"/"+"time diff", 60, -6, 6);
            timediff_vs_esum[i] = aida.histogram2D(tridentCollections[i]+"/"+"time diff vs energy sum", 60, -6, 6, 100, 0, 3);
            
            chi2Vtx[i] = aida.histogram1D(tridentCollections[i]+"/"+"chi2 vertex", 100, 0, 100);
            chi2TrkPlus[i] = aida.histogram1D(tridentCollections[i]+"/"+"chi2 track plus", 100, 0, 100);
            chi2TrkMinus[i] = aida.histogram1D(tridentCollections[i]+"/"+"chi2 track minus", 100, 0, 100);   
            pz_vs_pz[i] = aida.histogram2D(tridentCollections[i]+"/"+"pz ele vs pos", 100, 0, 4, 100, 0, 4);
        }
    }
}
