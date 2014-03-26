package org.hps.recon.tracking.kalman;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hps.recon.tracking.kalman.util.RKDebug;
import org.hps.recon.tracking.kalman.util.SurfaceCode;
import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfcyl.ThinCylMs;
import org.lcsim.recon.tracking.trffit.FullFitter;
import org.lcsim.recon.tracking.trffit.HTrack;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;
import org.lcsim.recon.tracking.trfzp.ThinZPlaneMs;
import org.lcsim.util.aida.AIDA;

/**
 *
 * Copied from org.lcsim.contrib.RobKutschke.TRF.trffit. Added some very naive
 * multiple scattering, which will need to be updated.
 *
 *
 * Full track fit using Kalman filter.  The propagator is specified
 * when the fitter is constructed.  The starting surface, vector and
 * error matrix are taken from the input track.  Errors should be
 * increased appropriately if the fitter is applied repeatedly to
 * a single track.
 *
 *@author $Author: mgraham $
 *@version $Id: FullFitKalman.java,v 1.6 2011/11/16 18:00:03 mgraham Exp $
 *
 * Date $Date: 2011/11/16 18:00:03 $
 *
 */
public class FullFitKalman extends FullFitter {

    boolean _DEBUG = true;
    private AIDA aida = AIDA.defaultInstance();
    // Flags to control: multiple scattering, energy loss and adding the hit.
    private boolean doMs = true;
    private boolean doEloss = true;
    private boolean doMeas = true;
    private double dedxscale = 1.;
    private double dedxsigma = 0.0;

    // static methods
    //
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public static String typeName() {
        return "FullFitKalman";
    }

    //
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public static String staticType() {
        return typeName();
    }
    // The propagator.
    private Propagator _pprop;
    // The add fitter.
    private AddFitKalman _addfit;
    int AddFitKalmanDebugLevel = 0;
    //

    /**
     *Construct an instance specifying a propagator.
     *
     * @param   prop The Propagator to be used during the fit.
     */
    public FullFitKalman(Propagator prop) {
        _pprop = prop;
        _addfit = new AddFitKalman();

//	try{
//	    ToyConfig config = ToyConfig.getInstance();
//	    AddFitKalmanDebugLevel = config.getInt( "AddFitKalmanDebugLevel",
//						    AddFitKalmanDebugLevel );
//	    dedxscale = config.getDouble("dEdXScale");
//	    dedxsigma = config.getDouble("dEdXSigma");
//
//
//	} catch (ToyConfigException e){
//            System.out.println (e.getMessage() );
//            System.out.println ("Stopping now." );
//            System.exit(-1);
//        }
//	System.out.println ("FullfitKalman dedxscale: " + dedxscale );

    }

    //
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public String type() {
        return staticType();
    }

    //
    /**
     *Return the propagator.
     *
     * @return The Propagator used in the fit.
     */
    public Propagator propagator() {
        return _pprop;
    }

    //
    public void setDoMs(boolean b) {
        doMs = b;
    }

    public void setDoEloss(boolean b) {
        doEloss = b;
    }

