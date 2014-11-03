package org.hps.users.phansson;

import hep.aida.ICloud1D;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram;
import hep.aida.IPlotter;
import hep.aida.IProfile1D;
import hep.aida.ref.plotter.PlotterRegion;
import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.MultipleScatter;
import org.lcsim.fit.helicaltrack.TrackDirection;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

/**
 * Class to calculate and print the residuals and derivatives
 * of the alignment parameters...used as input for MillePede
 * Notation follows the MillePede manual:
 * http://www.desy.de/~blobel/Mptwo.pdf
 *
 * the track is measured in the HelicalTrackFit frame
 * and residuals are in the tracking frame (x,y,z)
 *
 * ordering of track parameters is
 *    double d0 = _trk.dca();
 *    double z0 = _trk.z0();
 *    double slope = _trk.slope();
 *    double phi0 = _trk.phi0();
 *    double R = _trk.R();
 *
 * @author mgraham
 */
public final class ModuleMPAlignmentInput extends MPAlignmentInputCalculator {

    private double[] _resid = new double[3];
    private double[] _error = new double[3];
    
    
    //private AIDAFrame plotterFrame;
    //private AIDAFrame plotterFrameSummary;
    private IDataPointSet dps_t;
    private IDataPointSet dps_b;
    private IDataPointSet dps_pull_t;
    private IDataPointSet dps_pull_b;
    private IPlotter plotter_resuydiff_t;
    private IPlotter plotter_resuydiff_b;
    
    
    
   
    
    
    
    public ModuleMPAlignmentInput(String outfile,String type) {
        super(outfile,type);
        
        makeAlignmentPlots();

    }

 
    
    @Override
    public void PrintResidualsAndDerivatives(Track track, int itrack) {
        
        SeedTrack st = (SeedTrack) track;
        SeedCandidate seed = st.getSeedCandidate();
        Map<HelicalTrackHit, MultipleScatter> msmap = seed.getMSMap();
        _trk = seed.getHelix();
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        String half = hitsOnTrack.get(0).getPosition()[2]>0 ? "top" : "bottom";
        this.addMilleInputLine(String.format("TRACK %s (%d)\n",half,itrack));
        aida.cloud1D("Track Chi2 "+ half).fill(track.getChi2());
        aida.cloud1D("Track Chi2ndf "+ half).fill(track.getChi2()/track.getNDF());
        if(_DEBUG) System.out.printf("%s: track %d (chi2=%.2f ndf=%d) has %d hits\n",this.getClass().getSimpleName(),itrack,track.getChi2(),track.getNDF(),hitsOnTrack.size());
        for (TrackerHit hit : hitsOnTrack) {
            
            HelicalTrackHit htc = (HelicalTrackHit) hit;
            
            if(!(htc instanceof HelicalTrackCross)) {
                if(_DEBUG) System.out.println(this.getClass().getSimpleName() + ": this hit is not a cross");
                continue;
            }
            
            // Update the hit position to be sure it's the latest
            HelicalTrackCross cross = (HelicalTrackCross) htc;
            cross.setTrackDirection(_trk);
            

            CalculateResidual(htc);
            CalculateLocalDerivatives(htc);
            CalculateGlobalDerivatives(htc);
            PrintHitResiduals(htc);

            
        }
        
        if(itrack%50==0) this.updatePlots();
    }

    
   
    
    private void CalculateLocalDerivatives(HelicalTrackHit hit) {
        double xint = hit.x();
        BasicMatrix dfdqGlobalOld = _oldAlignUtils.calculateLocalHelixDerivatives(_trk, xint);
        BasicMatrix dfdqGlobal = _alignUtils.calculateLocalHelixDerivatives(_trk, xint);
        BasicMatrix dfdqGlobalNum = this._numDerivatives.calculateLocalHelixDerivatives(_trk, xint);
        _dfdq = dfdqGlobal;
        
        if (_DEBUG) {
            
            //get track parameters.
            double d0 = _trk.dca();
            double z0 = _trk.z0();
            double slope = _trk.slope();
            double phi0 = _trk.phi0();
            double R = _trk.R();
            double s = HelixUtils.PathToXPlane(_trk, xint, 0, 0).get(0);
            double phi = -s/R + phi0;
            double[] trackpars = {d0, z0, slope, phi0, R, s, xint};
            System.out.printf("%s: --- CalculateLocalDerivatives HelicalTrackHit Result --- \n",this.getClass().getSimpleName());
            System.out.printf("%s: Hit position: %s \n",this.getClass().getSimpleName(),hit.getCorrectedPosition().toString());
            System.out.printf("%s: %10s%10s%10s%10s%10s%10s%10s\n",this.getClass().getSimpleName(),"d0","z0","slope","phi0","R", "xint", "s");
            System.out.printf("%s: %10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f\n",this.getClass().getSimpleName(), d0, z0, slope, phi0, R,xint,s);
            System.out.printf("%s: Local derivatives:\n",this.getClass().getSimpleName());
            System.out.printf("%s\n",dfdqGlobal.toString());
            System.out.printf("%s: Numerical Local derivatives:\n",this.getClass().getSimpleName());
            System.out.printf("%s\n",dfdqGlobalNum.toString());
            System.out.printf("%s: OLD Local derivatives:\n",this.getClass().getSimpleName());
            System.out.printf("%s\n",dfdqGlobalOld.toString());
        }
    
    }
    
    
   
    
    
