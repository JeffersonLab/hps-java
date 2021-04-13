package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.SiTrackerHitStrip1D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * This class is a converter from track to GBLTrajectory for alignment purposes.
 * It is largely a copy of the methods used in MakeGblTracks but with the difference that then invokes:
 * 1) ejml for matrix algebra
 * 2) jna for full support of GBL C++ external library
 *
 * No Fit is performed over GBLTrajectories
 * Constraints supported:
 * - impact parameters
 * - vertex constraint
 */
public class GblTrajectoryMaker {

    private final static Logger LOGGER = Logger.getLogger(MakeGblTracks.class.getPackage().getName());
    MultipleScattering _scattering;
    double _B;
    boolean _debug = false;
    boolean _includeMS = false;

    static {
        LOGGER.setLevel(Level.WARNING);
    }

    public GblTrajectoryMaker(MultipleScattering scattering , double B) {
        _scattering = scattering;
        _B = B;
    }
    
    public void setDebug(boolean debug) {
        _debug = debug;
    }
    
    public void setIncludeMS(boolean includeMS) {
        _includeMS = includeMS;
    }

    
    /**
     * Create a list of {@link GBLStripClusterData} objects that can be used as
     * input to the GBL fitter.
     *
     * @param htf
     * @param stripHits
     * @param _scattering
     * @param _B
     * @param _debug
     * @return the list of GBL strip cluster data
     */
    public List<GBLStripClusterData> makeStripData(HelicalTrackFit htf, List<TrackerHit> stripHits) {


        GblSimpleHelix gblHelix = new GblSimpleHelix(htf.curvature(), htf.phi0(), htf.dca(), htf.slope(), htf.z0());
        
        
        
        List<GBLStripClusterData> stripClusterDataList = new ArrayList<GBLStripClusterData>();

        // Find scatter points along the path
        //In principle I could use this to add the hits - TODO ?
        MultipleScattering.ScatterPoints scatters = _scattering.FindHPSScatterPoints(htf);
        
        //Loop over the Scatters
        
        //Two things can happen here at each iteration:
        //1) Nscatters >= Nhits on tracks =>
        //   Build GBLStripClusterData for both measurements and scatters
        //2) Nscatters < Nhits on track =>
        //   The hit has been associated to the track but couldn't find the scatter on the volume => create a scatter at that sensor
        //   Does this makes sense?
        
        //Case (1)
        //Check if the scatter has a measurement => then use the usual way to build the HelicalTrackStripGbl
        
        int nhits = 0;
        int nscatters = 0;
        boolean addScatters = true;
            
        if (scatters.getPoints().size() >= stripHits.size() && _includeMS) {
            
            for (MultipleScattering.ScatterPoint scatter : scatters.getPoints()) {
            
                boolean MeasOnScatter = false;
                
                //This is bit inefficient as it always loop on all the stripHits
                //TODO: optimize and only check hit once if is in the scatters.
                
                HpsSiSensor scatter_sensor = (HpsSiSensor) scatter.getDet();
            
                for (TrackerHit stripHit : stripHits) {
                
                    if (MeasOnScatter)
                        continue;
                
                    IDetectorElement det_element = ((RawTrackerHit) stripHit.getRawHits().get(0)).getDetectorElement();
                    
                    if (det_element.equals(scatter.getDet())) {
                        MeasOnScatter = true;
                        HpsSiSensor hit_sensor = (HpsSiSensor) det_element;
                        nhits+=1;
                        HelicalTrackStripGbl gbl_strip;
                        if (stripHit instanceof SiTrackerHitStrip1D) {
                            gbl_strip = new HelicalTrackStripGbl(makeDigiStrip((SiTrackerHitStrip1D) stripHit),true);
                        } else {
                            SiTrackerHitStrip1D newHit = new SiTrackerHitStrip1D(stripHit);
                            gbl_strip = new HelicalTrackStripGbl(makeDigiStrip(newHit), true);
                        }
                        //hit_sensor or scatter_sensor, they are the same here
                        GBLStripClusterData stripData = makeStripData(hit_sensor, gbl_strip, htf, scatter);
                        if (stripData != null) {
                            stripClusterDataList.add(stripData); 
                        }
                        else {
                            System.out.printf("WARNING::MakeGblTracks::Couldn't make stripClusterData for hps sensor. Skipping scatter");
                        }
                    }
                }
            
                if (!MeasOnScatter && addScatters) {
                    //No measurement
                    nscatters+=1;
                    //If the scatter has no measurement => then only build a scatter point 
                    //Here only scatter sensor is available
                    GBLStripClusterData stripData = makeScatterOnlyData(scatter_sensor,htf,scatter);
                    
                    if (stripData != null) {
                        stripClusterDataList.add(stripData);
                    }
                }
            } // loop on scatters
        } // more scatters than hits
        
        else { //more hits than scatters 
            
   
            for (TrackerHit stripHit : stripHits) {
                HelicalTrackStripGbl strip;
                if (stripHit instanceof SiTrackerHitStrip1D) {
                    
                    strip = new HelicalTrackStripGbl(makeDigiStrip((SiTrackerHitStrip1D) stripHit), true);
                    
                } else {
                    SiTrackerHitStrip1D newHit = new SiTrackerHitStrip1D(stripHit);
                    strip = new HelicalTrackStripGbl(makeDigiStrip(newHit),true);
                                                            
                }
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stripHit.getRawHits().get(0)).getDetectorElement();
                MultipleScattering.ScatterPoint temp = scatters.getScatterPoint(((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement());
                
                //This is done to correct the fact that the helical fit might not hit a volume
                //But hit is associated to the track => must scatter
                if (temp == null){
                    temp = getScatterPointGbl(sensor, strip, htf);
                    if(temp == null){
                        return null;
                    }
                }

                //Making strip data 
                GBLStripClusterData stripData = makeStripData(sensor, strip, htf, temp);
                if (stripData != null)
                    stripClusterDataList.add(stripData);
            }
        }
        
        //Make sure they are sorted by path Lenght
        Collections.sort(stripClusterDataList,new SortByPathLength());
        
        return stripClusterDataList;
    }
    
    private HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {
        
        
        SiTrackerHitStrip1D local  = new SiTrackerHitStrip1D (h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR));
        SiTrackerHitStrip1D global = new SiTrackerHitStrip1D (h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL));
        