    /**
     *Fit the specified track.
     *
     * @param   trh The HTrack to fit.
     * @return 0 if successful.
     */
    public int fit(HTrack trh) {
        // Copy the hits from the track.
        List hits = trh.hits();
        if (_DEBUG)
            System.out.println("FullFitKalman::Hits has " + hits.size() + " elements");
        // Delete the list of hits from the track.
        while (trh.hits().size() > 0)
            trh.dropHit();
//        System.out.println("Hits has "+hits.size()+" elements");

        // Set direction to be nearest.
        PropDir dir = PropDir.NEAREST;
//        PropDir dir = PropDir.FORWARD;
//        RKDebug.Instance().setPropDir(dir);

        // Loop over hits and fit.
        int icount = 0;
        for (Iterator ihit = hits.iterator(); ihit.hasNext();) {

            // Extract the next hit pointer.
            Hit hit = (Hit) ihit.next();
            if (_DEBUG) {
                System.out.println("*******   ADDING NEW HIT    *********** ");
                System.out.println("Hit " + icount + " is: \n");
                System.out.println("Before prop: " + trh.newTrack());
                System.out.println("Propogating to surface : " + hit.surface());
            }
            //            System.out.println("Before prop spacepoint: " + hit.surface().spacePoint(trh.newTrack().vector()));

            // propagate to the surface
            PropStat pstat = trh.propagate(_pprop, hit.surface(), dir);

            if (_DEBUG) {
                System.out.println("pstat = " + pstat);
                System.out.println("After prop: " + trh.newTrack());
            }
            if (!pstat.success())
                return -666;
//            System.out.println("trh= \n"+trh+", hit= \n"+hit);
//            System.out.println("_addfit= "+_addfit);
//            System.out.println("After prop spacepoint: " + hit.cluster().surface().spacePoint(trh.newTrack().vector()));
//            System.out.println("trh surface: " + trh.newTrack().surface().toString());
//            System.out.println("hit surface" + hit.surface().toString());

//             fit track
            if (_DEBUG)
                System.out.println("Predicted Hit Vector before addHit: " + hit.predictedVector());
            int fstat = _addfit.addHit(trh, hit);
            if (_DEBUG)
                System.out.println("Predicted Hit Vector after addHit: " + hit.predictedVector());
//
            if (_DEBUG) {
                System.out.println("Measured Hit Vector after addHit: " + hit.cluster());
                System.out.println("After addhit: " + fstat + " chi^2 = "
                        + trh.chisquared() + "\n" + trh.newTrack());
//            System.out.println("After addhit spacepoint: " + hit.cluster().surface().spacePoint(trh.newTrack().vector()));
//
            }

            // Multiple scattering--this is a silly way to do it..
            ThinXYPlaneMs interactor = new ThinXYPlaneMs(.02);

            if (_DEBUG)
                System.out.println("trh error pre MS:  " + trh.newTrack().error().toString());
//            ThinXYPlaneMs interactor = new ThinXYPlaneMs(.005);
            ETrack tre2 = trh.newTrack();
            interactor.interact(tre2);
            trh.setFit(tre2, trh.chisquared());
            if (_DEBUG)
                System.out.println("trh error pre MS:  " + trh.newTrack().error().toString());
            if (fstat > 0)
                return 10000 + 1000 * fstat + icount;


            icount++;

        }
        if (_DEBUG)
            System.out.println("fit completed");
        return 0;
    }

