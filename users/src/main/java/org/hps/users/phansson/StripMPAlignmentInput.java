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
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.SiSensor;
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
 * and residuals are in the sensor frame (u,v,w)
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
public class StripMPAlignmentInput extends MPAlignmentInputCalculator {

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
    
    
    
   
    
    
    
    public StripMPAlignmentInput(String outfile,String type) {
        super(outfile,type);
        
        makeAlignmentPlots();

    }

    @Override
    public void setResLimits(int l,int d, double low,double high) {
       
    }

    public void setResLimits(int s, int l,int d, double low,double high) {
       
    }

    
    
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

            // Get the MS errors
            double msdrphi = msmap.get(htc).drphi();
            double msdz = msmap.get(htc).dz();
            // Find the strip clusters for this 3D hit 
            List<HelicalTrackStrip> clusterlist = cross.getStrips();

            if(_DEBUG) {
                System.out.printf("%s: This hit has %d clusters msdrphi=%.4f msdz=%.4f\n",this.getClass().getSimpleName(),clusterlist.size(),msdrphi,msdz);
            }
            for (HelicalTrackStrip cl : clusterlist) {
                    CalculateResidual(cl, msdrphi, msdz);
                    CalculateLocalDerivatives(cl);
                    CalculateGlobalDerivatives(cl);
                    PrintStripResiduals(cl);

            }

        }
        
        if(itrack%50==0) this.updatePlots();
    }

    
    private void CalculateLocalDerivatives(HelicalTrackStrip strip) {
        double xint = strip.origin().x();
        BasicMatrix dfdqGlobalOld = _oldAlignUtils.calculateLocalHelixDerivatives(_trk, xint);
        BasicMatrix dfdqGlobal = _alignUtils.calculateLocalHelixDerivatives(_trk, xint);
        BasicMatrix dfdqGlobalNum = this._numDerivatives.calculateLocalHelixDerivatives(_trk, xint);
        Hep3Matrix trkToStrip = trackerHitUtil.getTrackToStripRotation(strip);
        _dfdq = (BasicMatrix) MatrixOp.mult(trkToStrip, dfdqGlobal);
        
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
            System.out.printf("%s: --- CalculateLocalDerivatives Result --- \n",this.getClass().getSimpleName());
            System.out.printf("%s: Strip Origin %s \n",this.getClass().getSimpleName(),strip.origin().toString());
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
    
   
    
    private void CalculateGlobalDerivatives(HelicalTrackStrip strip) {
        
        /*
         * Residual in local sensor frame is defined as r = m_a-p
         * with m_a as the alignment corrected position (u_a,v_a,w_a) 
         * and p is the predicted hit position in the local sensor frame
         * 
         * Calcualte the derivative of dr/da where
         * a=(delta_u,delta_v,delta_w,alpha,beta,gamma) 
         * are the alingment paramerers in the local sensor frame
         * 
         * Factorize: dr/da=dr/dm_a*dm_a/da
         * 
         * Start with dr/dma
         */
        
        //Find interception with the plane the hit belongs to i.e. the predicted hit position 
        Hep3Vector p = TrackUtils.getHelixPlaneIntercept(_trk, strip, Math.abs(this._bfield.z()));
        double pathLengthToInterception = HelixUtils.PathToXPlane(_trk, p.x(),0,0).get(0);
        //Find the unit vector of the track direction
        TrackDirection trkdir = HelixUtils.CalculateTrackDirection(_trk, pathLengthToInterception);
        Hep3Vector t_TRACK = trkdir.Direction();
        Hep3Vector n_TRACK = strip.w();
        Hep3Matrix T = trackerHitUtil.getTrackToStripRotation(strip);
        Hep3Vector t = VecOp.mult(T, t_TRACK);
        Hep3Vector n = VecOp.mult(T, n_TRACK);
        
       
        /*
         * Measured position, either
         * 1. use the measured hit position on the plane w.r.t. an origin that is centered on the sensor plane.
         * 2. use the predicted hit position in order to get a better measure of the unmeasured directions
         */
        double umeas,vmeas,wmeas;
        boolean useMeasuredHitPosition = false;
        if(useMeasuredHitPosition) {
            if(_DEBUG) System.out.printf("%s: using measured hit position as \"m\"\n",this.getClass().getSimpleName());
            umeas = strip.umeas();
            vmeas = 0.; //the un-measured (along the strip) is set to zero
            wmeas = 0.; // the hit is on the surface of the plane
        } else {
            if(_DEBUG) System.out.printf("%s: using predicted hit position at %s  as \"m\"\n",this.getClass().getSimpleName(),p.toString());
            Hep3Vector p_local = VecOp.sub(p,strip.origin()); //subtract the center of the sensor in tracking frame
            if(_DEBUG) System.out.printf("%s: vector between origin of sensor and predicted position in tracking frame: %s \n",this.getClass().getSimpleName(),p_local.toString());
            p_local = VecOp.mult(T, p_local); //rotate to local frame
            if(_DEBUG) System.out.printf("%s: predicted position in local frame: %s \n",this.getClass().getSimpleName(),p_local.toString());
            umeas = p_local.x();
            vmeas = p_local.y(); 
            wmeas = p_local.z();
            Hep3Vector res_tmp = new BasicHep3Vector(strip.umeas()-p_local.x(),0.-p_local.y(),0.-p_local.z());
            if(_DEBUG) System.out.printf("%s: cross check residuals %s\n",this.getClass().getSimpleName(),res_tmp.toString());
            
        }
        
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
        
   
        int layer = strip.layer();
        int side = SvtUtils.getInstance().isTopLayer((SiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? 10000 : 20000;

        
        if(_DEBUG) {
            double phi = -pathLengthToInterception/_trk.R()+_trk.phi0();
            System.out.printf("%s: --- Calculate SIMPLE global derivatives ---\n",this.getClass().getSimpleName());
            System.out.printf("%s: Side %d, layer %d, strip origin %s\n",this.getClass().getSimpleName(),side,layer,strip.origin().toString());
            System.out.printf("%s: %10s%10s%10s%10s%10s%10s%10s%10s%10s\n",this.getClass().getSimpleName(),"d0","z0","slope","phi0","R","xint","phi", "xint","s");
            System.out.printf("%s: %10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f\n",this.getClass().getSimpleName(), _trk.dca(), _trk.z0(), _trk.slope(), _trk.phi0(), _trk.R(),p.x(),phi,p.x(),pathLengthToInterception);
            System.out.printf("%s: Track direction t = %s\n",this.getClass().getSimpleName(),t.toString());
            System.out.printf("%s: Plane normal    n = %s\n",this.getClass().getSimpleName(),n.toString());
            System.out.printf("%s: m=(umeas,vmeas,wmeas)=(%.3f,%.3f,%.3f)\n",this.getClass().getSimpleName(),umeas,vmeas,wmeas);
            System.out.printf("dma_da=\n%s\ndr_dma=\n%s\ndr_da=\n%s\n",dma_da.toString(),dr_dma.toString(),dr_da.toString());
            
        }
   
        
        
        
        /*
         * Prepare the derivatives for output
         */
        
        
        
        _glp.clear();
        
        GlobalParameter gp_tu = new GlobalParameter("Translation in u",side,layer,1000,100,true);
        Hep3Vector mat = new BasicHep3Vector(dr_da.e(0, 0),dr_da.e(1, 0),dr_da.e(2, 0));
        gp_tu.setDfDp(mat);
        _glp.add(gp_tu);
        if (_DEBUG) {
            gp_tu.print();
        }

        GlobalParameter gp_tv = new GlobalParameter("Translation in v",side,layer,1000,200,true);
        mat = new BasicHep3Vector(dr_da.e(0, 1),dr_da.e(1, 1),dr_da.e(2, 1));
        gp_tv.setDfDp(mat);
        _glp.add(gp_tv);
        if (_DEBUG) {
            gp_tv.print();
        }

        
        GlobalParameter gp_tw = new GlobalParameter("Translation in w",side,layer,1000,300,true);
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

    
   
    
    
    
    private void CalculateResidual(HelicalTrackStrip strip, double msdrdphi, double msdz) {
        if(_DEBUG) System.out.printf("%s: --- CalculateResidual ---\n",this.getClass().getSimpleName());
        
        double bfield = Math.abs(this._bfield.z());

        Map<String,Double> res_local = TrackUtils.calculateLocalTrackHitResiduals(_trk, strip, msdrdphi, msdz, bfield);
        
        _resid[0] = res_local.get("ures");
        _resid[1] = res_local.get("vres");
        _resid[2] = res_local.get("wres");
        _error[0] = res_local.get("ureserr");
        _error[1] = res_local.get("vreserr");
        _error[2] = res_local.get("wreserr");
        
        double vdiffy = res_local.get("vdiffTrky");
        String side = SvtUtils.getInstance().isTopLayer((SiSensor)((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? "top" : "bottom";
        //Fill residuals vs distrance from center of plane in the v directions
        aida.profile1D("res_u_vs_ydiff_layer_" + strip.layer() + "_" + side).fill(vdiffy,_resid[0]);

        
        /*
        Hep3Vector u = strip.u();
        Hep3Vector v = strip.v();
        Hep3Vector w = strip.w();
        Hep3Vector eta = VecOp.cross(u,v);
        Hep3Vector corigin = strip.origin();
        String side = SvtUtils.getInstance().isTopLayer((SiSensor)((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? "top" : "bottom";

        
//        if(_DEBUG) {
//            System.out.printf("%s: Finding interception point for residual calculation (B=%s)\n",this.getClass().getSimpleName(),this._bfield.toString());
//            Hep3Vector trkpos_wrong= TrackUtils.helixPositionAtPlaneApprox(_trk, strip); 
//            System.out.printf("%s: using wrong method xint_wrong=%.3f s_wrong=%.3f -> trkpos_wrong=%s\n",this.getClass().getSimpleName(),trkpos_wrong.toString());
//        }
        //Find interception with plane that the strips belongs to
        Hep3Vector trkpos = TrackUtils.getHelixPlaneIntercept(_trk, strip, bfield);
        
        if(_DEBUG) {
            System.out.printf("%s: found interception point at %s \n",this.getClass().getSimpleName(),trkpos.toString());
        }
        

        if(Double.isNaN(trkpos.x()) || Double.isNaN(trkpos.y()) || Double.isNaN(trkpos.z())) {
            System.out.printf("%s: failed to get interception point (%s) \n",this.getClass().getSimpleName(),trkpos.toString());
            System.out.printf("%s: track params\n%s\n",this.getClass().getSimpleName(),_trk.toString());
            System.out.printf("%s: track pT=%.3f chi2=[%.3f][%.3f] \n",this.getClass().getSimpleName(),_trk.pT(bfield),_trk.chisq()[0],_trk.chisq()[1]);
            trkpos = TrackUtils.getHelixPlaneIntercept(_trk, strip, bfield);
            System.exit(1);
        }
        
        
        
        double xint = trkpos.x();
        double phi0 = _trk.phi0();
        double R = _trk.R();
        double s = HelixUtils.PathToXPlane(_trk, xint, 0, 0).get(0);
        double phi = -s/R + phi0;
        
        
        
        Hep3Vector mserr = new BasicHep3Vector(msdrdphi * Math.sin(phi), msdrdphi * Math.sin(phi), msdz);
        Hep3Vector vdiffTrk = VecOp.sub(trkpos, corigin);
        Hep3Matrix trkToStrip = this.trackerHitUtil.getTrackToStripRotation(strip);
        Hep3Vector vdiff = VecOp.mult(trkToStrip, vdiffTrk);
        
        
        
        double umc = vdiff.x();
        double vmc = vdiff.y();
        double wmc = vdiff.z();
        double umeas = strip.umeas();
        double uError = strip.du();
        double msuError = VecOp.dot(mserr, u);
        double vmeas = 0;
        double vError = (strip.vmax() - strip.vmin()) / Math.sqrt(12);
        double wmeas = 0;
        double wError = 10.0/Math.sqrt(12); //0.001;
        _resid[0] = umeas - umc;
        _error[0] = _includeMS ? Math.sqrt(uError * uError + msuError * msuError) : uError;
        _resid[1] = vmeas - vmc;
        _error[1] = vError;
        _resid[2] = wmeas - wmc;
        _error[2] = wError;

        if(res_local.get("ures")!=_resid[0] || res_local.get("vres")!=_resid[1] || res_local.get("wres")!=_resid[2] || 
                res_local.get("ureserr")!=_error[0] || res_local.get("vreserr")!=_error[1] || res_local.get("wreserr")!=_error[2] ) {
            System.out.printf("%s: resu is different? %f vs %f \n",res_local.get("ures"),_resid[0]);
            System.out.printf("%s: resv is different? %f vs %f \n",res_local.get("vres"),_resid[1]);
            System.out.printf("%s: resw is different? %f vs %f \n",res_local.get("wres"),_resid[2]);
            System.exit(1);
        }
        if (_DEBUG) {
            System.out.printf("%s: CalculateResidual Result ----\n",this.getClass().getSimpleName());
            System.out.printf("%s: Strip layer %d Origin: %s\n",this.getClass().getSimpleName(),strip.layer(),corigin.toString());
            System.out.printf("%s: Position on the track (tracking coordinates) at the strip: %s\n",this.getClass().getSimpleName(),trkpos.toString());
            System.out.printf("%s: vdiffTrk %s\n",this.getClass().getSimpleName(),vdiffTrk.toString());
            System.out.printf("%s: vdiff %s\n",this.getClass().getSimpleName(),vdiff.toString());
            System.out.printf("%s: u %s\n",this.getClass().getSimpleName(),u.toString());
            System.out.printf("%s: umeas = %.4f  umc = %.4f\n",this.getClass().getSimpleName(),umeas,umc);
            System.out.printf("%s: udiff = %.4f +/- %.4f  ( uError=%.4f , msuError=%.4f\n",this.getClass().getSimpleName(),_resid[0],_error[0],uError,msuError);
            System.out.printf("%s: MS: drdphi=%.4f, dz=%.4f\n",this.getClass().getSimpleName(),msdrdphi,msdz);
            System.out.printf("%s: MS: phi=%.4f => msvec=%s\n",this.getClass().getSimpleName(),phi,mserr.toString());
            System.out.printf("%s: MS: msuError = %.4f (msvec*u = %s * %s\n",this.getClass().getSimpleName(),msuError,mserr.toString(),u.toString());          
        }
        */

        

    }
    
    
  
    
    
   
    
    

    private void PrintStripResiduals(HelicalTrackStrip strip) {
        
        String side = SvtUtils.getInstance().isTopLayer((SiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? "top" : "bottom";        
        
        if (_DEBUG) {
            System.out.printf("%s: --- Alignment Results for this Strip ---\n",this.getClass().getSimpleName());            
            String sensor_type = SvtUtils.getInstance().isAxial((SiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? "axial" : "stereo";
            System.out.printf("%s: Strip layer %4d is %s in %s  at origin %s\n",this.getClass().getSimpleName(), strip.layer(),sensor_type, side, strip.origin().toString());
            System.out.printf("%s: u=%s v=%s w=%s\n",this.getClass().getSimpleName(), strip.u().toString(),strip.v().toString(),strip.w().toString());
            System.out.printf("%s: Residuals (u,v,w) : %5.5e %5.5e %5.5e\n",this.getClass().getSimpleName(), _resid[0], _resid[1], _resid[2]);
            System.out.printf("%s: Errors (u,v,w)    : %5.5e %5.5e %5.5e\n",this.getClass().getSimpleName(), _error[0], _error[1], _error[2]);
            String[] q = {"d0", "z0", "slope", "phi0", "R"};
            System.out.printf("%s: track parameter derivatives:\n",this.getClass().getSimpleName());
            for (int i = 0; i < _nTrackParameters; i++)
                System.out.printf("%s: %s     %5.5e %5.5e %5.5e\n",this.getClass().getSimpleName(), q[i], _dfdq.e(0, i), _dfdq.e(1, i), _dfdq.e(2, i));
            System.out.printf("%s: Global parameter derivatives\n",this.getClass().getSimpleName());
            for (GlobalParameter gl : _glp) System.out.printf("%s: %s  %8.3e %5.8e %8.3e   %5d  \"%10s\"\n",this.getClass().getSimpleName(), "", gl.dfdp(0), gl.dfdp(1), gl.dfdp(2), gl.getLabel(),gl.getName());
            System.out.printf("%s: --- END Alignment Results for this Strip ---\n",this.getClass().getSimpleName());
        }
        
        
        
        this.addMilleInputLine(String.format("%d\n", strip.layer()));
        
        
        List<String> direction = new ArrayList<String>(Arrays.asList("u")); //use only u direction!
        
        int s;
        for(int d=0;d<direction.size();++d){
            side = "bottom";
            s = 1;
            if( strip.origin().z()>0) {
                side = "top";
                s = 0;
            }   
            
            
            //if(!isAllowedResidual(s,strip.layer(),j,_residirection.get(d))) {
            //    if(_DEBUG) System.out.println("Layer " + strip.layer() + " in " + direction.get(d) + " res " + _residirection.get(d) + " +- " + _error[d] + " was outside allowed range");
            //    continue;
            //}

            this.addMilleInputLine(String.format("res_%s %5.5e %5.5e \n", direction.get(d), _resid[d], _error[d])); 
            for (int i = 0; i < _nTrackParameters; i++) {
                this.addMilleInputLine(String.format("lc_%s %5.5e \n", direction.get(d), _dfdq.e(d, i)));
            }
            for (GlobalParameter gl: _glp) {
                if(gl.active()){
                    this.addMilleInputLine(String.format("gl_%s %5.5e %5d\n", direction.get(d), gl.dfdp(d), gl.getLabel()));
                    //x-check that side is correct
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
            if (_DEBUG) System.out.println(this.getClass().getSimpleName() + ": filling ures with " + _resid[d]);
            aida.histogram1D("res_"+direction.get(d)+"_layer" + strip.layer() + "_" + side).fill(_resid[d]);    
            aida.histogram1D("reserr_"+direction.get(d)+"_layer" + strip.layer() + "_" + side).fill(_error[d]);    
            aida.histogram1D("respull_"+direction.get(d)+"_layer" + strip.layer() + "_" + side).fill(_resid[d]/_error[d]);    
            aida.histogram2D("respull_slope_"+direction.get(d)+"_layer" + strip.layer() + "_" + side).fill(_trk.slope(),_resid[d]/_error[d]);    

        }
    }

    

   

    
    @Override
    protected void makeAlignmentPlots() {
    
        
        
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
        
        
        List<String> direction = "MODULE".equals(this._type) ? new ArrayList<String>(Arrays.asList("x","y","z")) : new ArrayList<String>(Arrays.asList("u"));
        
        
        
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
        final List<String> direction = this._type.equals("MODULE") ? new ArrayList<String>(Arrays.asList("x","y","z")) : new ArrayList<String>(Arrays.asList("u"));
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

    
    
    
    
    private void CalculateGlobalDerivativesWRONG(HelicalTrackStrip strip) {
       
        //Find interception with plane that the strips belongs to
        Hep3Vector x_vec = TrackUtils.getHelixPlaneIntercept(_trk, strip, Math.abs(this._bfield.z()));
        
        if(Double.isNaN(x_vec.x())) {
            System.out.printf("%s: error this trkpos is wrong %s\n",this.getClass().getSimpleName(),x_vec.toString());
            System.out.printf("%s: origin %s trk \n%s\n",this.getClass().getSimpleName(),strip.origin(),_trk.toString());
            x_vec = TrackUtils.getHelixPlaneIntercept(_trk, strip, Math.abs(this._bfield.z()));
            System.exit(1);
        }
        double slope = _trk.slope();
        double xr = 0.0;
        double yr = 0.0;
        double d0 = _trk.dca();
        double phi0 = _trk.phi0();
        double R = _trk.R();
        double z0 = _trk.z0();
        double s = HelixUtils.PathToXPlane(_trk, x_vec.x(), 0, 0).get(0);
        double phi = -s/R + phi0;
        double umeas = strip.umeas();
        Hep3Vector corigin = strip.origin();
        double vmeas = corigin.y(); //THis is wrong
        int layer = strip.layer();
        int side = SvtUtils.getInstance().isTopLayer((SiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement()) ? 10000 : 20000;

        if(_DEBUG) {
            System.out.printf("%s: --- Calculate global derivatives ---\n",this.getClass().getSimpleName());
            System.out.printf("%s: Side %d, layer %d, strip origin %s\n",this.getClass().getSimpleName(),side,layer,corigin.toString());
            System.out.printf("%s: %10s%10s%10s%10s%10s%10s%10s%10s%10s\n",this.getClass().getSimpleName(),"d0","z0","slope","phi0","R","xint","phi", "xint","s");
            System.out.printf("%s: %10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f%10.4f\n",this.getClass().getSimpleName(), d0, z0, slope, phi0, R,x_vec.x(),phi,x_vec.x(),s);
        }
        
        //1st index = alignment parameter (only u so far)
        //2nd index = residual coordinate (on du so far)
        //Naming scheme:
        //[Top]:
        // 10000 = top
        // 20000 = bottom
        //[Type]: 
        // 1000 - translation
        // 2000 - rotation
        //[Direction] (tracker coord. frame)
        // 100 - x (beamline direction)
        // 200 - y (bend plane)
        // 300 - z (non-bend direction)
        // [Layer]
        // 1-10
        
             
       //Rotation matrix from the tracking fram to the sensor/strip frame
        Hep3Matrix T = trackerHitUtil.getTrackToStripRotation(strip);
        
      
        
        /*
         * Calculate jacobian da/db
         */
        
        BasicMatrix da_db = this._alignUtils.calculateJacobian(x_vec,T);
      
        
        if(_DEBUG) {
            this._alignUtils.printJacobianInfo(da_db);
        }
        
        
        
        /*
         * Invert the Jacobian
         */
        
        
        BasicMatrix db_da = (BasicMatrix) MatrixOp.inverse(da_db);
        

        
        if(_DEBUG) {
            System.out.printf("%s: Invert the Jacobian. We get da_db^-1=db_da: \n%s \n",this.getClass().getSimpleName(),db_da.toString());
            BasicMatrix prod = (BasicMatrix) MatrixOp.mult(db_da,da_db);
            System.out.printf("%s: Check the inversion i.e. db_da*da_db: \n%s \n",this.getClass().getSimpleName(),prod.toString());
        }


        /*
         * Calculate global derivative of the residual dz/db = dq_a^gl/db-dq_p^gl/db
         * dq_a^gl is the alignment corrected position in the global tracking frame
         * dq_p^gl_db is the predicted hit position (from the track model) in the global/tracking frame
         * 
         */

       

         //****************************************************************************
        // First term in dz/db 

        //3x6 matrix
        BasicMatrix dq_agl_db = _alignUtils.calculateGlobalHitPositionDers(x_vec);
       
        
        if(_DEBUG) {
            _alignUtils.printGlobalHitPositionDers(dq_agl_db);
        }
        
        
        
        /*
         * Second term in dz/db  
         * dq_p^gl_db is the predicted hit position (from the track model) in the global/tracking frame
         * 
         */

        //3x6 matrix
        BasicMatrix dq_pgl_db = _alignUtils.calculateGlobalPredictedHitPositionDers(_trk,x_vec);

        if(_DEBUG) {
            _alignUtils.printGlobalPredictedHitPositionDers(dq_pgl_db);
        
            System.out.printf("%s: Cross-check using numerical derivatives for dq_pgl/db (numder)\n",this.getClass().getSimpleName());
        
            BasicMatrix dq_pgl_db_numder = this._numDerivatives.calculateGlobalPredictedHitPositionDers(_trk,x_vec);
            _alignUtils.printGlobalPredictedHitPositionDers(dq_pgl_db_numder);
        
        }

        /*
         * Note that a shift in the position of the hit (q_agl) in in the 
         * y/z plane is equivalent of the shift of the position of the prediction hit q_pgl
         * in the same plane. Thus we set these terms of the derivative to zero
         * 
         */

        dq_pgl_db.setElement(0, 0, 0);
        dq_pgl_db.setElement(1, 1, 0);
        dq_pgl_db.setElement(2, 2, 0);
        
        
        /*
         * For the rotations in this global frame the rotation leads to a shift of the position 
         * of the predicted hit. Since angles are small this rotation is the same as the 
         * rotation of the hit position
         */
        
        dq_pgl_db.setElement(0, 3, 0);
        dq_pgl_db.setElement(1, 3, 0);
        dq_pgl_db.setElement(2, 3, 0);
        dq_pgl_db.setElement(0, 4, 0);
        dq_pgl_db.setElement(1, 4, 0);
        dq_pgl_db.setElement(2, 4, 0);
        dq_pgl_db.setElement(0, 5, 0);
        dq_pgl_db.setElement(1, 5, 0);
        dq_pgl_db.setElement(2, 5, 0);
        
        
         if(_DEBUG) {
            System.out.printf("%s: Fixing the predicted derivatives that are equivalent to the hit position derivatives. The result is below.\n",this.getClass().getSimpleName());
            _alignUtils.printGlobalPredictedHitPositionDers(dq_pgl_db);
        }
        
        /*
         * Putting them together i.e. subtract the predicted from the hit position derivative 
         */
               
        //3x6 matrix    
        BasicMatrix dz_db = (BasicMatrix) MatrixOp.sub(dq_agl_db, dq_pgl_db);

            
        if(_DEBUG) {
            _alignUtils.printGlobalResidualDers(dz_db);
        }
        
        /*
         * Convert the to the local frame using the Jacobian
         */
       
        //3x6 = 3x6*6x6 matrix
        BasicMatrix dz_da = (BasicMatrix) MatrixOp.mult(dz_db, db_da);
        
        if(_DEBUG) {   
            _alignUtils.printLocalResidualDers(dz_da);
        }
        

        
        if(_DEBUG) {   
            System.out.printf("%s: PELLE check manual one entry\n",this.getClass().getSimpleName());
            double dx_dx = dz_db.e(0, 0);
            double dx_dy = dz_db.e(0, 1);
            double dx_dz = dz_db.e(0, 2);
            double dx_da = dz_db.e(0, 3);
            double dx_db = dz_db.e(0, 4);
            double dx_dc = dz_db.e(0, 5);
            double dx_du = db_da.e(0, 0);
            double dy_du = db_da.e(1, 0);
            double dz_du = db_da.e(2, 0);
            double da_du = db_da.e(3, 0);
            double db_du = db_da.e(4, 0);
            double dc_du = db_da.e(5, 0);
            System.out.printf("%s: dx_dx*dx_du = %.3f ",this.getClass().getSimpleName(),dx_dx*dx_du);
            System.out.printf(" dx_dy*dy_du = %.3f  ",dx_dy*dy_du);
            System.out.printf(" dx_dz*dz_du = %.3f (=%.3f*%.3f)\n",dx_dz*dz_du,dx_dz,dz_du);
            System.out.printf("%s: dx_da*da_du = %.3f ",this.getClass().getSimpleName(),dx_da*da_du);
            System.out.printf(" dx_db*db_du = %.3f ",dx_db*db_du);
            System.out.printf(" dx_dc*dc_du = %.3f \n",dx_dc*dc_du);
            
            double du_du = dx_dx*dx_du + dx_dy*dy_du + dx_dz*dz_du + dx_da*da_du + dx_db*db_du + dx_dc*dc_du;
            System.out.printf("%s: du_du = %.3f comapred to %.3f \n",this.getClass().getSimpleName(),du_du,dz_da.e(0, 0));
        }

        
        
        
        if(1==1) return;

        
         //Flag to tell if this hit is affected by the given global parameter
        boolean useGL = false;
        
        //Clear the old parameter list
        _glp.clear();

      
         
        
        
        
        
        BasicMatrix dqp_da_TRACK = new BasicMatrix(3,1);
        
        
        //Put it into a matrix to be able to transform easily
        //BasicMatrix _dqp_da_TRACK = FillMatrix(dqp_da_TRACK, 3, 1);
        //Get transformation matrix from tracking frame to sensor frame where the residuals are calculated
        Hep3Matrix trkToStrip = this.trackerHitUtil.getTrackToStripRotation(strip);
        if(_DEBUG) {
            System.out.println("Final transformation from tracking frame to strip frame:\n" + trkToStrip.toString());
        }
        
        //Transform to sensor frame!
        BasicMatrix dqp_da = (BasicMatrix) MatrixOp.mult(trkToStrip, dqp_da_TRACK);
        //Add it to the global parameter object
        GlobalParameter gp_tx = new GlobalParameter("Translation in x",side,layer,1000,100,true);
        gp_tx.setDfDp(dqp_da);
        _glp.add(gp_tx);
        if (_DEBUG) {
            gp_tx.print();
            System.out.println("Track frame dqp_da: " + dqp_da_TRACK);
            //System.out.printf("dqp_da = %5.5f     %5.5f   %5.5f   GL%d  name: %s\n", gp.dqp_da(0), gp.dqp_da(1), gp.dqp_da(2), gp.getLabel(),gp.getName());
        }

        
        //****************************************************************************
        //Derivatives of the predicted hit position qp for a translation of in y
        double y = -(R-d0)*Math.cos(phi0) + _alignUtils.sign(R) *Math.sqrt(Math.pow(R, 2)-Math.pow(x_vec.x()-(R-d0)*Math.sin(phi0), 2));
        dqp_da_TRACK.setElement(0, 0, _alignUtils.dx_dy(y, d0, phi0, R, phi)); 
        dqp_da_TRACK.setElement(1, 0, _alignUtils.dy_dy()); 
        dqp_da_TRACK.setElement(2, 0, _alignUtils.dz_dy(y, d0, phi0, slope, R, phi)); 

        
        //Put it into a matrix to be able to transform easily
        //BasicMatrix _dqp_da_TRACK = FillMatrix(dqp_da_TRACK, 3, 1);
        //Transform derivatives to sensor frame!
        dqp_da = (BasicMatrix) MatrixOp.mult(trkToStrip, dqp_da_TRACK);
        //Add it to the global parameter object
        GlobalParameter gp_ty = new GlobalParameter("Translation in y",side,layer,1000,200,true);
        gp_ty.setDfDp(dqp_da);
        _glp.add(gp_ty);
        if (_DEBUG) {
            gp_ty.print();
            System.out.println("Track frame dqp_da: " + dqp_da_TRACK);
            //System.out.printf("dfdp = %5.5f     %5.5f   %5.5f   GL%d  name: %s\n", gp.dfdp(0), gp.dfdp(1), gp.dfdp(2), gp.getLabel(),gp.getName());
        }

        //****************************************************************************
        //Derivatives of the predicted hit position qp for a translation of in z
        
        dqp_da_TRACK.setElement(0, 0, _alignUtils.dx_dz(slope, R, phi));
        dqp_da_TRACK.setElement(1, 0, _alignUtils.dy_dz(slope, R, phi));
        dqp_da_TRACK.setElement(2, 0, _alignUtils.dz_dz());

        
        //Put it into a matrix to be able to transform easily
        //BasicMatrix _dqp_da_TRACK = FillMatrix(dqp_da_TRACK, 3, 1);
        //Transform derivatives to sensor frame!
        dqp_da = (BasicMatrix) MatrixOp.mult(trkToStrip, dqp_da_TRACK);
        //Add it to the global parameter object
        GlobalParameter gp_tz = new GlobalParameter("Translation in z",side,layer,1000,300,true);
        gp_tz.setDfDp(dqp_da);
        _glp.add(gp_tz);
        if (_DEBUG) {
            gp_tz.print();
            System.out.println("Track frame dqp_da: " + dqp_da_TRACK);
            //System.out.printf("dfdp = %5.5f     %5.5f   %5.5f   GL%d  name: %s\n", gp.dfdp(0), gp.dfdp(1), gp.dfdp(2), gp.getLabel(),gp.getName());
        }

        
   
        
        
        
        
        

    }

    
   
    

}
