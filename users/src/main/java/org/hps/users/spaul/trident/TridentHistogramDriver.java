package org.hps.users.spaul.trident;

import java.util.List;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IProfile1D;

import org.hps.recon.tracking.TrackType;
import org.hps.users.spaul.StyleUtil;
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
                pypz_vs_diff[i].fill(diff,pypz );
                pxpz_vs_diff[i].fill(diff, pxpz );



                pxpz_vs_sum[i].fill(sum, pxpz );
                pypz_vs_sum[i].fill(sum, pypz );

                pxpz_vs_mass[i].fill(mass, pxpz );
                pypz_vs_mass[i].fill(mass, pypz );
                timediff[i].fill(top.getClusters().get(0).getCalorimeterHits().get(0).getTime()
                        -bottom.getClusters().get(0).getCalorimeterHits().get(0).getTime());
                /*if(moreEnergetic.getMomentum().y() > 0)
            {
                pypz_tophighE.fill(pypz);
                pxpz_tophighE.fill(pxpz);
            }
            if(moreEnergetic.getMomentum().y() < 0)
            {
                pypz_bottomhighE.fill(pypz);
                pxpz_bottomhighE.fill(pxpz);
            }*/
            }
        }
    }

    static double BIG_NUMBER = Double.POSITIVE_INFINITY; 
    double _maxVtxChi2 = 10;
    double _maxTrkChi2 = 15;
    double _maxMass = 1.0;
    double _minMass = 0;
    double _minVtxPz = 2.0;
    double _maxVtxPz = 4.0;
    double _maxTrkPz = 0;
    double _maxPzDiff = 4.0;
    boolean passesCuts(Vertex vertex){
        ReconstructedParticle part = vertex.getAssociatedParticle();
        if(!TrackType.isGBL(part.getType()))
            return false;
        if(part.getMomentum().z() > _maxVtxPz || part.getMomentum().z() < _minVtxPz)
            return false;
        if(part.getMass() > _maxMass || part.getMass() < _minMass)
            return false;

        if(part.getParticles().get(0).getCharge()*part.getParticles().get(1).getCharge() != -1 )
            return false;

        if(vertex.getChi2() > _maxVtxChi2)
            return false;


        if(part.getParticles().get(0).getClusters().size() == 0)
            return false;
        if(part.getParticles().get(1).getClusters().size() == 0)
            return false;

        if(part.getParticles().get(0).getTracks().get(0).getChi2() > _maxTrkChi2)
            return false;
        if(part.getParticles().get(1).getTracks().get(0).getChi2() > _maxTrkChi2)
            return false;
        return true;
    }

    IHistogram1D hpypz[], hpxpz[], diff[], sum[], mass[];
    
    IHistogram1D chi2TrkPlus[];
    IHistogram1D chi2TrkMinus[];
    IHistogram1D chi2Vtx[];
    




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

    IHistogram1D vtx_x[], vtx_y[], timediff[];

    IProfile1D pxpz_vs_diff[], pypz_vs_diff[], pxpz_vs_sum[], pypz_vs_sum[],
    pxpz_vs_mass[], pypz_vs_mass[];


    //IHistogram1D pypz_tophighE, pxpz_tophighE;
    //IHistogram1D pypz_bottomhighE, pxpz_bottomhighE;
    @Override
    public void startOfData(){
        AIDA aida = AIDA.defaultInstance();
        hpypz = new IHistogram1D[3];
        hpxpz = new IHistogram1D[3];
        
        
        pxpz_vs_diff= new IProfile1D[3];
        pypz_vs_diff= new IProfile1D[3];

        diff= new IHistogram1D[3];

        sum= new IHistogram1D[3];

        pxpz_vs_sum= new IProfile1D[3];
        pypz_vs_sum= new IProfile1D[3];

        pxpz_vs_mass= new IProfile1D[3];
        pypz_vs_mass= new IProfile1D[3];

        //vtx_x= new IHistogram1D[3];
        //vtx_y= new IHistogram1D[3];
        mass= new IHistogram1D[3];
        timediff= new IHistogram1D[3];
        
        for(int i = 0; i< 3; i++){
            
            hpypz[i] = aida.histogram1D(tridentCollections[i]+"/"+"pypz", 60, -.005,.005);
            hpxpz[i] = aida.histogram1D(tridentCollections[i]+"/"+"pxpz", 60, .025,.035);


           

            pxpz_vs_diff[i] = aida.profile1D(tridentCollections[i]+"/"+"pxpz vs diff", 25, -.60, .60);
            pypz_vs_diff[i] = aida.profile1D(tridentCollections[i]+"/"+"pypz vs diff", 25, -.60, .60);

            diff[i] = aida.histogram1D(tridentCollections[i]+"/"+"diff", 50, -.60, .60);

            sum[i] = aida.histogram1D(tridentCollections[i]+"/"+"sum", 50, 1.0, 1.1);

            pxpz_vs_sum[i] = aida.profile1D(tridentCollections[i]+"/"+"pxpz vs sum", 25, 1.0, 1.1);
            pypz_vs_sum[i] = aida.profile1D(tridentCollections[i]+"/"+"pypz vs sum", 25, 1.0, 1.1);

            pxpz_vs_mass[i] = aida.profile1D(tridentCollections[i]+"/"+"pxpz vs mass", 25, .03, .037);
            pypz_vs_mass[i] = aida.profile1D(tridentCollections[i]+"/"+"pypz vs mass", 25, .03, .037);

            //vtx_x[i] = aida.histogram1D(tridentCollections[i]+"/"+"vtx x", 60, -1, 1);
            //vtx_y[i] = aida.histogram1D(tridentCollections[i]+"/"+"vtx y", 60, -1, 1);
            mass[i] = aida.histogram1D(tridentCollections[i]+"/"+"mass", 60, .030, .037);
            timediff[i] = aida.histogram1D(tridentCollections[i]+"/"+"time diff", 60, -6, 6);
        }

        
    }
}
