package org.hps.users.spaul.moller;

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

public class MollerBeamtiltAnalysis extends Driver{
    @Override
    public void process(EventHeader event){
        List<Vertex> mollers = event.get(Vertex.class, "TargetConstrainedMollerVertices");
        for(Vertex v : mollers){
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

            hpypz.fill(pypz);
            hpxpz.fill(pxpz);


            double diff = top.getMomentum().z()-bottom.getMomentum().z();
            double sum = m.getMomentum().z();//top.getMomentum().z()+bottom.getMomentum().z();
            double mass = m.getMass();

            if(diff > -.05 && diff < .05){
                hpypz_mid.fill(pypz);
                hpxpz_mid.fill(pxpz);
            }

            if(diff > .2 && diff < .3){
                hpypz_topHighE.fill(pypz);
                hpxpz_topHighE.fill(pxpz);
            }

            if(diff > -.3 && diff < -.2){
                hpypz_botHighE.fill(pypz);
                hpxpz_botHighE.fill(pxpz);
            }


            this.diff.fill(diff);
            this.sum.fill(sum);
            this.mass.fill(mass);
            pypz_vs_diff.fill(diff,pypz );
            pxpz_vs_diff.fill(diff, pxpz );



            pxpz_vs_sum.fill(sum, pxpz );
            pypz_vs_sum.fill(sum, pypz );

            pxpz_vs_mass.fill(mass, pxpz );
            pypz_vs_mass.fill(mass, pypz );
            timediff.fill(top.getClusters().get(0).getCalorimeterHits().get(0).getTime()
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

    double _maxVtxChi2 = 15;
    double _maxTrkChi2 = 30;
    double _maxMass = .037;
    double _minMass = .030;
    double _minPz = 1.0;
    double _maxPz = 1.1;
    boolean passesCuts(Vertex vertex){
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
        return true;
    }

    IHistogram1D hpypz, hpxpz, diff, sum, mass,
    hpypz_topHighE, hpxpz_topHighE,
    hpypz_botHighE, hpxpz_botHighE,
    hpypz_mid, hpxpz_mid;

    boolean display = false;




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


    public boolean getDisplay() {
        return display;
    }


    public void setDisplay(boolean display) {
        this.display = display;
    }

    IHistogram1D vtx_x, vtx_y, timediff;

    IProfile1D pxpz_vs_diff, pypz_vs_diff, pxpz_vs_sum, pypz_vs_sum,
    pxpz_vs_mass, pypz_vs_mass;


    //IHistogram1D pypz_tophighE, pxpz_tophighE;
    //IHistogram1D pypz_bottomhighE, pxpz_bottomhighE;
    @Override
    public void startOfData(){
        AIDA aida = AIDA.defaultInstance();
        hpypz = aida.histogram1D("pypz", 60, -.005,.005);
        hpxpz = aida.histogram1D("pxpz", 60, .025,.035);


        hpypz_mid = aida.histogram1D("pypz mid", 60, -.005,.005);
        hpxpz_mid = aida.histogram1D("pxpz mid", 60, .025,.035);

        hpypz_topHighE = aida.histogram1D("pypz top", 30, -.005,.005);
        hpxpz_topHighE = aida.histogram1D("pxpz top", 30, .025,.035);

        hpypz_botHighE = aida.histogram1D("pypz bot", 30, -.005,.005);
        hpxpz_botHighE = aida.histogram1D("pxpz bot", 30, .025,.035);


        pxpz_vs_diff = aida.profile1D("pxpz vs diff", 25, -.60, .60);
        pypz_vs_diff = aida.profile1D("pypz vs diff", 25, -.60, .60);

        diff = aida.histogram1D("diff", 50, -.60, .60);

        sum = aida.histogram1D("sum", 50, 1.0, 1.1);

        pxpz_vs_sum = aida.profile1D("pxpz vs sum", 25, 1.0, 1.1);
        pypz_vs_sum = aida.profile1D("pypz vs sum", 25, 1.0, 1.1);

        pxpz_vs_mass = aida.profile1D("pxpz vs mass", 25, .03, .037);
        pypz_vs_mass = aida.profile1D("pypz vs mass", 25, .03, .037);

        //vtx_x = aida.histogram1D("vtx x", 60, -1, 1);
        //vtx_y = aida.histogram1D("vtx y", 60, -1, 1);
        mass = aida.histogram1D("mass", 60, .030, .037);
        timediff = aida.histogram1D("time diff", 60, -6, 6);


        /*pypz_tophighE = aida.histogram1D("topHighE pypz", 60, -.005,.005);
        pxpz_tophighE = aida.histogram1D("topHighE pxpz", 60,  .025,.035);
        pypz_bottomhighE = aida.histogram1D("bottomHighE pypz", 60, -.005,.005);
        pxpz_bottomhighE = aida.histogram1D("bottomHighE pxpz", 60, .025,.035);*/
        if(display){
            IPlotter p = aida.analysisFactory().createPlotterFactory().create();
            StyleUtil.setSize(p, 1300, 900);
            //p.createRegions(3, 2);
            p.createRegions(4, 3);

            p.region(0).plot(hpypz);
            p.region(1).plot(hpxpz);
            p.region(2).plot(timediff);
            p.region(3).plot(pypz_vs_diff);
            p.region(4).plot(pxpz_vs_diff);
            p.region(5).plot(diff);
            p.region(6).plot(pypz_vs_sum);
            p.region(7).plot(pxpz_vs_sum);
            p.region(8).plot(sum);

            p.region(9).plot(pypz_vs_mass);
            p.region(10).plot(pxpz_vs_mass);
            p.region(11).plot(mass);
            /*p.region(2).plot(pypz_tophighE);
        p.region(3).plot(pxpz_tophighE);
        p.region(4).plot(pypz_bottomhighE);
        p.region(5).plot(pxpz_bottomhighE);*/
            StyleUtil.stylize(p.region(0),"py/pz", "py/pz", "#");
            StyleUtil.stylize(p.region(1),"px/pz", "px/pz", "#");
            StyleUtil.stylize(p.region(2),"time diff (t-b)", "diff (ns)", "#");
            StyleUtil.stylize(p.region(3),"py/pz vs diff", "diff (GeV)", "py/pz");
            StyleUtil.stylize(p.region(4),"px/pz vs diff", "diff (GeV)", "px/pz");
            StyleUtil.stylize(p.region(5),"diff", "diff (GeV)", "#");

            StyleUtil.stylize(p.region(6),"py/pz vs sum", "sum (GeV)", "py/pz");
            StyleUtil.stylize(p.region(7),"px/pz vs sum", "sum (GeV)", "px/pz");
            StyleUtil.stylize(p.region(8),"sum", "sum (GeV)", "#");

            StyleUtil.stylize(p.region(9),"py/pz vs mass", "mass (GeV)", "py/pz");
            StyleUtil.stylize(p.region(10),"px/pz vs mass", "mass (GeV)", "px/pz");
            StyleUtil.stylize(p.region(11),"mass", "mass (GeV)", "#");

            p.show();

            IPlotter p2 = aida.analysisFactory().createPlotterFactory().create();

            p2.createRegions(2, 1);


            p2.region(0).plot(hpypz_botHighE);
            p2.region(1).plot(hpxpz_botHighE);
            p2.region(0).plot(hpypz_mid);
            p2.region(1).plot(hpxpz_mid);
            p2.region(0).plot(hpypz_topHighE);
            p2.region(1).plot(hpxpz_topHighE);

            StyleUtil.stylize(p2.region(0),"py/pz", "py/pz", "#");
            StyleUtil.stylize(p2.region(1),"px/pz", "py/pz", "#");
            StyleUtil.noFillHistogramBars(p2.region(0));
            StyleUtil.noFillHistogramBars(p2.region(1));
            p2.show();
        }
    }
}
