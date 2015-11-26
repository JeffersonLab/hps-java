/**
 * 
 */
package org.hps.users.phansson;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hps.users.phansson.STUtils.STStereoTrack.VIEW;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HitUtils;
import org.lcsim.fit.line.SlopeInterceptLineFit;
import org.lcsim.fit.line.SlopeInterceptLineFitter;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType.CoordinateSystem;

/**
 * Class with static utilities for straight through tracking.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class STUtils {
    
    public final static Logger logger = Logger.getLogger(STUtils.class.getSimpleName());

    
    /**
     * Private constructor to avoid instantiation.
     */
    private STUtils() {
    }


    abstract static class STBaseTrack {
        protected List<STUtils.STTrackFit> fit = new ArrayList<STUtils.STTrackFit>();
        protected abstract double getIntercept();
        protected abstract double getSlope();
        protected abstract double getPath(double dz);
        public double predict(double zHit) {
            checkFit();
            return getFit().predict(zHit);
        }
        public STUtils.STTrackFit getFit() {
            return getFit(0);
        }
        public STUtils.STTrackFit getFit(int i) {
            if(i<fit.size()) 
                return fit.get(i);
            else 
                return null;
        }
        public void addFit(STUtils.STTrackFit fit) {
            this.fit.add(fit);
        }
        protected void checkFit() {
            if(getFit() == null) throw new RuntimeException("This track has no fit");
        }
        public abstract boolean isTop();
        public abstract String toString();
        public abstract List<double[]> getPointList();
        public void clearFit() {
            fit.clear();
        }
    
    
    }

    static class STTrack extends STBaseTrack {
        private List<SiTrackerHitStrip1D> hits = new ArrayList<SiTrackerHitStrip1D>();
        
        public STTrack() {}
        
        public void setHits(List<SiTrackerHitStrip1D> hits) {
           this.hits = hits;
        }
    
        public List<SiTrackerHitStrip1D> getHits() {
            return hits;
        }
    
        public double getIntercept() {
           checkFit();
           return getFit().intercept();
        }
    
        public double getSlope() {
            checkFit();
            return getFit().slope();
        }
        
        protected double getPath(double dz) {
            double slope = getSlope();
            double dy = slope*dz;
            return Math.sqrt( dz*dz + dy*dy );
        }
    
        public boolean isTop() {
            if(hits.size()==0) throw new RuntimeException("need hits to determine half.");
            return ((HpsSiSensor) hits.get(0).getRawHits().get(0).getDetectorElement()).isTopLayer();
        }
        
        public List<double[]> getPointList() {
            List<double[]> l = new ArrayList<double[]>();
            for(SiTrackerHitStrip1D hit : hits) {
                l.add( new double[]{hit.getPositionAsVector().z(), hit.getPositionAsVector().y()});
            }
            return l;
        }
    
        public String toString() {
            String s = "STTrack: \n";
            s+= String.valueOf(hits.size()) + " hits:\n";
            for(SiTrackerHitStrip1D hit : hits) {
                s += hit.getPositionAsVector().toString() + "(" + hit.getRawHits().get(0).getDetectorElement().getName() + ")\n";
            }
            s += fit.toString();
            return s;
        }
        
    }

    
    
    static class STStereoTrack {
        protected static enum VIEW { YZ,XZ };
        private List<STUtils.StereoPair> hits = new ArrayList<STUtils.StereoPair>();
        protected STTrackFit fit[] = {null,null};
        
        public STStereoTrack() {
        }
        
        void checkView(VIEW view) {
            if(!view.equals(VIEW.YZ) && !view.equals(VIEW.XZ)) throw new RuntimeException("This view is not valid.");
        }
        
        public double[] predict(double z) {
            checkFit(VIEW.XZ);
            checkFit(VIEW.YZ);
            double p[] = new double[2];
            double s = getSignedPathLength(this, z, VIEW.YZ);
            p[VIEW.YZ.ordinal()] = predict(z, VIEW.YZ);
            p[VIEW.XZ.ordinal()] = predict(s, VIEW.XZ);
            return p;
        }

        private double predict(double z, VIEW view) {
            checkView(view);
            return getFit(view).predict(z);
        }

        public STTrackFit getFit(VIEW view) {
            return fit[view.ordinal()];
        }
        
        public void setFit(STTrackFit f, VIEW view) {
            fit[view.ordinal()] = f;
        }
        
        protected void checkFit(VIEW view) {
            if(getFit(view) == null) throw new RuntimeException("This track has no fit");
        }

        public void clearFit() {
            clearFit(VIEW.XZ);
            clearFit(VIEW.YZ);
        }

        public void clearFit(VIEW view) {
            fit[view.ordinal()] = null;
        }
    
        /**
         * Track direction. Assumes track moves in positive z.
         * @return track direction.
         */
        public Hep3Vector getDirection() {
            double dxdz = getFit(VIEW.XZ).slope();
            double dydz = getFit(VIEW.YZ).slope();
            double dzdz = 1- Math.sqrt( dxdz*dxdz + dydz*dydz);
            return new BasicHep3Vector(dxdz, dydz, dzdz);
        }
    
        public List<StereoPair> getHits() {
            return hits;
        }

        protected double[] getIntercept() {
            double[] p = new double[2];
            p[VIEW.XZ.ordinal()] = getIntercept(VIEW.XZ);
            p[VIEW.YZ.ordinal()] = getIntercept(VIEW.YZ);
            return p;
        }
    
        private double getIntercept(VIEW view) {
            checkView(view);
            return getFit(view).intercept();
        }

        protected double[] getSlope() {
            double[] p = new double[2];
            p[VIEW.XZ.ordinal()] = getSlope(VIEW.XZ);
            p[VIEW.YZ.ordinal()] = getSlope(VIEW.YZ);
            return p;
        }
    
        private double getSlope(VIEW view) {
            checkView(view);
            return getFit(view).slope();
        }

        public void setHits(List<STUtils.StereoPair> seedHits) {
            hits = seedHits;
        }
        
        public boolean isTop() {
            if(hits.size()==0) throw new RuntimeException("need hits to determine half.");
            return ((HpsSiSensor) hits.get(0).getAxial().getRawHits().get(0).getDetectorElement()).isTopLayer();
        }
    
        /**
         * Get list of points that would be used in a simple univariate regression. 
         * Note that one {@link VIEW} uses the path length.
         * @param view - {@link VIEW} to be obtained
         * @return {@link List} of points for this track. 
         */
        public List<double[]> getPointList(VIEW view) {
            checkView(view);
            List<double[]> l = new ArrayList<double[]>();
            for(StereoPair hit : hits) {
                Hep3Vector p = hit.getPosition();
                // get the path length
                // in one view this is simple the z-position.
                double s = view.compareTo(VIEW.YZ) == 0 ? p.z() : getSignedPathLength(this, p.z(), VIEW.YZ);
                double y = view.equals(VIEW.YZ) ? p.y() : p.x();
                l.add( new double[]{s, y} );
            }
            return l;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer("STStereoTrack:\n");
            sb.append(hits.size() + " stereo hits.\n");
            for(int i=0;i<2;++i) {
                VIEW v = VIEW.values()[i];
                sb.append("View " + v.name() + ": \n");
                for(double[] xy : getPointList(v))
                    sb.append(xy[0] + ", " + xy[1] + "\n");
                if(getFit(v)!=null) sb.append(getFit(v).toString());
            }
            return sb.toString();
        }


       
    }

    static class StereoPair {
        SiTrackerHitStrip1D axialCluster;
        SiTrackerHitStrip1D stereoCluster;
        Hep3Vector position = null;
        public StereoPair(SiTrackerHitStrip1D axial, SiTrackerHitStrip1D stereo, Hep3Vector origin) {
            this.axialCluster = axial;
            this.stereoCluster = stereo;
            this.position = STUtils.getStereoHitPositionFromReference(origin, getAxial(), getStereo());
        }
        public void updatePosition(Hep3Vector trackDirection) {
            position = STUtils.getPosition(trackDirection, axialCluster, stereoCluster);
            
        }
        public SiTrackerHitStrip1D getStereo() {
            return stereoCluster;
        }
        public SiTrackerHitStrip1D getAxial() {
           return axialCluster;
        }
        public Hep3Vector getPosition() {
            return position;
        }
        public static boolean passCuts(StereoPair pair) {
           //find intersection with sensor
            return true;
        }
        
    }

    static class STTrackFit extends SlopeInterceptLineFit {
        private String type;
        public STTrackFit(String type, SlopeInterceptLineFit fit) {
            super(fit.slope(), fit.intercept(), fit.slopeUncertainty(),fit.interceptUncertainty(),fit.covariance(),fit.chisquared(),fit.ndf());
            this.type = type;
        }
        public STTrackFit(String type, double slope, double intercept, double slopeUncertainty, double interceptUncertainty, double sigAB, double chisq, int ndf) {
            super(slope, intercept,slopeUncertainty,interceptUncertainty,sigAB,chisq,ndf);
            this.type = type;
        }
        public double predict(double zHit) {
            return intercept() + slope() * zHit;
        }
        public String getType() {
            return type;
        }
        public String toString() {
            String s = "STTrackFit " + getType() + ": " + super.toString();
            return s;
        }
    }

    static abstract class STTrackFitter {
        private STTrackFit fit = null;
        public STTrackFitter() {}
        abstract String getType();
        abstract public void fit(STTrack track);
        abstract public void fit(List<double[]> xyList);
        protected void setFit(STTrackFit fit) {
            this.fit = fit;
        }
        public STTrackFit getFit() { return fit; }
        void clear() {
            fit = null;
        }
        abstract public void setErrY(double err_y);
            
    }

    static class RegressionFit extends STTrackFitter {
        private static final String TYPE = "SimpleRegression";
        public void fit(List<double[]> xyList) {
            clear();
            SimpleRegression regression = new SimpleRegression();
            for(double[] hit : xyList) {
                regression.addData(hit[0], hit[1]);
            }
            setFit(new STTrackFit(TYPE,regression.getSlope(), regression.getIntercept(), regression.getSlopeStdErr(), regression.getInterceptStdErr(), regression.getR(), 0.0, (int) (regression.getN()-2)));
        }
        public void fit(STTrack track) {
            fit(track.getPointList());
        }
        String getType() {
            return TYPE;
        }
        public void setErrY(double err_y) {
        }
    }

    private static class LineFit extends STTrackFitter {
        private static final String TYPE = "LineFit";
        private double errY = 6e-3;
        String getType() {
            return TYPE;
        }
        public void setErrY(double err_y) {
            this.errY = err_y;
        }
        public void fit(STTrack track) {
            fit(track.getPointList());
        }
        @Override
        public void fit(List<double[]> xyList) {
            int n = xyList.size();
            double[] x = new double[n];
            double[] y = new double[n];
            double[] sigma_y = new double[n];
            for(int i=0; i<n; ++i ) {
                x[i] = xyList.get(i)[0];
                y[i] = xyList.get(i)[1];
                sigma_y[i] = errY;
            }            
            SlopeInterceptLineFitter fitter = new SlopeInterceptLineFitter();
            fitter.fit(x, y, sigma_y, n);
            setFit( new STTrackFit(TYPE, fitter.getFit()) );
        }
    
    }


    static Hep3Vector getOrigin(SiTrackerHitStrip1D stripCluster) {
        SiTrackerHitStrip1D local = stripCluster.getTransformedHit(CoordinateSystem.SENSOR);
        ITransform3D trans = local.getLocalToGlobal();
        return trans.transformed(StraightThroughAnalysisDriver.origo);
    }

    static Hep3Vector getNormal(SiTrackerHitStrip1D s2) {
        Hep3Vector u2 = s2.getMeasuredCoordinate();
        Hep3Vector v2 = s2.getUnmeasuredCoordinate();
        return  VecOp.cross(u2, v2);
    }

    /**
     * Basically an adaptation of what's in {@link HelicalTrackCross}.
     * @param origin
     * @param strip1
     * @param strip2
     * @return
     */
    static Hep3Vector getStereoHitPositionFromReference(Hep3Vector origin, SiTrackerHitStrip1D strip1, SiTrackerHitStrip1D strip2) {
        SiTrackerHitStrip1D s1 = strip1;
        SiTrackerHitStrip1D s2 = strip2;
        // sort in direction of track
        // TODO do I need to sort these?
        if(s1.getPositionAsVector().z() > s2.getPositionAsVector().z()) { s1 = strip2; s2 = strip1; }
    
        StraightThroughAnalysisDriver.logger.finest("Calculate stereo hit position assuming track from " + origin.toString());
        
        // Get origin of sensors
        Hep3Vector o1 = getOrigin(s1);
        Hep3Vector o2 = getOrigin(s2);
        
        // Get sensor orientation
        Hep3Vector u1 = s1.getMeasuredCoordinate();
        Hep3Vector v1 = s1.getUnmeasuredCoordinate();
        Hep3Vector w1 = getNormal(s1);
        Hep3Vector u2 = s2.getMeasuredCoordinate();
        Hep3Vector v2 = s2.getUnmeasuredCoordinate();
        Hep3Vector w2 = getNormal(s2);
    
        // update origin calculation with displaced origin
        o1 = VecOp.sub(o1, origin);
        o2 = VecOp.sub(o2, origin);
    
        // For this to work the normal needs to point along track i.e. along z for both sensors
        // so rotate coordinate system pi around u axis in this case
        if(VecOp.dot(w1, o1) < 0) {
           v1 = VecOp.mult(-1.0, v1);
           w1 = VecOp.cross(u1, v1);
        }
        if(VecOp.dot(w2, o2) < 0) {
            v2 = VecOp.mult(-1.0, v2);
            w2 = VecOp.cross(u2, v2);
         }
        
        
        StraightThroughAnalysisDriver.logger.finest("o1 " + o1.toString());
        StraightThroughAnalysisDriver.logger.finest("u1 " + u1.toString());
        StraightThroughAnalysisDriver.logger.finest("v1 " + v1.toString());
        StraightThroughAnalysisDriver.logger.finest("w1 " + w1.toString());
        StraightThroughAnalysisDriver.logger.finest("o2 " + o2.toString());
        StraightThroughAnalysisDriver.logger.finest("u2 " + u2.toString());
        StraightThroughAnalysisDriver.logger.finest("v2 " + v2.toString());
        StraightThroughAnalysisDriver.logger.finest("w2 " + w2.toString());
    
        
        // Get the measured strip position on each sensor (basically origin + mult(umeas,u))
        Hep3Vector p1 = s1.getPositionAsVector();
        Hep3Vector p2 = s2.getPositionAsVector();
        StraightThroughAnalysisDriver.logger.finest("p1 " + p1.toString());
        StraightThroughAnalysisDriver.logger.finest("p2 " + p2.toString());
    
        // sin(stereo angle)
        double sinAlpha = VecOp.dot(v1, u2); 
        StraightThroughAnalysisDriver.logger.finest("sinAlpha " + sinAlpha);
        
        // check
        if(Math.abs( VecOp.dot(o1, w1)) < 0.0001) throw new RuntimeException("o1 and w1 are orthogonal: o1 " + o1.toString() + " w1 " + w1.toString());
        
        // calculate effective distance along z
        double gamma = VecOp.dot(o2, w2) / VecOp.dot(o1, w1);
        StraightThroughAnalysisDriver.logger.finest("gamma " + gamma);
    
        // Get the vector between the two strip (center) positions
        Hep3Vector dp = VecOp.sub(p2, VecOp.mult(gamma, p1));
        StraightThroughAnalysisDriver.logger.finest("dp " + dp.toString());
        
        // Get the projection onto u2 and scale with stereo angle to get un-measured coordinate
        double p1_v = VecOp.dot(dp, u2) / (gamma * sinAlpha);
        StraightThroughAnalysisDriver.logger.finest("p1_v " + p1_v);
        
        // calculate the position of the hit on the strip
        Hep3Vector r1 = VecOp.add(p1, VecOp.mult(p1_v, v1));
        StraightThroughAnalysisDriver.logger.finest("r1 " + r1.toString());
        
        // Take the final position as the half-way between the sensors
        Hep3Vector p = VecOp.mult( 0.5 * ( 1 + gamma ), r1);
    
        StraightThroughAnalysisDriver.logger.finest("p " + p.toString());
    
        
        return p;
        
    }

    /**
     * Basically the same as what's in {@link HitUtils}.
     * @param origin
     * @param strip1
     * @param strip2
     * @return
     */
    static Hep3Vector getPosition(Hep3Vector t, SiTrackerHitStrip1D strip1, SiTrackerHitStrip1D strip2) {
        SiTrackerHitStrip1D s1 = strip1;
        SiTrackerHitStrip1D s2 = strip2;
        // sort in direction of track
        // TODO do I need to sort these?
        if(s1.getPositionAsVector().z() > s2.getPositionAsVector().z()) { s1 = strip2; s2 = strip1; }
    
        StraightThroughAnalysisDriver.logger.finest("Calculate stereo hit position assuming track direction " + t.toString());
        
        // Get origin of sensors
        Hep3Vector o1 = getOrigin(s1);
        Hep3Vector o2 = getOrigin(s2);
        
        // Get sensor orientation
        Hep3Vector u1 = s1.getMeasuredCoordinate();
        Hep3Vector v1 = s1.getUnmeasuredCoordinate();
        Hep3Vector w1 = getNormal(s1);
        Hep3Vector u2 = s2.getMeasuredCoordinate();
        Hep3Vector v2 = s2.getUnmeasuredCoordinate();
        Hep3Vector w2 = getNormal(s2);
    
        // For this to work the normal needs to point along track i.e. along z for both sensors
        // so rotate coordinate system pi around u axis in this case
        if(VecOp.dot(w1, o1) < 0) {
           v1 = VecOp.mult(-1.0, v1);
           w1 = VecOp.cross(u1, v1);
        }
        if(VecOp.dot(w2, o2) < 0) {
            v2 = VecOp.mult(-1.0, v2);
            w2 = VecOp.cross(u2, v2);
         }
        
        
        StraightThroughAnalysisDriver.logger.finest("o1 " + o1.toString());
        StraightThroughAnalysisDriver.logger.finest("u1 " + u1.toString());
        StraightThroughAnalysisDriver.logger.finest("v1 " + v1.toString());
        StraightThroughAnalysisDriver.logger.finest("w1 " + w1.toString());
        StraightThroughAnalysisDriver.logger.finest("o2 " + o2.toString());
        StraightThroughAnalysisDriver.logger.finest("u2 " + u2.toString());
        StraightThroughAnalysisDriver.logger.finest("v2 " + v2.toString());
        StraightThroughAnalysisDriver.logger.finest("w2 " + w2.toString());
    
        
        // Get the measured strip position on each sensor (basically origin + mult(umeas,u))
        Hep3Vector p1 = s1.getPositionAsVector();
        Hep3Vector p2 = s2.getPositionAsVector();
        StraightThroughAnalysisDriver.logger.finest("p1 " + p1.toString());
        StraightThroughAnalysisDriver.logger.finest("p2 " + p2.toString());
    
        // sin(stereo angle)
        double sinAlpha = VecOp.dot(v1, u2); 
        StraightThroughAnalysisDriver.logger.finest("sinAlpha " + sinAlpha);
        
        // check
        if(Math.abs( VecOp.dot(o1, w1)) < 0.0001) throw new RuntimeException("o1 and w1 are orthogonal: o1 " + o1.toString() + " w1 " + w1.toString());
        
        // calculate the distance between the sensors
        double gamma = VecOp.dot( w1, VecOp.sub(o2, o1)) / VecOp.dot(w1, t);
        StraightThroughAnalysisDriver.logger.finest("gamma " + gamma);
    
        // Get the vector along the track direction
        Hep3Vector tgamma = VecOp.mult(gamma, t);
        StraightThroughAnalysisDriver.logger.finest("tgamma " + tgamma.toString());
        
        // Get the vector taking you from the midpoint of strip1 in the direction of the track
        Hep3Vector p1Prime = VecOp.add(p1, tgamma);
        StraightThroughAnalysisDriver.logger.finest("p1Prime " + p1Prime.toString());
        
        // Get the vector between the predicted and measured center values on strip2
        Hep3Vector dp = VecOp.sub(p2, p1Prime);
        StraightThroughAnalysisDriver.logger.finest("dp " + dp.toString());
        
        // Get the projection onto u2 and scale with stereo angle to get un-measured coordinate
        double p1_v = VecOp.dot(dp, u2) / (sinAlpha);
        StraightThroughAnalysisDriver.logger.finest("p1_v " + p1_v + " (u2="+u2.toString()+")");
        
        // calculate the position of the hit on the strip
        Hep3Vector r1 = VecOp.add(p1, VecOp.mult(p1_v, v1));
        StraightThroughAnalysisDriver.logger.finest("r1 " + r1.toString() + "(v1="+v1.toString()+")");
        
        // Take the final position as the half-way between the sensors
        Hep3Vector p = VecOp.add(r1, VecOp.mult( 0.5 * gamma, t) );
    
        StraightThroughAnalysisDriver.logger.finest("p " + p.toString());
    
        
        return p;
        
    }

    /**
     * Fit the {@link STStereoTrack} track with the supplied {@link STTrackFitter} and add the fit to the track.
     * @param regressionFitter
     * @param track
     */
    public static void fit(STTrackFitter regressionFitter, STStereoTrack track) {
        track.clearFit();
        regressionFitter.fit(track.getPointList(STStereoTrack.VIEW.YZ));
        track.setFit(regressionFitter.getFit(), STStereoTrack.VIEW.YZ);

        regressionFitter.fit(track.getPointList(STStereoTrack.VIEW.XZ));
        track.setFit(regressionFitter.getFit(), STStereoTrack.VIEW.XZ);
        
    }
    
    public static double getSignedPathLength(STStereoTrack track, double z, STStereoTrack.VIEW view) {
        double slope = track.getSlope(view);
        double dy = slope*z;
        return z > 0 ? Math.sqrt( z*z + dy*dy ) : -1*Math.sqrt( z*z + dy*dy );
    }
    
    
    
    
    public static void printGBL(PrintWriter printWriter, EventHeader event, List<STStereoTrack> tracks) {
        printWriter.printf("New Event %d %f", event.getEventNumber(), 0.0);
        printWriter.println();
        for(int iTrack=0; iTrack<tracks.size(); ++iTrack) {
            
            STStereoTrack track = tracks.get(iTrack);
            
            printWriter.printf("New Track %d", iTrack);
            printWriter.println();
            
            double[] perPars = getPerPars(track);
            printWriter.printf("Track perPar (x0 y0 dxdz dydz) %.12f %.12f %.12f %.12f", perPars[0],perPars[1],perPars[2],perPars[3]);
            printWriter.println();
            
            double[] clPars = getCLPars(track);
            printWriter.printf("Track clPar (xT yT dxTdz dyTdz) %.12f %.12f %.12f %.12f", clPars[0],clPars[1],clPars[2],clPars[3]);
            printWriter.println();
            
            Hep3Matrix clPrj = curvilinearProjectionMatrix(track.getDirection());
            StringBuffer sb = new StringBuffer();
            for(int i=0;i!=3;++i) 
                for(int j=0;j!=3;++j) 
                    sb.append(String.format("%.8f ", clPrj.e(i, j)));
            printWriter.printf("Track clPrj %s", sb.toString());
            printWriter.println();
            
            //List<SiTrackerHitStrip1D> stripHits = new ArrayList<SiTrackerHitStrip1D>();
            int iStrip = 0;
            for(StereoPair pair : track.getHits()) {
                SiTrackerHitStrip1D strips[] = new SiTrackerHitStrip1D[2];
                if(pair.getAxial().getPosition()[2] < pair.getStereo().getPosition()[2]) {
                    strips[0] = pair.getAxial();
                    strips[1] = pair.getStereo();
                } else {
                    strips[1] = pair.getAxial();
                    strips[0] = pair.getStereo();
                }
                for(int i=0; i<strips.length;++i) {
                    SiTrackerHitStrip1D strip = strips[i];
                    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) strip.getRawHits().get(0)).getDetectorElement();
                    printWriter.printf("New Strip id layer %d %d %s", iStrip, sensor.getMillepedeId(), sensor.getName() );
                    printWriter.println();
                    iStrip++;
                }
            }
            
            
        }
        
    }
    
    
    
    
    public static double[] getPerPars(STStereoTrack track) {
        double intercept[] = track.getIntercept();
        double slope[] = track.getSlope();
        return new double[]{
                intercept[VIEW.XZ.ordinal()],
                intercept[VIEW.YZ.ordinal()],
                slope[VIEW.XZ.ordinal()],
                slope[VIEW.YZ.ordinal()]};
    }
    
    
    /**
     * Computes projection matrix from the ST variables x0,y0,z0
     * to the curvilinear xT,yT,zT variables.
     *
     * @param track - {@link STStereoTrack}
     * @return 3x3 projection matrix
     */
    static Hep3Matrix curvilinearProjectionMatrix(Hep3Vector dir) {
        
        Hep3Vector X = new BasicHep3Vector(1, 0, 0);
        Hep3Vector Y = new BasicHep3Vector(0, 1, 0);
        Hep3Vector Z = new BasicHep3Vector(0, 0, 1);
        
        // build the curvilinear unit vectors: UVT
        Hep3Vector T = dir;
        Hep3Vector J = VecOp.mult(1. / VecOp.cross(T, Z).magnitude(), VecOp.cross(T, Z));
        Hep3Vector U = VecOp.mult(-1, J);
        Hep3Vector V = VecOp.cross(T, U);
        Hep3Vector K = Z;
        Hep3Vector I = VecOp.cross(J, K);

        logger.info("U " + U.toString());
        logger.info("V " + V.toString());
        logger.info("T " + T.toString());
        logger.info("X " + X.toString());
        logger.info("Y " + Y.toString());
        logger.info("Z " + Z.toString());
        BasicHep3Matrix trans = new BasicHep3Matrix();
        trans.setElement(0, 0, VecOp.dot(X, U));
        trans.setElement(0, 1, VecOp.dot(Y, U));
        trans.setElement(0, 2, VecOp.dot(Z, U));
        trans.setElement(1, 0, VecOp.dot(X, V));
        trans.setElement(1, 1, VecOp.dot(Y, V));
        trans.setElement(1, 2, VecOp.dot(Z, V));
        trans.setElement(2, 0, VecOp.dot(X, T));
        trans.setElement(2, 1, VecOp.dot(Y, T));
        trans.setElement(2, 2, VecOp.dot(Z, T));
        
        
        
        return trans;

    }

    
    /**
     * Computes projection matrix from the ST variables x0,y0,z0
     * to the curvilinear xT,yT,zT variables.
     *
     * @param track - {@link STStereoTrack}
     * @return 3x3 projection matrix
     */
    static Hep3Vector curvilinearProjection(double x0, double y0, Hep3Vector dir) {
        final double z0 = 0; 
        Hep3Vector v = new BasicHep3Vector(x0, y0, z0);
        Hep3Matrix trans = curvilinearProjectionMatrix(dir);
        Hep3Vector vnew = VecOp.mult(trans, v);

        StringBuffer sb = new StringBuffer();
        sb.append("Convert v " + v.toString() + " in ST x,y,z to curvilinear frame xT,yT,zT with prjection matrix:\n");
        for(int i=0;i!=3;++i) {
            sb.append("\n");
            for(int j=0;j!=3;++j) {
                sb.append(trans.e(i, j) + "  ");
            }
        }
        sb.append("New vector is " + vnew.toString() + " in curvilinear frame xT,yT,zT");
        logger.info(sb.toString());
        
        return vnew;

    }
    
    /**
     * 
     * Get curvilinear track parameter. 
     */
    public static double[] getCLPars(STStereoTrack track) {
        return getCLPars(track.getIntercept(VIEW.XZ), track.getIntercept(VIEW.YZ), track.getSlope(VIEW.XZ), track.getSlope(VIEW.YZ));
    }
    
    public static double[] getCLPars(double x0, double y0, double dxdz, double dydz) {
        // track direction
        Hep3Vector dir = new BasicHep3Vector(dxdz, dydz, 1 - Math.sqrt(dxdz*dxdz + dydz*dydz));
        
        final double z0 = 0;
        Hep3Vector v = new BasicHep3Vector(x0, y0, z0);
        Hep3Matrix trans = curvilinearProjectionMatrix(dir);
        Hep3Vector vnew = VecOp.mult(trans, v);

        // for readability
        double xT = vnew.x();
        double yT = vnew.y();
        //double zT = vnew.z();

        final double dzdz = 1.0;
        Hep3Vector dxyz_dv = new BasicHep3Vector(dxdz, dydz, dzdz);
        Hep3Vector dxyzT_dv = VecOp.mult(trans, dxyz_dv);

        // for readability
        double xT_prime = dxyzT_dv.x();
        double yT_prime = dxyzT_dv.y();
        //double zT_prime = dxyzT_dv.z();

        double clPars[] = new double[]{xT,yT,xT_prime,yT_prime};
        
        logger.info("getCLPars: \n(x0,y0,z0) " + v.toString() + "\n(xT,yT,zT) " + vnew.toString() + "\nd/dz(x,y,z) " + dxyz_dv.toString() + "\nd/dz(xT,yT,zT) " + dxyzT_dv.toString()  );
        StringBuffer sb = new StringBuffer();
        for(int i=0;i!=3;++i) {
            sb.append("\n");
            for(int j=0;j!=3;++j) {
                sb.append(trans.e(i, j) + "  ");
            }
        }
        logger.info("using CL prj matrix:\n" + sb.toString() + "\n and track direction "+ dir.toString());
        
        return clPars;
    }

    
    
    

    
    
}
