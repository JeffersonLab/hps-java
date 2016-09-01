package org.hps.users.spaul.moller;

import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.hypot;
import static java.lang.Math.sin;

import java.util.List;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;

import org.hps.recon.tracking.TrackType;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class MollerBeamtiltAnalysis extends Driver{

    private String[] mollerCollections =  new String[]{
            "TargetConstrainedMollerVertices",
            "UnconstrainedMollerVertices",
            "BeamspotConstrainedMollerVertices",
    };


    @Override
    public void process(EventHeader event){
        for(int i = 0; i< mollerCollections.length; i++){
            List<Vertex> mollers = event.get(Vertex.class, mollerCollections[i]);
            Cleanup.purgeDuplicates(mollers);
            int nPassCuts = 0;
            for(Vertex v : mollers){
                if(!passesBradleysCuts(v))
                    continue;
                nPassCuts ++;
                ReconstructedParticle m = v.getAssociatedParticle();
                ReconstructedParticle top;
                ReconstructedParticle bottom;
                if(m.getParticles().get(0).getMomentum().y()>0){
                    top = m.getParticles().get(0);
                    bottom = m.getParticles().get(1);
                }else{
                    top = m.getParticles().get(1);
                    bottom = m.getParticles().get(0);
                }

                double pypz = m.getMomentum().y()/m.getMomentum().z();
                double pxpz = m.getMomentum().x()/m.getMomentum().z();
                //double pypz = (top.getMomentum().y()+bottom.getMomentum().y())/(top.getMomentum().z()+bottom.getMomentum().z());
                //double pxpz = (top.getMomentum().x()+bottom.getMomentum().x())/(top.getMomentum().z()+bottom.getMomentum().z());

                hpypz[i].fill(pypz);
                hpxpz[i].fill(pxpz);


                double diff = top.getMomentum().z()-bottom.getMomentum().z();
                double sum = m.getMomentum().z();//top.getMomentum().z()+bottom.getMomentum().z();
                double mass = m.getMass();

               


                this.diff[i].fill(diff);
                this.sum[i].fill(sum);
                this.mass[i].fill(mass);
                pypz_vs_diff[i].fill(pypz, diff );
                pxpz_vs_diff[i].fill(pxpz, diff );



                pxpz_vs_sum[i].fill(pxpz, sum );
                pypz_vs_sum[i].fill(pypz, sum );

                pxpz_vs_mass[i].fill(pxpz, mass);
                pypz_vs_mass[i].fill(pypz, mass);
                //only do this if both tracks have associated clusters:
                if(top.getClusters().size()*bottom.getClusters().size()>0){
                	timediff[i].fill(top.getClusters().get(0).getCalorimeterHits().get(0).getTime()
                        -bottom.getClusters().get(0).getCalorimeterHits().get(0).getTime());
                	meanClusterTime[i].fill((top.getClusters().get(0).getCalorimeterHits().get(0).getTime()
                            +bottom.getClusters().get(0).getCalorimeterHits().get(0).getTime())/2.);
                }
                
                double [] res = getThetaAndPhi(top.getMomentum());
                double theta1 = res[0];
                double phi1 = res[1];
                thetaPhi[i].fill(theta1, phi1);
                
                res = getThetaAndPhi(bottom.getMomentum());
                double theta2 = res[0];
                double phi2 = res[1];
                thetaPhi[i].fill(theta2, phi2);
                
                vtxchi2[i].fill(v.getChi2());
                trkchi2[i].fill(top.getTracks().get(0).getChi2());
                trkchi2[i].fill(bottom.getTracks().get(0).getChi2());
                
                vtx_xy[i].fill(v.getPosition().x(), v.getPosition().y());
                
                double dPhi = phi1-phi2 + Math.PI;
                while(dPhi > Math.PI)
                	dPhi-= 2*Math.PI;
                while(dPhi< -Math.PI)
                	dPhi += 2*Math.PI;
                dphi[i].fill(dPhi*180/Math.PI);
                dphi_vs_dtheta[i].fill(dPhi*180/Math.PI, theta1-theta2);
                
                theta_theta[i].fill(sin(theta1/2)*sin(theta2/2));
                
            }
            this.nPassCuts[i].fill(nPassCuts);
        }
    }

    //private double _maxVtxChi2 = 15;
    //private double _maxTrkChi2 = 30;
    private double _maxVtxChi2 = 30;
    private double _maxTrkChi2 = 50;
    private double _maxMass = .037;
    private double _minMass = .030;
    private double _minPz = 1.0;
    private double _maxPz = 1.1;
    private double _maxTrkPz = .8;
    public double get_maxTrkPz() {
		return _maxTrkPz;
	}


	public void set_maxTrkPz(double _maxTrkPz) {
		this._maxTrkPz = _maxTrkPz;
	}


	private boolean passesCuts(Vertex vertex){
        ReconstructedParticle m = vertex.getAssociatedParticle();
        if(!TrackType.isGBL(m.getType()))
            return false;
        if(m.getMomentum().z() > _maxPz || m.getMomentum().z() < _minPz)
            return false;
        if(m.getMass() > _maxMass || m.getMass() < _minMass)
            return false;

        
        if(m.getParticles().get(0).getCharge() != -1 
                || m.getParticles().get(1).getCharge() != -1 )
            return false;

        if(vertex.getChi2() > _maxVtxChi2)
            return false;


        if(m.getParticles().get(0).getClusters().size() == 0)
            return false;
        if(m.getParticles().get(1).getClusters().size() == 0)
            return false;

        if(m.getParticles().get(0).getTracks().get(0).getChi2() > _maxTrkChi2)
            return false;
        if(m.getParticles().get(1).getTracks().get(0).getChi2() > _maxTrkChi2)
            return false;
        
        if(m.getParticles().get(0).getMomentum().z()>_maxTrkPz )
        	return false;
        if(m.getParticles().get(1).getMomentum().z()>_maxTrkPz )
        	return false;
        
        //very loose cut around the nominal beam direction
        if(Math.abs(m.getMomentum().x()/m.getMomentum().z()-.030)> .01)
        	return false;
        if(Math.abs(m.getMomentum().y()/m.getMomentum().z()-.000)> .01)
        	return false;
        
        return true;
    }
	
	private boolean passesBradleysCuts(Vertex vertex){
		ReconstructedParticle m = vertex.getAssociatedParticle();
        if(!TrackType.isGBL(m.getType()))
            return false;
        if(m.getMomentum().z() < .95 || m.getMomentum().z() > 1.15)
            return false;
        
        //both particles must have clusters
        if(m.getParticles().get(0).getClusters().size()==0 || m.getParticles().get(1).getClusters().size()==0)
        	return false;
        
        
        ReconstructedParticle p1 = m.getParticles().get(0);
        ReconstructedParticle p2 = m.getParticles().get(1);
        
        //timing difference cut (2 ns)
        if(Math.abs(p1.getClusters().get(0).getCalorimeterHits().get(0).getTime()-
        		p2.getClusters().get(0).getCalorimeterHits().get(0).getTime())>2)
        	return false;
        
        
        double phi1 = getThetaAndPhi(p1.getMomentum())[1];
        double phi2 = getThetaAndPhi(p2.getMomentum())[1];
        
        double dPhi = phi1-phi2 + Math.PI;
        while(dPhi>Math.PI)
        	dPhi-= 2*Math.PI;
        while(dPhi<-Math.PI)
        	dPhi+= 2*Math.PI;
        
        if(Math.abs(dPhi)>.35) //about 20 degrees
        	return false;
		return true;
	}

    private IHistogram1D hpypz[], hpxpz[], diff[], sum[], mass[], trkchi2[], vtxchi2[], dphi[];

    private IHistogram2D[] thetaPhi, dphi_vs_dtheta;
    
    private IHistogram1D[] meanClusterTime;
    
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


    public double getMinPz() {
        return _minPz;
    }


    public void setMinPz(double _minPz) {
        this._minPz = _minPz;
    }


    public double getMaxPz() {
        return _maxPz;
    }


    public void setMaxPz(double _maxPz) {
        this._maxPz = _maxPz;
    }


    private IHistogram2D vtx_xy[];
    private IHistogram1D timediff[];
    private IHistogram1D nPassCuts[], theta_theta[];
    

    private IHistogram2D pxpz_vs_diff[], pypz_vs_diff[], pxpz_vs_sum[], pypz_vs_sum[],
    pxpz_vs_mass[], pypz_vs_mass[];


    //IHistogram1D pypz_tophighE, pxpz_tophighE;
    //IHistogram1D pypz_bottomhighE, pxpz_bottomhighE;
    @Override
    public void startOfData(){
        AIDA aida = AIDA.defaultInstance();
        
        
        
        thetaPhi = new IHistogram2D[3];
        
        hpypz = new IHistogram1D[3];
        hpxpz = new IHistogram1D[3];
        
        
        pxpz_vs_diff= new IHistogram2D[3];
        pypz_vs_diff= new IHistogram2D[3];

        diff= new IHistogram1D[3];

        sum= new IHistogram1D[3];

        pxpz_vs_sum= new IHistogram2D[3];
        pypz_vs_sum= new IHistogram2D[3];

        pxpz_vs_mass= new IHistogram2D[3];
        pypz_vs_mass= new IHistogram2D[3];

        //vtx_x= new IHistogram1D[3];
        //vtx_y= new IHistogram1D[3];
        mass= new IHistogram1D[3];
        timediff= new IHistogram1D[3];
        nPassCuts = new IHistogram1D[3];
        vtx_xy = new IHistogram2D[3];
        trkchi2 = new IHistogram1D[3];
        vtxchi2 = new IHistogram1D[3];
        meanClusterTime = new IHistogram1D[3];
        
        dphi = new IHistogram1D[3];
        
        theta_theta = new IHistogram1D[3];
        
        dphi_vs_dtheta = new IHistogram2D[3];
        
        for(int i = 0; i< 3; i++){
        	
        	thetaPhi[i] = aida.histogram2D(mollerCollections[i]+"/"+"theta vs phi", 100, 0, .2, 628, -3.14, 3.14);
            
            hpypz[i] = aida.histogram1D(mollerCollections[i]+"/"+"pypz", 60, -.005,.005);
            hpxpz[i] = aida.histogram1D(mollerCollections[i]+"/"+"pxpz", 60, .025,.035);

            pxpz_vs_diff[i] = aida.histogram2D(mollerCollections[i]+"/"+"pxpz vs diff", 50, .025, .035, 50, -.60, .60);
            pypz_vs_diff[i] = aida.histogram2D(mollerCollections[i]+"/"+"pypz vs diff", 50, -.005, .005, 50, -.60, .60);

            diff[i] = aida.histogram1D(mollerCollections[i]+"/"+"diff", 50, -.60, .60);

            sum[i] = aida.histogram1D(mollerCollections[i]+"/"+"sum", 50, .95, 1.15);

            pxpz_vs_sum[i] = aida.histogram2D(mollerCollections[i]+"/"+"pxpz vs sum", 50, .025, .035, 50, .95, 1.15);
            pypz_vs_sum[i] = aida.histogram2D(mollerCollections[i]+"/"+"pypz vs sum", 50, -.005, .005, 50, .95, 1.15);

            pxpz_vs_mass[i] = aida.histogram2D(mollerCollections[i]+"/"+"pxpz vs mass", 50, .025, .035, 50,.03, .037);
            pypz_vs_mass[i] = aida.histogram2D(mollerCollections[i]+"/"+"pypz vs mass", 50, -.005, .005, 50, .03, .037);

            vtx_xy[i] = aida.histogram2D(mollerCollections[i]+"/"+"vtx xy", 50, -5, 5, 50, -5, 5);
            
            mass[i] = aida.histogram1D(mollerCollections[i]+"/"+"mass", 300, 0, .060);
            timediff[i] = aida.histogram1D(mollerCollections[i]+"/"+"time diff", 60, -6, 6);
            
            trkchi2[i] = aida.histogram1D(mollerCollections[i]+"/"+"trk chi2", 100, 0, 100);
            vtxchi2[i] = aida.histogram1D(mollerCollections[i]+"/"+"vtx chi2", 100, 0, 100);
            
            nPassCuts[i] = aida.histogram1D(mollerCollections[i]+"/"+"moller events pass cut per event", 20, 0, 20);
            meanClusterTime[i] = aida.histogram1D(mollerCollections[i]+"/"+"mean cluster time", 200, 0, 200);
            
            dphi[i] = aida.histogram1D(mollerCollections[i]+"/"+"dphi (degrees)", 90, -180, 180);
            theta_theta[i] = aida.histogram1D(mollerCollections[i]+"/"+"sin(theta1_2)sin(theta2_2)", 100, 0, .002);
            dphi_vs_dtheta[i] = aida.histogram2D(mollerCollections[i]+"/"+"dphi vs dtheta", 180, -180, 180, 100, 0, .05);
        }

        
    }
    
    
    double beamTiltX = .0295;
    double beamTiltY = -.0008;
    
    double[] getThetaAndPhi(Hep3Vector p){
    	double px = p.x(), py = p.y(), pz = p.z();
        
        double cx = cos(beamTiltX);
        double sy = sin(beamTiltY);
        double cy = cos(beamTiltY);
        double sx = sin(beamTiltX);
        
        double pxtilt = px*cx              -pz*sx;
        double pytilt = -py*sx*sy + py*cy  -pz*sy*cx;
        double pztilt = px*cy*sx  + py*sy  +pz*cy*cx;
        

        double theta = atan(hypot(pxtilt, pytilt)/pztilt);
        double phi = atan2(pytilt, pxtilt);
        return new double[]{theta, phi};
    }
}
