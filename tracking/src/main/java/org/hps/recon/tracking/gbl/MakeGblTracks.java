package org.hps.recon.tracking.gbl;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
//import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.hps.recon.tracking.SiTrackerHitStrip1D;
import org.lcsim.constants.Constants;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * Utilities that create track objects from fitted GBL trajectories.
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @author Miriam Diamond
 *
 */
public class MakeGblTracks {

    private final static Logger LOGGER = Logger.getLogger(MakeGblTracks.class.getPackage().getName());

    static {
        LOGGER.setLevel(Level.WARNING);
    }

    private MakeGblTracks() {
    }

    public static void setDebug(boolean debug) {
        if (debug) {
            LOGGER.setLevel(Level.INFO);
        } else {
            LOGGER.setLevel(Level.OFF);
        }
    }

    public static Pair<Track, GBLKinkData> makeCorrectedTrack(FittedGblTrajectory fittedGblTrajectory, HelicalTrackFit helicalTrackFit, List<TrackerHit> hitsOnTrack, int trackType, double bfield) {
        return makeCorrectedTrack(fittedGblTrajectory, helicalTrackFit, hitsOnTrack, trackType, bfield, false);
    }

    /**
     * Create a new {@link BaseTrack} from a {@link FittedGblTrajectory}.
     *
     * @param fittedGblTrajectory
     * @param helicalTrackFit
     * @param hitsOnTrack
     * @param trackType
     * @param bfield
     * @return the new {@link BaseTrack} and the kinks along the
     * {@link GblTrajectory} as a {@link Pair}.
     */
    public static Pair<Track, GBLKinkData> makeCorrectedTrack(FittedGblTrajectory fittedGblTrajectory, HelicalTrackFit helicalTrackFit, List<TrackerHit> hitsOnTrack, int trackType, double bfield, boolean storeTrackStates) {
        //  Initialize the reference point to the origin
        double[] ref = new double[] { 0., 0., 0. };

        // Create a new SeedTrack
        BaseTrack trk = new BaseTrack();

        // Add the hits to the track
        for (TrackerHit hit : hitsOnTrack) {
            trk.addHit(hit);
        }

        // Set state at IP
        Pair<double[], SymmetricMatrix> correctedHelixParams = fittedGblTrajectory.getCorrectedPerigeeParameters(helicalTrackFit, FittedGblTrajectory.GBLPOINT.IP, bfield);
        trk.setTrackParameters(correctedHelixParams.getFirst(), bfield);// hack to set the track charge
        trk.getTrackStates().clear();
        TrackState stateIP = new BaseTrackState(correctedHelixParams.getFirst(), ref, correctedHelixParams.getSecond().asPackedArray(true), TrackState.AtIP, bfield);
        trk.getTrackStates().add(stateIP);

        if (!storeTrackStates) {
            // just store last state
            Pair<double[], SymmetricMatrix> correctedHelixParamsLast = fittedGblTrajectory.getCorrectedPerigeeParameters(helicalTrackFit, FittedGblTrajectory.GBLPOINT.LAST, bfield);
            TrackState stateLast = new BaseTrackState(correctedHelixParamsLast.getFirst(), ref, correctedHelixParamsLast.getSecond().asPackedArray(true), TrackState.AtLastHit, bfield);
            trk.getTrackStates().add(stateLast);
        } else {

            // store states at all 18 sensors
            int prevID = 0;
            int dummyCounter = -1;
            // note: SensorMap doesn't include IP
            Integer[] sensorsFromMapArray = fittedGblTrajectory.getSensorMap().keySet().toArray(new Integer[0]);

            for (int i = 0; i < sensorsFromMapArray.length; i++) {
                int ilabel = sensorsFromMapArray[i];
                int millepedeID = fittedGblTrajectory.getSensorMap().get(ilabel);

                // if sensors are missing from track, insert blank TrackState objects
                for (int k = 1; k < millepedeID - prevID; k++) {
                    // uses new lcsim constructor
                    BaseTrackState dummy = new BaseTrackState(dummyCounter);
                    trk.getTrackStates().add(dummy);
                    dummyCounter--;
                }
                prevID = millepedeID;
                Pair<double[], SymmetricMatrix> correctedHelixParamsSensor = fittedGblTrajectory.getCorrectedPerigeeParameters(helicalTrackFit, ilabel, bfield);

                // set TrackState location code
                int loc = TrackState.AtOther;
                if (i == 0) {
                    loc = TrackState.AtFirstHit;
                } else if (i == sensorsFromMapArray.length - 1) {
                    loc = TrackState.AtLastHit;
                }
                // insert TrackState at sensor
                TrackState stateSensor = new BaseTrackState(correctedHelixParamsSensor.getFirst(), ref, correctedHelixParamsSensor.getSecond().asPackedArray(true), loc, bfield);
                trk.getTrackStates().add(stateSensor);
            }

        }

        // Extract kinks from trajectory
        GBLKinkData kinkData = fittedGblTrajectory.getKinks();

        // Set other info needed
        trk.setChisq(fittedGblTrajectory.get_chi2());
        trk.setNDF(fittedGblTrajectory.get_ndf());
        trk.setFitSuccess(true);
        trk.setRefPointIsDCA(true);
        trk.setTrackType(TrackType.setGBL(trackType, true));

        LOGGER.fine(String.format("helix chi2 %f ndf %d gbl chi2 %f ndf %d\n", helicalTrackFit.chisqtot(), helicalTrackFit.ndf()[0] + helicalTrackFit.ndf()[1], trk.getChi2(), trk.getNDF()));

        return new Pair<Track, GBLKinkData>(trk, kinkData);
    }

