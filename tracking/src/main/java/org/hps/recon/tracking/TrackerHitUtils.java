package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//===> import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;

public class TrackerHitUtils {

    private boolean _DEBUG = false;

    public TrackerHitUtils() {
    }

    public TrackerHitUtils(boolean debug) {
        _DEBUG = debug;
    }

    public void setDebug(boolean debug) {
        _DEBUG = debug;
    }

    public Hep3Matrix detToTrackRotationMatrix() {
        return (BasicHep3Matrix) CoordinateTransformations.getMatrix();
    }

    public ITransform3D GetGlobalToLocal(HelicalTrackStrip strip) {
        // Transform from JLab frame (RawTrackerHit) to sensor frame (i.e. u,v,w)
        RawTrackerHit rth = (RawTrackerHit) strip.rawhits().get(0);
        IDetectorElement ide = rth.getDetectorElement();
        SiSensor sensor = ide.findDescendants(SiSensor.class).get(0);
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);        
        return electrodes.getGlobalToLocal();
    }

    public Hep3Matrix getStripToTrackRotation(HelicalTrackStrip strip) {
        // This function transforms the hit to the tracking coordinates

        // Transform from JLab frame to sensor frame (done through the RawTrackerHit)
        ITransform3D detToStrip = GetGlobalToLocal(strip);
        // Get rotation matrix
        Hep3Matrix detToStripMatrix = (BasicHep3Matrix) detToStrip.getRotation().getRotationMatrix();
        // Transformation between the JLAB and tracking coordinate systems
        Hep3Matrix detToTrackMatrix = (BasicHep3Matrix) CoordinateTransformations.getMatrix();

        if (_DEBUG) {
            System.out.println("gblToLoc translation:");
            System.out.println(detToStrip.getTranslation().toString());
            System.out.println("gblToLoc Rotation:");
            System.out.println(detToStrip.getRotation().toString());
            System.out.println("detToTrack Rotation:");
            System.out.println(detToTrackMatrix.toString());
        }

        return (Hep3Matrix) VecOp.mult(detToTrackMatrix, VecOp.inverse(detToStripMatrix));
    }

    public Hep3Matrix getTrackToStripRotation(HelicalTrackStrip strip) {
        // This function transforms the hit to the sensor coordinates

        // Transform from JLab frame to sensor frame (done through the RawTrackerHit)
        ITransform3D detToStrip = this.GetGlobalToLocal(strip);
        // Get rotation matrix
        Hep3Matrix detToStripMatrix = (BasicHep3Matrix) detToStrip.getRotation().getRotationMatrix();
        // Transformation between the JLAB and tracking coordinate systems
        Hep3Matrix detToTrackMatrix = this.detToTrackRotationMatrix();

        if (_DEBUG) {
            System.out.println("gblToLoc translation:");
            System.out.println(detToStrip.getTranslation().toString());
            System.out.println("gblToLoc Rotation:");
            System.out.println(detToStrip.getRotation().toString());
            System.out.println("detToTrack Rotation:");
            System.out.println(detToTrackMatrix.toString());
            System.out.println("inverse detToTrack Rotation:");
            System.out.println(VecOp.inverse(detToTrackMatrix).toString());
        }

        return (Hep3Matrix) VecOp.mult(detToStripMatrix, VecOp.inverse(detToTrackMatrix));
    }

    public Hep3Vector getClusterPosition(HelicalTrackStrip strip, boolean stripInTrackingFrame) {
        if (_DEBUG)
            System.out.println(this.getClass().getSimpleName() + " getClusterPosition--");
        Hep3Vector origin = stripInTrackingFrame ? strip.origin() : VecOp.mult(CoordinateTransformations.getMatrix(), strip.origin());
        if (_DEBUG)
            System.out.println(this.getClass().getSimpleName() + " origin " + origin.toString());
        Hep3Vector hit_vec_LOCAL = new BasicHep3Vector(strip.umeas(), 0, 0);
        if (_DEBUG)
            System.out.println(this.getClass().getSimpleName() + " hit_vec_LOCAL " + hit_vec_LOCAL.toString());
        Hep3Matrix stripToTrack = this.getStripToTrackRotation(strip);
        if (_DEBUG)
            System.out.println(this.getClass().getSimpleName() + " stripToTrack " + stripToTrack.toString());
        Hep3Vector hit_vec_TRACK = VecOp.mult(stripToTrack, hit_vec_LOCAL);
        if (_DEBUG)
            System.out.println(this.getClass().getSimpleName() + " hit_vec_TRACK " + hit_vec_TRACK.toString());
        Hep3Vector strip_pos = VecOp.add(origin, hit_vec_TRACK);
        if (_DEBUG)
            System.out.println(this.getClass().getSimpleName() + " strip_pos " + strip_pos.toString());

        // Hep3Vector hit_vec_LOCAL_dep = new BasicHep3Vector(strip.umeas(),0,0.16);
        // if(_DEBUG) System.out.println(this.getClass().getSimpleName() + " hit_vec_LOCAL_dep " +
        // hit_vec_LOCAL_dep.toString());
        // Hep3Vector hit_vec_TRACK_dep = VecOp.mult(stripToTrack, hit_vec_LOCAL_dep);
        // Hep3Vector strip_pos_dep = VecOp.add(origin, hit_vec_TRACK_dep);
        // if(_DEBUG) System.out.println(this.getClass().getSimpleName() + " strip_pos ALTERNATE "
        // + strip_pos_dep.toString());

        return strip_pos;
    }

    public Hep3Vector CalculateStripUncertaintyInGlobalFrame(HelicalTrackStrip strip, HelicalTrackFit trk, double msdrdphi, double msdz) {

        if (_DEBUG)
            System.out.println("--- CalculateStripUncertainyInGlobalFrame ---");
        if (_DEBUG)
            System.out.println("Strip origin = " + strip.origin().toString());
        Hep3Vector u = strip.u();
        Hep3Vector v = strip.v();
        Hep3Vector w = strip.w();
        Hep3Vector corigin = strip.origin();

        double phi0 = trk.phi0();
        double R = trk.R();
        double xint = strip.origin().x();
        // double xint = this.calculateHelixInterceptXPlane(_trk, strip);
        double s = HelixUtils.PathToXPlane(trk, xint, 0, 0).get(0);
        double phi = -s / R + phi0;
        if (_DEBUG)
            System.out.println("phi0 " + phi0 + " R " + R + " xint " + xint + " s " + s + " phi " + phi);
        // if(_DEBUG) System.out.println("trkpos = "+trkpos.toString());
        // if(_DEBUG) System.out.println("origin = "+corigin.toString());

        Hep3Vector mserr = new BasicHep3Vector(msdrdphi * Math.sin(phi), msdrdphi * Math.sin(phi), msdz);
        if (_DEBUG)
            System.out.println("msdrdphi = " + msdrdphi + " msdz = " + msdz);
        if (_DEBUG)
            System.out.println("mserr = " + mserr.toString());
        double uHitError = strip.du();
        double msuError = VecOp.dot(mserr, u);
        double uError = Math.sqrt(uHitError * uHitError + msuError * msuError);
        if (_DEBUG)
            System.out.println("uError = " + uError + "(MS " + msuError + ",u=" + u.toString() + ")");
        double vHitError = (strip.vmax() - strip.vmin()) / Math.sqrt(12);
        double msvError = VecOp.dot(mserr, v);
        double vError = Math.sqrt(vHitError * vHitError + msvError * msvError);
        if (_DEBUG)
            System.out.println("vError = " + vError + "(MS " + msvError + ",v=" + v.toString() + ")");

        double wHitError = 10.0 / Math.sqrt(12); // 0.001;
        double mswError = VecOp.dot(mserr, w);
        double wError = Math.sqrt(wHitError * wHitError + mswError * mswError);
        if (_DEBUG)
            System.out.println("wError = " + wError + "(MS " + mswError + ",w=" + w.toString() + ")");

        Hep3Vector dq_local = new BasicHep3Vector(uError, vError, wError);
        if (_DEBUG)
            System.out.println("dq_local = " + dq_local.toString());
        Hep3Matrix trackToStripRot = getTrackToStripRotation(strip);
        if (_DEBUG)
            System.out.println("trackToStripRot:\n " + trackToStripRot.toString());
        Hep3Matrix stripToTrackRot = VecOp.inverse(trackToStripRot);
        if (_DEBUG)
            System.out.println("stripToTrackRot:\n " + stripToTrackRot.toString());
        Hep3Vector dq_global = VecOp.mult(stripToTrackRot, dq_local);
        if (_DEBUG)
            System.out.println("q_global = " + dq_global.toString());
        return dq_global;
    }

    public List<SimTrackerHit> stripClusterToSimHits(HelicalTrackStrip strip, List<SimTrackerHit> simTrackerHits, boolean stripsInTrackingFrame) {

        int layer = strip.layer();
        Hep3Vector stripPosition = this.getClusterPosition(strip, stripsInTrackingFrame);

        // Sort the SimTrackerHits by Layer
        Map<Integer, List<SimTrackerHit>> layerToSimTrackerHit = new HashMap<Integer, List<SimTrackerHit>>();
        for (SimTrackerHit simTrackerHit : simTrackerHits) {
            if (!layerToSimTrackerHit.containsKey(simTrackerHit.getLayer()))
                layerToSimTrackerHit.put(simTrackerHit.getLayer(), new ArrayList<SimTrackerHit>());
            layerToSimTrackerHit.get(simTrackerHit.getLayer()).add(simTrackerHit);
        }

        //
        List<SimTrackerHit> simhits = new ArrayList<SimTrackerHit>();

        if (layerToSimTrackerHit.get(strip.layer()) == null) {
            System.out.println(this.getClass().getSimpleName() + ": WARNING there is a strip in layer " + strip.layer() + " but no SimTrackerHits");
            return simhits;
        }

        // If there is only a single SimTrackerHit on a layer and it's in the same volume as the
        // strip hit then they likely match to each other
        if (layerToSimTrackerHit.get(strip.layer()).size() == 1) {
            Hep3Vector simTrackerHitPosition = layerToSimTrackerHit.get(strip.layer()).get(0).getPositionVec();
            if (Math.signum(simTrackerHitPosition.y()) == Math.signum(stripPosition.z())) {
                simhits.add(layerToSimTrackerHit.get(strip.layer()).get(0));
                layerToSimTrackerHit.remove(strip.layer());
                if (_DEBUG) {
                    System.out.println(this.getClass().getSimpleName() + ": SimTrackerHit position: " + simTrackerHitPosition.toString());
                    System.out.println(this.getClass().getSimpleName() + ": Cluster position: " + stripPosition.toString());
                }
            } else {
                System.out.println(this.getClass().getSimpleName() + ": Cluster and SimTrackerHit are on different volumes");
            }
        } else if (layerToSimTrackerHit.get(strip.layer()).size() > 1) {
            if (_DEBUG)
                System.out.println(this.getClass().getSimpleName() + ": found " + layerToSimTrackerHit.get(strip.layer()).size() + " SimTrackerHits to match to strip in layer " + strip.layer());
            // System.exit(1);
            double deltaZ = Double.MAX_VALUE;
            SimTrackerHit simTrackerHitMatch = null;
            for (SimTrackerHit simTrackerHit : layerToSimTrackerHit.get(strip.layer())) {
                if (Math.abs(simTrackerHit.getPositionVec().y() - stripPosition.z()) < deltaZ) {
                    deltaZ = Math.abs(simTrackerHit.getPositionVec().y() - stripPosition.z());
                    simTrackerHitMatch = simTrackerHit;
                }
            }
            simhits.add(simTrackerHitMatch);
            layerToSimTrackerHit.remove(strip.layer()).remove(simTrackerHitMatch);
            if (_DEBUG) {
                System.out.println(this.getClass().getSimpleName() + ": SimTrackerHit position: " + simTrackerHitMatch.getPositionVec().toString());
                System.out.println(this.getClass().getSimpleName() + ": Cluster position: " + stripPosition);
            }
        }

        return simhits;
    }

    /**
     * Make a SimTrackerHit {@link IIdentifier} for a given layer number
     * 
     * @param sensor : The sensor on which the SimTrackerHit is created on
     * @return A 32-bit SimTrackerHit identifier
     */
    public static IIdentifier makeSimTrackerHitId(SiSensor sensor) {

        // Get the sensors identifier
        IExpandedIdentifier id = new ExpandedIdentifier(sensor.getExpandedIdentifier());

        // Get the helper and dictionary
        IIdentifierHelper helper = sensor.getIdentifierHelper();
        IIdentifierDictionary dictionary = helper.getIdentifierDictionary();

        // Fill in the layer number
        //===> id.setValue(dictionary.getFieldIndex("layer"), SvtUtils.getInstance().getLayerNumber(sensor));
        id.setValue(dictionary.getFieldIndex("layer"), ((HpsSiSensor) sensor).getLayerNumber());

        // Pack and return the identifier
        return helper.pack(id);

    }

    // public List<SiTrackerHit> stripClusterToSiHits(HelicalTrackStrip strip, List<SiTrackerHit>
    // siTrackerHits, boolean stripsInTrackingFrame)
    // {
    //
    // //Should be a one to one match with a strip!
    // Hep3Vector stripPosition = this.getClusterPosition(strip,stripsInTrackingFrame);
    // if(_DEBUG) System.out.println("Strip position " + stripPosition.toString() + " ( " +
    // strip.origin().toString() + ")");
    //
    // for(SiTrackerHit siTrackerHit : siTrackerHits){
    // SiTrackerHitStrip1D h = (SiTrackerHitStrip1D) siTrackerHit;
    // if(_DEBUG) System.out.println("SiTrackerHit origin position " +
    // h.getPositionAsVector().toString());
    //
    // }
    //
    // List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();
    // return hits;
    //
    // }
}