    public int fitForward(HTrack trh) {
        if (_DEBUG)
            System.out.println("fitting forward...");

        PropDir dir = PropDir.FORWARD;
//        RKDebug.Instance().setPropDir(dir);


        // Copy the hits from the track.
        List hits = trh.hits();

        // Delete the list of hits from the track.
        while (trh.hits().size() > 0)
            trh.dropHit();

        double sumde = 0.;

        // Loop over hits and fit.
        int icount = 0;
        for (Iterator ihit = hits.iterator(); ihit.hasNext();) {
            Surface s_save = trh.newTrack().surface().newPureSurface();
            ETrack e_save = trh.newTrack();

            // Extract the next hit pointer.
            Hit hit = (Hit) ihit.next();

            int from = (new SurfaceCode(s_save)).getCode();
            int to = (new SurfaceCode(hit.surface())).getCode();

            // Propagate to the next surface.
            PropStat pstat = trh.propagate(_pprop, hit.surface(), dir);
            if (!pstat.success()) {
                if (AddFitKalmanDebugLevel > 0) {
                    System.out.println("Error:        "
                            //                            + RKDebug.Instance().getTrack() + " "
                            //                            + RKDebug.Instance().getPropDir() + " "
                            + icount);
                    System.out.println("From surface 5: " + s_save);
                    System.out.println("To surface 5:   " + hit.surface());
                    System.out.println("Params: " + e_save.vector());
                }
//		aida.histogram1D("/Bugs/Fit/Failed Fwd prop from Surface",5,0,5).fill( from );
//		aida.histogram1D("/Bugs/Fit/Failed Fwd prop to Surface",5,0,5).fill( to  );
//		aida.cloud2D("/Bugs/Fit/Failed Fwd prop to vs from Surface").fill( from, to  );
                if (_DEBUG)
                    System.out.println("pstat not success :(");
                return icount + 1;
            }
//
            if (icount != 0) {
                int istat = interact(trh, hit, dir);
            }


            // Add the hit.
            int fstat = _addfit.addHit(trh, hit);
            if (_DEBUG)
                System.out.println("Hit added!");
            if (fstat > 0) {
                if (AddFitKalmanDebugLevel > 0) {

//                    System.out.println("Error:        "
//                            + RKDebug.Instance().getTrack() + " "
//                            + RKDebug.Instance().getPropDir() + " ");
                    System.out.println("From surface 4: " + s_save);
                    System.out.println("To surface 4:   " + hit.surface());
                }
//		aida.histogram1D("/Bugs/Fit/Failed Fwd addhit from Surface",5,5,5).fill( from );
//		aida.histogram1D("/Bugs/Fit/Failed Fwd addhit to Surface",5,0,5).fill( to  );
//		aida.cloud2D("/Bugs/Fit/Failed Fwd addhit to vs from Surface").fill( from, to  );
//
            }
            /*
            if (fstat > 0)
            return 10000 + 1000 * fstat + icount;

            VTUtil before = new VTUtil(trh.newTrack());
            int istat = interact(trh, hit, dir);
            VTUtil after = new VTUtil(trh.newTrack());

            double de = before.e() - after.e();
            sumde += de;

            SurfCylinder ss = (SurfCylinder) trh.newTrack().surface();
            if (_DEBUG)System.out.printf("Forw dedx: %10.4f %12.8f  %12.8f\n", ss.radius(), de, sumde);
             */

            // Multiple scattering--this is a silly way to do it..
            ThinXYPlaneMs interactor = new ThinXYPlaneMs(.02);

            if (_DEBUG)
                System.out.println("trh error pre MS:  " + trh.newTrack().error().toString());
//            ThinXYPlaneMs interactor = new ThinXYPlaneMs(.005);
            ETrack tre2 = trh.newTrack();
            interactor.interact(tre2);
            trh.setFit(tre2, trh.chisquared());
            if (_DEBUG)
                System.out.println("trh error pre MS:  " + trh.newTrack().error().toString());
            if (fstat > 0)
                return 10000 + 1000 * fstat + icount;


            ++icount;
        }


        //System.out.println ("Forward fit sumde: " + sumde );
//	aida.cloud1D("Forward dedx check:").fill(sumde-RKDebug.Instance().getDeGen());
        if (_DEBUG)
            System.out.println("Completed forward fit");
        return 0;

    }