    private void CalculateGlobalDerivatives(HelicalTrackHit hit) {
        
        /*
         * Residual in tracking frame is defined as r = m_a-p
         * with m_a as the alignment corrected position (u_a,v_a,w_a) 
         * and p is the predicted hit position in the tracking frame
         * 
         * Calcualte the derivative of dr/da where
         * a=(delta_u,delta_v,delta_w,alpha,beta,gamma) 
         * are the alingment paramerers in the tracking frame
         * 
         * Factorize: dr/da=dr/dm_a*dm_a/da
         * 
         * Start with dr/dma
         */
        
        //Find the unit vector of the track direction
        TrackDirection trkdir = HelixUtils.CalculateTrackDirection(_trk, _trk.PathMap().get(hit));
        Hep3Vector t = trkdir.Direction();
        //Find the unit vector of the plane where the hit happened
        if(!(hit instanceof HelicalTrackCross) ) {
            throw new RuntimeException("Error: gl ders for HTH only works if they are crosses!");
        }
        
        
        HelicalTrackCross cross = (HelicalTrackCross) hit;
        HelicalTrackStrip strip = cross.getStrips().get(0);
        Hep3Vector n = strip.w();
        
        
       
        /*
         * use the measured hit position 
         */
        double umeas,vmeas,wmeas;
        umeas = hit.x();
        vmeas = hit.y(); //the un-measured (along the strip) is set to zero
        wmeas = hit.z(); // the hit is on the surface of the plane
        
        /*
         * Calculate the dma_da 
         */
        
        BasicMatrix dma_da = new BasicMatrix(3,6);
        //dma_ddeltau
        dma_da.setElement(0, 0, 1);
        dma_da.setElement(1, 0, 0);
        dma_da.setElement(2, 0, 0);
        //dma_ddeltav
        dma_da.setElement(0, 1, 0);
        dma_da.setElement(1, 1, 1);
        dma_da.setElement(2, 1, 0);
        //dma_ddeltau
        dma_da.setElement(0, 2, 0);
        dma_da.setElement(1, 2, 0);
        dma_da.setElement(2, 2, 1);
        //dma_dalpha
        dma_da.setElement(0, 3, 0);
        dma_da.setElement(1, 3, wmeas);
        dma_da.setElement(2, 3, -vmeas);
        //dma_dbeta
        dma_da.setElement(0, 4, -wmeas);
        dma_da.setElement(1, 4, 0);
        dma_da.setElement(2, 4, umeas);
        //dma_dgamma
        dma_da.setElement(0, 5, vmeas);
        dma_da.setElement(1, 5, -umeas);
        dma_da.setElement(2, 5, 0);
        
        
        
         /*
         * Calculate the dr_dma 
         * e_ij = delta(i,j) - t_i*t_j/(t*n)
         */
        BasicMatrix dr_dma = new BasicMatrix(3,3);
        double tn = VecOp.dot(t, n);
        double[] t_arr = t.v();
        double[] n_arr = n.v();
        for(int i=0;i<3;++i) {
            for(int j=0;j<3;++j) {
                double delta_ij = i==j ? 1 : 0;
                double elem = delta_ij - t_arr[i]*n_arr[j]/tn;
                dr_dma.setElement(i, j, elem);
            }
        }
        
        
        
        /*
         * Calculate the dr/da=dr/dma*dma/da
         */
        
        BasicMatrix dr_da = (BasicMatrix) MatrixOp.mult(dr_dma, dma_da);
        
   
        int layer = hit.Layer();
        HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) strip.rawhits().get(0)).getDetectorElement();
        //===> int side = SvtUtils.getInstance().isTopLayer((SiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? 10000 : 20000;
        int side = sensor.isTopLayer() ? 10000 : 20000;

        
        if(_DEBUG) {
            double phi = -_trk.PathMap().get(hit)/_trk.R()+_trk.phi0();
            System.out.printf("%s: --- Calculate SIMPLE global derivatives for helical track hit ---\n",this.getClass().getSimpleName());
            System.out.printf("%s: Side %d, layer %d, hit position %s\n",this.getClass().getSimpleName(),side,layer,hit.getCorrectedPosition().toString());
            System.out.printf("%s: %10s%10s%10s%10s%10s%10s%10s%10s\n",this.getClass().getSimpleName(),"d0","z0","slope","phi0","R","phi","xint","s");
            System.out.printf("%s: %10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f\n",this.getClass().getSimpleName(), _trk.dca(), _trk.z0(), _trk.slope(), _trk.phi0(), _trk.R(),phi,hit.x(),_trk.PathMap().get(hit));
            System.out.printf("%s: Track direction t = %s\n",this.getClass().getSimpleName(),t.toString());
            System.out.printf("%s: Plane normal    n = %s\n",this.getClass().getSimpleName(),n.toString());
            System.out.printf("%s: m=(umeas,vmeas,wmeas)=(%.3f,%.3f,%.3f)\n",this.getClass().getSimpleName(),umeas,vmeas,wmeas);
            System.out.printf("dma_da=\n%s\ndr_dma=\n%s\ndr_da=\n%s\n",dma_da.toString(),dr_dma.toString(),dr_da.toString());
            
        }
   
        
        
        
        /*
         * Prepare the derivatives for output
         */
        
        
        
        _glp.clear();
        
        GlobalParameter gp_tu = new GlobalParameter("Translation in x",side,layer,1000,100,true);
        Hep3Vector mat = new BasicHep3Vector(dr_da.e(0, 0),dr_da.e(1, 0),dr_da.e(2, 0));
        gp_tu.setDfDp(mat);
        _glp.add(gp_tu);
        if (_DEBUG) {
            gp_tu.print();
        }

        GlobalParameter gp_tv = new GlobalParameter("Translation in y",side,layer,1000,200,true);
        mat = new BasicHep3Vector(dr_da.e(0, 1),dr_da.e(1, 1),dr_da.e(2, 1));
        gp_tv.setDfDp(mat);
        _glp.add(gp_tv);
        if (_DEBUG) {
            gp_tv.print();
        }

        
        GlobalParameter gp_tw = new GlobalParameter("Translation in z",side,layer,1000,300,true);
        mat = new BasicHep3Vector(dr_da.e(0, 2),dr_da.e(1, 2),dr_da.e(2, 2));
        gp_tw.setDfDp(mat);
        _glp.add(gp_tw);
        if (_DEBUG) {
            gp_tw.print();
        }
        
        GlobalParameter gp_ta = new GlobalParameter("Rotation alpha",side,layer,2000,100,true);
        mat = new BasicHep3Vector(dr_da.e(0, 3),dr_da.e(1, 3),dr_da.e(2, 3));
        gp_ta.setDfDp(mat);
        _glp.add(gp_ta);
        if (_DEBUG) {
            gp_ta.print();
        }

        GlobalParameter gp_tb = new GlobalParameter("Rotation beta",side,layer,2000,200,true);
        mat = new BasicHep3Vector(dr_da.e(0, 4),dr_da.e(1, 4),dr_da.e(2, 4));
        gp_tb.setDfDp(mat);
        _glp.add(gp_tb);
        if (_DEBUG) {
            gp_tb.print();
        }
        
        GlobalParameter gp_tg = new GlobalParameter("Rotation gamma",side,layer,2000,300,true);
        mat = new BasicHep3Vector(dr_da.e(0, 5),dr_da.e(1, 5),dr_da.e(2, 5));
        gp_tg.setDfDp(mat);
        _glp.add(gp_tg);
        if (_DEBUG) {
            gp_tg.print();
        }

        

        
        
    }

    
    
    
    
   
    
    
     private void CalculateResidual(HelicalTrackHit hit) {
        if(_DEBUG) System.out.printf("%s: --- Calculate Residual for HelicalTrackhit ---\n",this.getClass().getSimpleName());

         Map<String,Double> res_map = TrackUtils.calculateTrackHitResidual(hit, _trk, _includeMS);
         _resid[0] = 0.;
         _error[0] = 0.320/Math.sqrt(12);
         _resid[1] = res_map.get("resy");
         _error[1] = res_map.get("erry");
         _resid[2] = res_map.get("resz");
         _error[2] = res_map.get("errz");



        //Fill residuals vs distrance from center of plane in the v directions
        //aida.profile1D("res_u_vs_ydiff_layer_" + strip.layer() + "_" + side).fill(vdiffTrk.y(),_resid[0]);
        
        if (_DEBUG) {
            System.out.printf("%s: CalculateResidual HelicalTrackHit Result ----\n",this.getClass().getSimpleName());
            System.out.printf("%s: Hit positon: %s\n",this.getClass().getSimpleName(),hit.getCorrectedPosition().toString());
            Hep3Vector trkpos = HelixUtils.PointOnHelix(_trk, HelixUtils.PathToXPlane(_trk, hit.x(), 0, 0).get(0));
            System.out.printf("%s: Predicted position: %s\n",this.getClass().getSimpleName(),trkpos.toString());
            System.out.printf("%s: drphi=%,4f dz=%.4f MS: drdphi=%.4f, dz=%.4f\n",this.getClass().getSimpleName(),hit.drphi(),hit.dr()*Math.abs(_trk.slope()),_trk.ScatterMap().get(hit).drphi(),_trk.ScatterMap().get(hit).dz());
            System.out.printf("%s: %12s %12s %12s\n","resx","resy","resz");
            System.out.printf("%s: %5.5f+-%5.5f %5.5f+-%5.5f %5.5f+-%5.5f\n",_resid[0],_error[0],_resid[1],_error[1],_resid[2],_error[2]);
        }

    }
     
    
    
    
  

   

     private void PrintHitResiduals(HelicalTrackHit hit) {
        HelicalTrackStrip strip = ((HelicalTrackCross) hit).getStrips().get(0);
        HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) strip.rawhits().get(0)).getDetectorElement();
        //===> String side = SvtUtils.getInstance().isTopLayer((SiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? "top" : "bottom";        
        String side = sensor.isTopLayer() ? "top" : "bottom";        
        if (_DEBUG) {
            System.out.printf("%s: --- Alignment Results for this helical track hit ---\n",this.getClass().getSimpleName());
            
            //===> String sensor_type = SvtUtils.getInstance().isAxial((SiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? "axial" : "stereo";
            String sensor_type = sensor.isAxial() ? "axial" : "stereo";
            System.out.printf("%s: Layer %d in %s at position %s\n",this.getClass().getSimpleName(), hit.Layer(),side, hit.getCorrectedPosition().toString());
            System.out.printf("%s: Residuals (x,y,z) : %5.5e %5.5e %5.5e\n",this.getClass().getSimpleName(), _resid[0], _resid[1], _resid[2]);
            System.out.printf("%s: Errors (x,y,z)    : %5.5e %5.5e %5.5e\n",this.getClass().getSimpleName(), _error[0], _error[1], _error[2]);
            String[] q = {"d0", "z0", "slope", "phi0", "R"};
            System.out.printf("%s: track parameter derivatives:\n",this.getClass().getSimpleName());
            for (int i = 0; i < _nTrackParameters; i++) {
                System.out.printf("%s: %s     %5.5e %5.5e %5.5e\n",this.getClass().getSimpleName(), q[i], _dfdq.e(0, i), _dfdq.e(1, i), _dfdq.e(2, i));
            }
            //String[] p = {"u-displacement"};
            System.out.printf("%s: Global parameter derivatives\n",this.getClass().getSimpleName());
            for (GlobalParameter gl : _glp) {
                System.out.printf("%s: %s  %8.3e %5.8e %8.3e   %5d  \"%10s\"\n",this.getClass().getSimpleName(), "", gl.dfdp(0), gl.dfdp(1), gl.dfdp(2), gl.getLabel(),gl.getName());
            }
            System.out.printf("%s: --- END Alignment Results for this helical track hit ---\n",this.getClass().getSimpleName());
        }
        this.addMilleInputLine(String.format("%d\n", hit.Layer()));
        // Loop over the three directions u,v,w and print residuals and derivatives
        
        List<String> direction = new ArrayList<String>(Arrays.asList("x","y","z"));
        
        int s = "bottom".equals(side) ? 1 : 0;
        for(int j=0;j<direction.size();++j){
           
            
            
            //if(!isAllowedResidual(s,strip.layer(),j,_resid[j])) {
            //    if(_DEBUG) System.out.println("Layer " + strip.layer() + " in " + d[j] + " res " + _resid[j] + " +- " + _error[j] + " was outside allowed range");
            //    continue;
            //}
////            if(Math.abs(_resid[j]/_error[j])>3) {
////                if(_DEBUG) System.out.println("Layer " + strip.layer() + " in " + d[j] + " res " + _resid[j] + " +- " + _error[j] + " had too high pull");
////                continue;
////            }
            //This should really be x,y,z but the downstream parser likes u,v,w -> FIX THIS!
            String d = direction.get(j).equals("x") ? "u" : direction.get(j).equals("y") ? "v" : "w";
            this.addMilleInputLine(String.format("res_%s %5.5e %5.5e \n", d, _resid[j], _error[j])); 
            for (int i = 0; i < _nTrackParameters; i++) {
                this.addMilleInputLine(String.format("lc_%s %5.5e \n", d, _dfdq.e(j, i)));
            }
            for (GlobalParameter gl: _glp) {
                if(gl.active()){
                    this.addMilleInputLine(String.format("gl_%s %5.5e %5d\n", d, gl.dfdp(j), gl.getLabel()));
                    
                    //Cross check that side is correct
                    int lbl = gl.getLabel();
                    Double df = Math.floor(lbl/10000);
                    int iside = (df.intValue() % 10) - 1;
                    //System.out.println("track is on " + s + " gl param is " + lbl + "(" + df + ","+iside+")"  );
                    if(iside!=s) {
                        System.out.println("WARNING track is on " + s + " while gl param is " + lbl + "(" + df + ")"  );
                        System.exit(1);
                    }
                    
                }
            }
            if( _resid[j] < 9999999 ) {
                if (_DEBUG) System.out.println(this.getClass().getSimpleName() + ": filling ures with " + _resid[j]);
                aida.histogram1D("res_"+direction.get(j)+"_layer" + strip.layer() + "_" + side).fill(_resid[j]);    
                aida.histogram1D("reserr_"+direction.get(j)+"_layer" + strip.layer() + "_" + side).fill(_error[j]);    
                aida.histogram1D("respull_"+direction.get(j)+"_layer" + strip.layer() + "_" + side).fill(_resid[j]/_error[j]);    
            
            
                aida.histogram2D("respull_slope_"+direction.get(j)+"_layer" + strip.layer() + "_" + side).fill(_trk.slope(),_resid[j]/_error[j]);    
            }
            
        }
    }
 

    

    
    @Override
    public void makeAlignmentPlots() {
    
        
        
        aida.tree().cd("/");
        //plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("Residuals");
        //plotterFrameSummary = new AIDAFrame();
        //plotterFrameSummary.setTitle("Summary");
              
        List<String> sides = new ArrayList<String>(Arrays.asList("top","bottom"));

        IPlotter plotter_chi2 = af.createPlotterFactory().create();
        plotter_chi2.createRegions(2,2);
        plotter_chi2.setTitle("Track Chi2");
        //plotterFrame.addPlotter(plotter_chi2);
        ICloud1D hchi2_top = aida.cloud1D("Track Chi2 top");
        ICloud1D hchi2_bot = aida.cloud1D("Track Chi2 bottom");
        ICloud1D hchi2ndf_top = aida.cloud1D("Track Chi2ndf top");
        ICloud1D hchi2ndf_bot = aida.cloud1D("Track Chi2ndf bottom");
        plotter_chi2.region(0).plot(hchi2_top);
        plotter_chi2.region(1).plot(hchi2_bot);
        plotter_chi2.region(2).plot(hchi2ndf_top);
        plotter_chi2.region(3).plot(hchi2ndf_bot);
        
        
        List<String> direction = new ArrayList<String>(Arrays.asList("x","y","z"));
        
        
        
        double xbins_u_res[][] = {
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01},
//            {-0.01,0.01}
                {-0.15,0.15},
                {-0.4,0.4},
                {-0.7,0.7},
                {-0.5,0.5},
                {-0.5,0.5},
                {-0.5,0.5},
                {-2,2},
                {-2,2},
                {-2,2},
                {-2,2}            
        };
        double xbins_u_reserr[][] = {
                {0,0.04},
                {0,0.04},
                {0,0.5},
                {0,0.5},
                {0,0.7},
                {0,0.7},
                {0,1.2},
                {0,1.2},
                {0,1.5},
                {0,1.5}
        };
        double xbins_u_respull[][] = {
                {-5,5},
                {-5,5},
                {-5,5},
                {-5,5},
                {-5,5},
                {-5,5},
                {-5,5},
                {-5,5},
                {-5,5},
                {-5,5}
        };
        
        for (int d=0;d<direction.size();++d) { 

            for (int s=0;s<2;++s) { 
        //    if(iSide==1) continue;
                IPlotter plotter_res = af.createPlotterFactory().create();
                plotter_res.createRegions(5,2,0);
                plotter_res.setTitle("res_" + direction.get(d) + " " + sides.get(s));
                IPlotter plotter_reserr = af.createPlotterFactory().create();
                plotter_reserr.createRegions(5,2,0);
                plotter_reserr.setTitle("reserr_" + direction.get(d) + " " + sides.get(s));
                IPlotter plotter_respull = af.createPlotterFactory().create();
                plotter_respull.createRegions(5,2,0);
                plotter_respull.setTitle("respull_" + direction.get(d) + " " + sides.get(s));
                IPlotter plotter_respull_slope = af.createPlotterFactory().create();
                plotter_respull_slope.createRegions(5,2,0);
                plotter_respull_slope.setTitle("respull_slope_" + direction.get(d) + " " + sides.get(s));

                for (int iLayer=1;iLayer<11;++iLayer) {
                    
                    IHistogram h = aida.histogram1D("res_"+direction.get(d)+"_layer" + (iLayer) + "_" + sides.get(s) , 50, xbins_u_res[iLayer-1][0], xbins_u_res[iLayer-1][1]);                        
                    IHistogram h1 = aida.histogram1D("reserr_"+direction.get(d)+"_layer" + (iLayer) + "_" + sides.get(s) , 50, xbins_u_reserr[iLayer-1][0], xbins_u_reserr[iLayer-1][1]);            
                    IHistogram h2 = aida.histogram1D("respull_"+direction.get(d)+"_layer" + (iLayer) + "_" + sides.get(s) , 50, xbins_u_respull[iLayer-1][0], xbins_u_respull[iLayer-1][1]);                        

                    IHistogram hslope;
                    if(sides.get(s)=="top") hslope = aida.histogram2D("respull_slope_"+direction.get(d)+"_layer" + (iLayer) + "_" + sides.get(s) ,50, 0 , 0.1, 50, xbins_u_respull[iLayer-1][0], xbins_u_respull[iLayer-1][1]);                        
                    else hslope = aida.histogram2D("respull_slope_"+direction.get(d)+"_layer" + (iLayer) + "_" + sides.get(s) ,50, -0.1 , 0, 50, xbins_u_respull[iLayer-1][0], xbins_u_respull[iLayer-1][1]);                        
                    
                        
                    int region = (iLayer-1);//*2+iSide;

                    plotter_res.region(region).plot(h);
                    plotter_reserr.region(region).plot(h1);
                    plotter_respull.region(region).plot(h2);
                    plotter_respull_slope.region(region).plot(hslope);

                    ((PlotterRegion) plotter_res.region(region)).getPlot().setAllowUserInteraction(true);
                    ((PlotterRegion) plotter_res.region(region)).getPlot().setAllowPopupMenus(true);
                    ((PlotterRegion) plotter_reserr.region(region)).getPlot().setAllowUserInteraction(true);
                    ((PlotterRegion) plotter_reserr.region(region)).getPlot().setAllowPopupMenus(true);
                    ((PlotterRegion) plotter_respull.region(region)).getPlot().setAllowUserInteraction(true);
                    ((PlotterRegion) plotter_respull.region(region)).getPlot().setAllowPopupMenus(true);
                    ((PlotterRegion) plotter_respull_slope.region(region)).getPlot().setAllowUserInteraction(true);
                    ((PlotterRegion) plotter_respull_slope.region(region)).getPlot().setAllowPopupMenus(true);
                    plotter_respull_slope.style().dataStyle().fillStyle().setParameter("colorMapScheme","rainbow");
                    ((PlotterRegion) plotter_respull_slope.region(region)).getPlot().setAllowPopupMenus(true);

                   
                } 

                //plotter_res.show();
                //plotterFrame.addPlotter(plotter_res);
                //plotterFrame.addPlotter(plotter_reserr);
                //plotterFrame.addPlotter(plotter_respull);
                //plotterFrame.addPlotter(plotter_respull_slope);
            }
        
            
        
           
        }
        
        
         
         
        IPlotter plotter_prf = af.createPlotterFactory().create();
        plotter_prf.createRegions(1,2,0);
        plotter_prf.setTitle("<Residuals>");
        IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(null);
        dps_t = dpsf.create("dps_t", "Mean of u residuals top",2);
        plotter_prf.region(0).plot(dps_t);
        dps_b = dpsf.create("dps_b", "Mean of u residuals bottom",2);
        plotter_prf.region(0).plot(dps_b);
        
        dps_pull_t = dpsf.create("dps_pull_t", "Mean of u pulls top",2);
        plotter_prf.region(1).plot(dps_pull_t);
        dps_pull_b = dpsf.create("dps_pull_b", "Mean of u pulls bottom",2);
        plotter_prf.region(1).plot(dps_pull_b);

        for(int region=0;region<2;++region) {
            ((PlotterRegion) plotter_prf.region(region)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_prf.region(region)).getPlot().setAllowPopupMenus(true);
        }
        
        //plotterFrameSummary.addPlotter(plotter_prf);
        
        
        plotter_resuydiff_t = af.createPlotterFactory().create();
        plotter_resuydiff_t.createRegions(5,2,0);
        plotter_resuydiff_t.setTitle("res u vs ydiff top");      
        plotter_resuydiff_b = af.createPlotterFactory().create();
        plotter_resuydiff_b.createRegions(5,2,0);
        plotter_resuydiff_b.setTitle("res u vs ydiff bot");      
        //plotter_resuydiff_t_b.createRegions(5,2,0);
        //plotter_resuydiff_b.setTitle("distr: res u vs ydiff");      
        for(int iLayer=1;iLayer<11;++iLayer) {
            IProfile1D prf_t = aida.profile1D("res_u_vs_ydiff_layer_"+iLayer+"_top",10,-50,50);
            IProfile1D prf_b = aida.profile1D("res_u_vs_ydiff_layer_"+iLayer+"_bottom",10,-50,50);          
            plotter_resuydiff_t.region(iLayer-1).plot(prf_t);
            plotter_resuydiff_b.region(iLayer-1).plot(prf_b);
            ((PlotterRegion) plotter_resuydiff_t.region(iLayer-1)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_resuydiff_t.region(iLayer-1)).getPlot().setAllowPopupMenus(true);
            ((PlotterRegion) plotter_resuydiff_b.region(iLayer-1)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_resuydiff_b.region(iLayer-1)).getPlot().setAllowPopupMenus(true);
        }
        
        //plotterFrameSummary.addPlotter(plotter_resuydiff_t);
        //plotterFrameSummary.addPlotter(plotter_resuydiff_b);
        
        
        //plotterFrame.pack();
        //plotterFrame.setVisible(!hideFrame);
        //plotterFrameSummary.pack();
        //plotterFrameSummary.setVisible(!hideFrame);
        
    
    }
    
    
    @Override
    public void updatePlots() {
        dps_t.clear();
        dps_b.clear();
        dps_pull_t.clear();
        dps_pull_b.clear();
        
        final int nLayers = 10;
        final List<String> direction = new ArrayList<String>(Arrays.asList("x","y","z"));
        for(int i=1;i<nLayers+1;++i) {
            
            for(String d : direction) {

                double mean = aida.histogram1D("res_"+d+"_layer"+i+"_top").mean();
                double stddev = aida.histogram1D("res_"+d+"_layer"+i+"_top").rms();
                double N =  aida.histogram1D("res_"+d+"_layer"+i+"_top").entries();
                double error = N >0 ? stddev/Math.sqrt(N) : 0; 
                dps_t.addPoint();
                dps_t.point(i-1).coordinate(1).setValue(mean);
                dps_t.point(i-1).coordinate(1).setErrorPlus(error);
                dps_t.point(i-1).coordinate(0).setValue(i);
                dps_t.point(i-1).coordinate(0).setErrorPlus(0);

                mean = aida.histogram1D("res_"+d+"_layer"+i+"_bottom").mean();
                stddev = aida.histogram1D("res_"+d+"_layer"+i+"_bottom").rms();
                N =  aida.histogram1D("res_"+d+"_layer"+i+"_bottom").entries();
                error = N >0 ? stddev/Math.sqrt(N) : 0; 
                dps_b.addPoint();
                dps_b.point(i-1).coordinate(1).setValue(mean);
                dps_b.point(i-1).coordinate(1).setErrorPlus(error);
                dps_b.point(i-1).coordinate(0).setValue(i);
                dps_b.point(i-1).coordinate(0).setErrorPlus(0);

                mean = aida.histogram1D("respull_"+d+"_layer"+i+"_top").mean();
                stddev = aida.histogram1D("respull_"+d+"_layer"+i+"_top").rms();
                N =  aida.histogram1D("respull_"+d+"_layer"+i+"_top").entries();
                error = N >0 ? stddev/Math.sqrt(N) : 0; 
                dps_pull_t.addPoint();
                dps_pull_t.point(i-1).coordinate(1).setValue(mean);
                dps_pull_t.point(i-1).coordinate(1).setErrorPlus(error);
                dps_pull_t.point(i-1).coordinate(0).setValue(i);
                dps_pull_t.point(i-1).coordinate(0).setErrorPlus(0);

                mean = aida.histogram1D("respull_"+d+"_layer"+i+"_bottom").mean();
                stddev = aida.histogram1D("respull_"+d+"_layer"+i+"_bottom").rms();
                N =  aida.histogram1D("respull_"+d+"_layer"+i+"_bottom").entries();
                error = N >0 ? stddev/Math.sqrt(N) : 0; 
                dps_pull_b.addPoint();
                dps_pull_b.point(i-1).coordinate(1).setValue(mean);
                dps_pull_b.point(i-1).coordinate(1).setErrorPlus(error);
                dps_pull_b.point(i-1).coordinate(0).setValue(i);
                dps_pull_b.point(i-1).coordinate(0).setErrorPlus(0);
                
            }
        }
        
         
         
    }

    
    
    

}