        ITransform3D trans = local.getLocalToGlobal();
        Hep3Vector org = trans.transformed(new BasicHep3Vector(0., 0., 0.));
        Hep3Vector u = global.getMeasuredCoordinate();
        Hep3Vector v = global.getUnmeasuredCoordinate();
        
        // rotate to tracking frame
        Hep3Vector neworigin = CoordinateTransformations.transformVectorToTracking(org);
        Hep3Vector newu = CoordinateTransformations.transformVectorToTracking(u);
        Hep3Vector newv = CoordinateTransformations.transformVectorToTracking(v);
        
        double umeas = local.getPosition()[0];
        double vmin = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getStartPoint());
        double vmax = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getEndPoint());
        double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));
        double dEdx = h.getdEdx();
        double time = h.getTime();
        List<RawTrackerHit> rawhits = h.getRawHits();
        
        //Print the origins!
        //System.out.printf("PF::Debug MakeDigiStrip For det element %d! \n", ((HpsSiSensor) det_element).getMillepedeId());
        //System.out.printf("Strip  origin %s \n", ((BasicHep3Vector)org).toString());
        //System.out.printf("Strip  origin Tracking %s \n", ((BasicHep3Vector)neworigin).toString());
        
        
        
        HelicalTrackStrip strip = new HelicalTrackStrip(neworigin, newu, newv, umeas, du, vmin, vmax, dEdx, time, rawhits, null, -1, null);
                
        return strip;
    }

    
    public MultipleScattering.ScatterPoint getScatterPointGbl(HpsSiSensor sensor, HelicalTrackStripGbl strip, HelicalTrackFit htf) {
        
        MultipleScattering.ScatterPoint temp = null;
        
        Hep3Vector pos = TrackUtils.getHelixPlaneIntercept(htf, strip, Math.abs(_B));
        if (pos == null) {
            System.out.println("Can't find track intercept; aborting Track refit");
            return null;
        }
        ScatterAngle scatAngle = new ScatterAngle((HelixUtils.PathToXPlane(htf, pos.x(), 0, 0).get(0)), GblUtils.estimateScatter(sensor, htf, _scattering, _B));
        temp = new MultipleScattering.ScatterPoint(((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement(), scatAngle);
        temp.setPosition(pos);
        temp.setDirection(HelixUtils.Direction(htf, scatAngle.PathLen()));
        
        return temp;
    }
    
    public static GBLStripClusterData makeStripData(HpsSiSensor sensor, HelicalTrackStripGbl strip, HelicalTrackFit htf, MultipleScattering.ScatterPoint temp) {
        if (temp == null)
            return null;
        
        // find Millepede layer definition from DetectorElement
        int millepedeId = sensor.getMillepedeId();
        // find volume of the sensor (top or bottom)
        int volume = sensor.isTopLayer() ? 0 : 1;
        
        // Center of the strip
        Hep3Vector origin = strip.origin();
        
        //Get the sensor frame transformations
        //ITransform3D sensor_l2g = sensor.getGeometry().getLocalToGlobal();
        //Hep3Vector s_org = sensor_l2g.transformed(new BasicHep3Vector(0.,0.,0.));
        
        //This is the center of the sensor
        Hep3Vector s_pos = sensor.getGeometry().getPosition();
        
        //Center of the sensor in tracking coordinates.
        Hep3Vector s_neworigin = CoordinateTransformations.transformVectorToTracking(s_pos);
        
        // GBLDATA
        GBLStripClusterData stripData = new GBLStripClusterData(millepedeId);
        
        // Add the volume
        stripData.setVolume(volume);
        
        //This GBLStripData holds a measurement
        stripData.setScatterOnly(0);
        
        // Add to output list
        
        // path length to intercept
        double s3D = temp.getScatterAngle().PathLen() / Math.cos(Math.atan(htf.slope()));
        
        // GBLDATA
        stripData.setPath(temp.getScatterAngle().PathLen());
        stripData.setPath3D(s3D);
        
        //GBLDATA
        stripData.setU(strip.u());
        stripData.setV(strip.v());
        stripData.setW(strip.w());

        // Print track direction at intercept
        double phi = htf.phi0() - temp.getScatterAngle().PathLen() / htf.R();
        double lambda = Math.atan(htf.slope());
        
        // GBLDATA
        stripData.setTrackDir(temp.getDirection());
        stripData.setTrackPhi(phi);
        stripData.setTrackLambda(lambda);
        
        // Print residual in measurement system
        // start by find the distance vector between the center and the track position
        
        //DEBUG PF: check if the we return the origin or the corner of the sensors. 
        
        // Find the rotation from tracking to measurement frame 
        // This matrix is just 
        //|u1 u2 u3 | 
        //|v1 v2 v3 |
        //|w1 w2 w3 | 
        
        //This is clearer and it's identical
        Hep3Matrix trkToStripRot = VecOp.mult(sensor.getGeometry().getGlobalToLocal().getRotation().getRotationMatrix(),
                                              CoordinateTransformations.getMatrixInverse());
        
        Hep3Vector vdiffTrk = VecOp.sub(temp.getPosition(), s_neworigin);
                
        // then rotate that vector into the measurement frame to get the predicted measurement position
        Hep3Vector trkpos_meas = VecOp.mult(trkToStripRot, vdiffTrk);
                
        
        // GBLDATA
        stripData.setMeas(strip.umeas());
        stripData.setTrackPos(trkpos_meas);
        stripData.setMeasErr(strip.du());
        
        // GBLDATA
        stripData.setScatterAngle(temp.getScatterAngle().Angle());
        
        return stripData;
    }
    
    public static GBLStripClusterData makeScatterOnlyData(HpsSiSensor sensor, HelicalTrackFit htf, MultipleScattering.ScatterPoint temp) {
        
        if (temp ==null)
            return null;
        
        // find Millepede layer definition from DetectorElement
        int millepedeId = sensor.getMillepedeId();
        // find volume of the sensor (top or bottom)
        int volume = sensor.isTopLayer() ? 0 : 1;
        
        // GBLDATA
        GBLStripClusterData stripData = new GBLStripClusterData(millepedeId);
        stripData.setVolume(volume);
        
        //This GBLStripData doesn't hold a measurement
        stripData.setScatterOnly(1);
        
        double s3D = temp.getScatterAngle().PathLen() / Math.cos(Math.atan(htf.slope()));
        stripData.setPath(temp.getScatterAngle().PathLen());
        stripData.setPath3D(s3D);
        
        //Do not set U,V,W
        
        // Print track direction at intercept
        double phi = htf.phi0() - temp.getScatterAngle().PathLen() / htf.R();
        double lambda = Math.atan(htf.slope());
        
        stripData.setTrackDir(temp.getDirection());
        stripData.setTrackPhi(phi);
        stripData.setTrackLambda(lambda);
        
        //Do not set the measurement. 
        //Set a negative large error
        stripData.setMeasErr(-9999);
        
        stripData.setScatterAngle(temp.getScatterAngle().Angle());
        
        return stripData;
    }

}

class SortByPathLength implements Comparator<GBLStripClusterData> {
    public int compare(GBLStripClusterData a, GBLStripClusterData b) 
    { 
        Double as = a.getPath();
        Double bs = b.getPath();
        return Double.compare(as,bs); 
    } 
} 