    public int fitBackward(HTrack trh) {
        PropDir dir = PropDir.BACKWARD;
//	RKDebug.Instance().setPropDir(dir);

        //RKPrintSymMatrix psm = new RKPrintSymMatrix();

        // Copy the hits from the track.
        List hits = trh.hits();

        // Delete the list of hits from the track.
        while (trh.hits().size() > 0)
            trh.dropHit();

        double chold = 0.;

        double sumde = 0.;

//	RKZot zot = new RKZot(trh);
        boolean zottable = true;

        int nc = 0;
        int nz = 0;
        int nu = 0;
        String thishit;


        ListIterator bkglist = hits.listIterator(hits.size());
        // Loop over hits and fit.
        int icount = 0;
        _DEBUG = false;
        while (bkglist.hasPrevious()) {
            Hit hit = (Hit) bkglist.previous();

            if (_DEBUG) {
                System.out.println("*******   Fit Backward:  ADDING NEW HIT    *********** ");
                System.out.println("Fit Backward: Hit " + icount + " is: \n");
                System.out.println("Fit Backward: Before prop: " + trh.newTrack());
                System.out.println("Fit Backward: Propogating to surface : " + hit.surface());
            }
            //            System.out.println("Before prop spacepoint: " + hit.surface().spacePoint(trh.newTrack().vector()));
            dir = PropDir.BACKWARD;
            if (icount == 0)
                dir = PropDir.FORWARD;//if it's the first hit, propagate forward.
            // propagate to the surface
            PropStat pstat = trh.propagate(_pprop, hit.surface(), dir);

            if (_DEBUG) {
                System.out.println("Fit Backward: pstat = " + pstat);
                System.out.println("Fit Backward: After prop: " + trh.newTrack());
            }

            if (!pstat.success()) {
                if (_DEBUG) {
                    System.out.println("*****************************************************************************");
                    System.out.println("FullFitKalman::fitBackward     pstat =" + pstat.toString() + " for hit# " + icount);
                    System.out.println("FullFitKalman::fitBackward     hit   = " + hit.toString());
                    System.out.println("FullFitKalman::fitBackward     track   = " + trh.toString());
                    System.out.println("FullFitKalman::fitBackward     propagator   = " + _pprop.toString());
                }
                return -666;
            }
//            System.out.println("trh= \n"+trh+", hit= \n"+hit);
//            System.out.println("_addfit= "+_addfit);
//            System.out.println("After prop spacepoint: " + hit.cluster().surface().spacePoint(trh.newTrack().vector()));
//            System.out.println("trh surface: " + trh.newTrack().surface().toString());
//            System.out.println("hit surface" + hit.surface().toString());

//             fit track
            if (_DEBUG)
                System.out.println("Fit Backward: Predicted Hit Vector before addHit: " + hit.predictedVector());
            int fstat = _addfit.addHit(trh, hit);
            if (icount > 2 && trh.chisquared() < 0.1)
                _DEBUG = true;
            if (_DEBUG)
                System.out.println("Fit Backward: Predicted Hit Vector after addHit: " + hit.predictedVector());
//
            if (_DEBUG) {
                System.out.println("Fit Backward: Measured Hit Vector after addHit: " + hit.cluster());
                System.out.println("Fit Backward: After addhit: " + fstat + " chi^2 = "
                        + trh.chisquared() + "\n" + trh.newTrack());
//            System.out.println("After addhit spacepoint: " + hit.cluster().surface().spacePoint(trh.newTrack().vector()));
//
            }

            // Multiple scattering--this is a silly way to do it..
            if (isEven(icount)) {
                ThinXYPlaneMs interactor = new ThinXYPlaneMs(0.01);


                if (_DEBUG)
                    System.out.println("Fit Backward: trh error pre MS:  " + trh.newTrack().error().toString());
                ETrack tre2 = trh.newTrack();
                interactor.interact(tre2);
                trh.setFit(tre2, trh.chisquared());
                if (_DEBUG)
                    System.out.println("Fit Backward: trh error post MS:  " + trh.newTrack().error().toString());
                if (fstat > 0) {
                    System.out.println("FullFitKalman::fitBackward     fstat= = " + fstat);
                    return 10000 + 1000 * fstat + icount;
                }
            }
            icount++;
        }

        return 0;
    }///    }