    public static Pair<Track, GBLKinkData> refitTrack(HelicalTrackFit helix, Collection<TrackerHit> stripHits, Collection<TrackerHit> hth, int nIterations, int trackType, MultipleScattering scattering, double bfield) {
        return refitTrack(helix, stripHits, hth, nIterations, trackType, scattering, bfield, false);
    }

    /**
     * Do a GBL fit to an arbitrary set of strip hits, with a starting value of
     * the helix parameters.
     *
     * @param helix Initial helix parameters. Only track parameters are used
     * (not covariance)
     * @param stripHits Strip hits to be used for the GBL fit. Does not need to
     * be in sorted order.
     * @param hth Stereo hits for the track's hit list (these are not used in
     * the GBL fit). Does not need to be in sorted order.
     * @param nIterations Number of times to iterate the GBL fit.
     * @param scattering Multiple scattering manager.
     * @param bfield B-field
     * @return The refitted track.
     */
    public static Pair<Track, GBLKinkData> refitTrack(HelicalTrackFit helix, Collection<TrackerHit> stripHits, Collection<TrackerHit> hth, int nIterations, int trackType, MultipleScattering scattering, double bfield, boolean storeTrackStates) {
        Pair<Pair<Track, GBLKinkData>, FittedGblTrajectory> refitTrack = refitTrackWithTraj(helix, stripHits, hth, nIterations, trackType, scattering, bfield, storeTrackStates,false);
        if(refitTrack == null)
            return null;
        else
            return refitTrack.getFirst();
    }

    public static Pair<Pair<Track, GBLKinkData>, FittedGblTrajectory> refitTrackWithTraj(HelicalTrackFit helix, Collection<TrackerHit> stripHits, Collection<TrackerHit> hth, int nIterations, int trackType, MultipleScattering scattering, double bfield, boolean storeTrackStates,boolean includeMS) {

        List<TrackerHit> allHthList = TrackUtils.sortHits(hth);
        List<TrackerHit> sortedStripHits = TrackUtils.sortHits(stripHits);
        FittedGblTrajectory fit = doGBLFit(helix, sortedStripHits, scattering, bfield, 0,includeMS);
        if(fit==null) return null;
        for (int i = 0; i < nIterations; i++) {
            Pair<Track, GBLKinkData> newTrack = makeCorrectedTrack(fit, helix, allHthList, trackType, bfield);
            helix = TrackUtils.getHTF(newTrack.getFirst());
            fit = doGBLFit(helix, sortedStripHits, scattering, bfield, 0,includeMS);
            if (fit == null)
                return null;
        }

        Pair<Track, GBLKinkData> mergedTrack = makeCorrectedTrack(fit, helix, allHthList, trackType, bfield, storeTrackStates);
        return new Pair<Pair<Track, GBLKinkData>, FittedGblTrajectory>(mergedTrack, fit);
    }