    private int interact(HTrack trh, Hit hit, PropDir dir) {

        if (hit.surface().pureType().equals(SurfCylinder.staticType())) {

            SurfCylinder s = (SurfCylinder) hit.surface();
            double r = s.radius();
            if (doMs) {
                TrackError eold = trh.newTrack().error();

//		aida.histogram1D("/Bugs/Fit/Fit scat radius:",300,0.,150.).fill(r);

                double l_over_radl = 0.;
                if (r < 1.3) {
                    l_over_radl = 0.006136;
                } else if (r < 10.) {
                    l_over_radl = 0.000916;
                } else {
                    l_over_radl = 0.013747;
                }

                ThinCylMs scat = new ThinCylMs(l_over_radl * RKDebug.Instance().getMsFac());
                ETrack et = trh.newTrack();
                ETrack ets = new ETrack(et);
                double chnew = trh.chisquared();

                scat.interact(et);
                hit.update(et);
                trh.setFit(et, chnew);


                for (int i = 0; i < 5; ++i) {
                    double ex1 = et.error().get(i, i);
                    double ex2 = ets.error().get(i, i);
                    double sigsq = ex1 - ex2;
                    double pull = -9.;
                    double sig = -1.;
                    if (sigsq > 0.) {
                        sig = Math.sqrt(sigsq);
                        pull = (et.vector(i) - ets.vector(i)) / sig;
                    }
//		  aida.cloud1D("/Bugs/Fit/Forward Fit Delta param:"+i).fill(et.vector(i)-ets.vector(i));
//		  if ( sig > 0. ) aida.cloud1D("/Bugs/Fit/Forward Fit Delta error:"+i).fill(sig);
                }
//		  System.out.println( "Error after: " + trh.newTrack().error().minus(eold) );
//		  */
            } // end if MS enabled
//
//
        } // end CYlinder MS
//
        if (hit.surface().pureType().equals(SurfZPlane.staticType())) {

            SurfZPlane s = (SurfZPlane) hit.surface();
            double z = s.z();
            if (doMs) {
                TrackError eold = trh.newTrack().error();

////		aida.histogram1D("/Bugs/Fit/Fit scat z forward:",300,-150,150.).fill(z);

                double l_over_radl = (Math.abs(z) < 25) ? 0.000916 : 0.013747;
                ThinZPlaneMs scat = new ThinZPlaneMs(l_over_radl * RKDebug.Instance().getMsFac());
                ETrack et = trh.newTrack();
                ETrack ets = new ETrack(et);
                double chnew = trh.chisquared();

                scat.interact(et);
                hit.update(et);
                trh.setFit(et, chnew);

            } // end if MS enabled.

        } // end ZPlane MS

//    if ( hit.surface().pureType().equals(SurfXYPlane.staticType()) ){
//
//	    SurfXYPlane s = (SurfXYPlane) hit.surface();
//	    double z = s.z();
//	    if ( doMs ){
//		TrackError eold = trh.newTrack().error();
//
//////		aida.histogram1D("/Bugs/Fit/Fit scat z forward:",300,-150,150.).fill(z);
//
//		double l_over_radl = ( Math.abs(z)< 25) ? 0.000916 : 0.013747;
//		ThinZPlaneMs scat = new ThinZPlaneMs(l_over_radl*RKDebug.Instance().getMsFac());
//		ETrack et = trh.newTrack();
//				ETrack ets = new ETrack(et);
//		double chnew = trh.chisquared();
//
//		scat.interact(et);
//		hit.update(et);
//		trh.setFit(et,chnew);
//
//	    } // end if MS enabled.
//
//	} // end ZPlane MS

        // Successful return;
        return 0;
    }

    // There is no hit at this site so we only need to do the interaction, not update hits.
    private int interactonly(HTrack trh, double r, double l_over_radl) {

        ThinCylMs scat = new ThinCylMs(l_over_radl * RKDebug.Instance().getMsFac());
        ETrack et = trh.newTrack();
        double chnew = trh.chisquared();

        scat.interact(et);
        trh.setFit(et, chnew);

        // Successful return;
        return 0;
    }

//    /**
//     *output stream
//     *
//     * @return The String representation of this instance.
//     */
    public String toString() {
        return getClass().getName();
    }

    private boolean isEven(int num) {
        return num % 2 == 0;
    }
}