    /**
     * Do a GBL fit to a list of {@link TrackerHit}.
     *
     * @param htf - seed fit
     * @param stripHits - list of {@link TrackerHit}.
     * @param _scattering - estimation of the multiple scattering
     * {@link MultipleScattering}.
     * @param bfield - magnitude of B-field.
     * @param debug - debug flag.
     * @return the fitted GBL trajectory
     */
    public static FittedGblTrajectory doGBLFit(HelicalTrackFit htf, List<TrackerHit> stripHits, MultipleScattering _scattering, double bfield, int debug,boolean includeMS) {
        List<GBLStripClusterData> stripData = makeStripData(htf, stripHits, _scattering, bfield, debug, includeMS);
        if (stripData == null)
            return null;
        double bfac = Constants.fieldConversion * bfield;
        FittedGblTrajectory fit = HpsGblRefitter.fit(stripData, bfac, debug > 0);
        return fit;
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
    public static List<GBLStripClusterData> makeStripData(HelicalTrackFit htf, List<TrackerHit> stripHits, MultipleScattering _scattering, double _B, int _debug,boolean includeMS) {
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
            
        if (scatters.getPoints().size() >= stripHits.size() && includeMS) {
            
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
                    temp = getScatterPointGbl(sensor, strip, htf, _scattering, _B);
                    if(temp == null){
                        return null;
                    }
                }
                GBLStripClusterData stripData = makeStripData(sensor, strip, htf, temp);
                if (stripData != null)
                    stripClusterDataList.add(stripData);
            }
        }
        
        return stripClusterDataList;
    }

    public static MultipleScattering.ScatterPoint getScatterPointGbl(HpsSiSensor sensor, HelicalTrackStripGbl strip, HelicalTrackFit htf, MultipleScattering _scattering, double _B) {

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

    public static GBLStripClusterData makeStripData(HpsSiSensor sensor, HelicalTrackStripGbl strip, HelicalTrackFit htf, MultipleScattering.ScatterPoint temp) {
        if (temp == null)
            return null;

        // find Millepede layer definition from DetectorElement
        int millepedeId = sensor.getMillepedeId();
        // find volume of the sensor (top or bottom)
        int volume = sensor.isTopLayer() ? 0 : 1;

        // Center of the sensor
        Hep3Vector origin = strip.origin();

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

        // GBLDATA
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
        Hep3Vector vdiffTrk = VecOp.sub(temp.getPosition(), origin);

        // then find the rotation from tracking to measurement frame
        Hep3Matrix trkToStripRot = getTrackToStripRotation(sensor);

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

    private static Hep3Matrix getTrackToStripRotation(SiSensor sensor) {
        // This function transforms the hit to the sensor coordinates

        // Transform from JLab frame to sensor frame (done through the RawTrackerHit)
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        ITransform3D detToStrip = electrodes.getGlobalToLocal();
        // Get rotation matrix
        Hep3Matrix detToStripMatrix = detToStrip.getRotation().getRotationMatrix();

        return VecOp.mult(detToStripMatrix, CoordinateTransformations.getMatrixInverse());
    }

    private static HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {
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
        HelicalTrackStrip strip = new HelicalTrackStrip(neworigin, newu, newv, umeas, du, vmin, vmax, dEdx, time, rawhits, null, -1, null);

        return strip;
    }
}
