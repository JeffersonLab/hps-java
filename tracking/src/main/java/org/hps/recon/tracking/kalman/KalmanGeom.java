package org.hps.recon.tracking.kalman;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.HPSTransformations;
import org.hps.recon.tracking.kalman.util.PropDCAZ;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.ISolid;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.PropDispatch;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.PropCyl;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfcylplane.PropCylZ;
import org.lcsim.recon.tracking.trfcylplane.PropZCyl;
import org.lcsim.recon.tracking.trfdca.PropCylDCA;
import org.lcsim.recon.tracking.trfdca.PropDCACyl;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trffit.HTrack;
import org.lcsim.recon.tracking.trfxyp.ClusXYPlane1;
import org.lcsim.recon.tracking.trfxyp.PropXYXY;
import org.lcsim.recon.tracking.trfxyp.SurfXYPlane;
import org.lcsim.recon.tracking.trfzp.PropZZ;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 *
 * Extract needed information from the geometry system and serve it
 * to the callers in a convenient form.
 *
 *
 *@author $Author: phansson $
 *@version $Id: KalmanGeom.java,v 1.6 2013/10/15 00:33:54 phansson Exp $
 *
 * Date $Date: 2013/10/15 00:33:54 $
 *
 */
/* To make the Kalman filter work for any detector, the type of hit that is
 * added to the HTrack has to be determined based on the lcsim detector
 * geometry. Additionally, this class has to be able to find the intersections
 * between a track and the detector elements to be able to implement more
 * realistic multiple scattering. Currently this class does not support
 * any of this functionality, and just adds hits. However there are methods for
 * looping through detector elements, etc. that may be useful for finding
 * intersections at some point. (ecfine) */
public class KalmanGeom {

    // TRF wants distances in cm, not mm.
    private double mmTocm = 0.1;
    // this flag is a temporary fix to avoid making a surface behind the origin in x.
    private double flag = 0;
//    //    private Array radius = null;
//    private Subdetector sd_tbar  = null;
//    private Subdetector sd_vbar  = null;
//    private Subdetector sd_tec   = null;
//    private Subdetector sd_vec   = null;
//    private Subdetector sd_tfor  = null;
//    private Subdetector sd_bpipe = null;
//
//    private IDetectorElement de_tbar  = null;
//    private IDetectorElement de_vbar  = null;
//    private IDetectorElement de_tec   = null;
//    private IDetectorElement de_vec   = null;
//    private IDetectorElement de_tfor  = null;
    // Nominal magnetic field.
    public double bz = 0.5;
    // Lists of interesting surfaces.
    public List<KalmanSurface> Surf = new ArrayList<KalmanSurface>();
    KalmanSurface surf = null;
    // Specific surface type A to surface type B propagators.
    private PropCyl propcyl = null;
    private PropZZ propzz = null;
    private PropDCACyl propdcacyl = null;
    private PropCylDCA propcyldca = null;
    private PropZCyl propzcyl = null;
    private PropCylZ propcylz = null;
    private PropXYXY propxyxy = null;
    private PropDCAZ propdcaz = null;
    private PropDCAXY propdcaxy = null;
    private PropXYDCA propxydca = null;
//    private PropZDCA propzdca     = null;
    // The master propagator that can go between any pair of surfaces.
    private PropDispatch pDispatch = null;
    // The run time configuration system.
    //ToyConfig config;
    // Information from the run time configuration system.
//    double  respixel;
//    double  resstrip;
//    boolean trackerbarrel2d;         
//    double  zres2dTrackerBarrel;
//    boolean vtxFwdEquivStrips;
    // Does this detector have forward tracking.
    boolean hasforward = false;
    // Stuff for adding surfaces
    static ArrayList physicalVolumes = new ArrayList();
    ILogicalVolume logical;
    ShapeDispatcher shapeDispatcher = new ShapeDispatcher();
    Detector detector = null;

    public KalmanGeom(Detector det) {
        detector = det;
        System.out.println("New detector: " + detector.getName());
        logical = detector.getTrackingVolume().getLogicalVolume();

        
        // Extract information from the run time configuration system.
        //	try{
        //	    ToyConfig config = ToyConfig.getInstance();
        //	    respixel            = config.getDouble("respixel");
        //	    resstrip            = config.getDouble("resstrip");
        //	    trackerbarrel2d     = config.getBoolean("trackerbarrel2d");
        //	    zres2dTrackerBarrel = config.getDouble("zres2dTrackerBarrel");
        //	    vtxFwdEquivStrips   = config.getBoolean("vtxFwdEquivStrips");
        //
        //	} catch (ToyConfigException e){
        //            System.out.println (e.getMessage() );
        //            System.out.println ("Stopping now." );
        //            System.exit(-1);
        //        }



        //	Map<String, Subdetector> subDetMap = detector.getSubdetectors();
        //
        //	Subdetector sd_tbar = subDetMap.get("TrackerBarrel");
        //	Subdetector sd_vbar = subDetMap.get("VertexBarrel");
        //	Subdetector sd_tec  = subDetMap.get("TrackerEndcap");
        //	Subdetector sd_vec  = subDetMap.get("VertexEndcap");
        //	Subdetector sd_tfor = subDetMap.get("TrackerForward");
        //	Subdetector sd_bpipe  = subDetMap.get("BeamPipe");
        //	System.out.println ("Checking .... " + sd_tbar + " | " + sd_tfor);
        // Don't use subdetectors anymore...

        //	 Check for forward tracking system.
        //	if ( sd_tfor == null ) {
        //	    System.out.println ("Checking 1 .... " );
        //	    if ( detector.getName().compareTo("sid00") != 0 ){
        //		System.out.println("Expected to find a TrackerForward Subdetector but did not!");
        //		System.exit(-1);
        //	    }
        //	}else{
        //	    System.out.println ("Checking 2  .... " );
        //
        //	    hasforward = true;
        //	}           // I may want to implement something like this at some point but not now.

        //	if ( hasforward ){
        //	    de_tfor = sd_tfor.getDetectorElement();
        //	}

        /* Cycle through detector and add all surfaces to the surface list. */
        //    addAllSurfaces();

        System.out.println("Number of surfaces: " + Surf.size());
        for (KalmanSurface surf : Surf) {
            surf.Print();
        }


        double[] origin = {0., 0., 0.};

//    	Map<String,Field> fields = detector.getFields();
//    	Set<String> keys = fields.keySet();
//    	for ( String key : keys ){
//    	    Field field = fields.get(key);
//    	    String classname = field.getClass().getName();
//    	    String shortname = classname.replaceAll( "org.lcsim.geometry.field.", "");
//    	    if ( shortname.compareTo("Solenoid") != 0 ){
//    		System.out.println("Expected, but did not find, a solenoid: " + shortname );
////    		System.exit(-1);
//    	    }
//    	    Solenoid s = (Solenoid)field;
//    	    bz = s.getInnerField()[2];
//    	    if ( bz == 0. ){
//    		System.out.println("This code will not work with a magnetic field of 0: " + shortname );
////    		System.exit(-1);
//    	    }
//    	    break;
//    	}

        // Instantiate surface-pair specific propagators.
        propcyl = new PropCyl(bz);
        propzz = new PropZZ(bz);
        propdcacyl = new PropDCACyl(bz);
        propcyldca = new PropCylDCA(bz);
        propzcyl = new PropZCyl(bz);
        propcylz = new PropCylZ(bz);
        propxyxy = new PropXYXY(bz);
        propdcaxy = new PropDCAXY(bz);
        propxydca = new PropXYDCA(bz);
        //	propdcaz   = new PropDCAZ(bz);
        //	propzdca   = new PropZDCA(bz);
        // Again, need xy plane propagators!

        // Instantiate and configure the general purpose propagator.
        pDispatch = new PropDispatch();
        pDispatch.addPropagator(SurfZPlane.staticType(), SurfZPlane.staticType(), propzz);
        pDispatch.addPropagator(SurfCylinder.staticType(), SurfCylinder.staticType(), propcyl);
        pDispatch.addPropagator(SurfDCA.staticType(), SurfCylinder.staticType(), propdcacyl);
        pDispatch.addPropagator(SurfCylinder.staticType(), SurfDCA.staticType(), propcyldca);
        pDispatch.addPropagator(SurfZPlane.staticType(), SurfCylinder.staticType(), propzcyl);
        pDispatch.addPropagator(SurfCylinder.staticType(), SurfZPlane.staticType(), propcylz);
        pDispatch.addPropagator(SurfXYPlane.staticType(), SurfXYPlane.staticType(), propxyxy);
        pDispatch.addPropagator(SurfDCA.staticType(), SurfXYPlane.staticType(), propdcaxy);
        pDispatch.addPropagator(SurfXYPlane.staticType(), SurfDCA.staticType(), propxydca);
        //	pDispatch.addPropagator( SurfDCA.staticType(),      SurfZPlane.staticType(),   propdcaz);
        //	pDispatch.addPropagator( SurfZPlane.staticType(),   SurfDCA.staticType(),      propzdca);

    }

    public Propagator newPropagator() {
        // Clone not supported for pDispatch.
        //return pDispatch.newPropagator();
        return (Propagator) pDispatch;
    }

//    public List<RKSurf> getCylinders(){
//	return Surf;
//    }
//    public List<RKSurf> getZplus(){
//	return ZSurfplus;
//    }
//
//    public List<RKSurf> getZMinus(){
//	return ZSurfminus;
//    }   
    public double getBz() {
        return bz;
    }

    public void setBz(double bfield) {
        bz = bfield;
    }

//   Return a list of z surfaces, going foward along the track.
//    public List<RKSurf> getZ( double z0, double cz ){
//	List<RKSurf> zlist = new ArrayList<RKSurf>();
//	if ( cz > 0 ){
//	    for ( Iterator ihit=ZSurfAll.iterator(); ihit.hasNext(); ){
//		RKSurf s = (RKSurf) ihit.next();
//		if ( s.zc >= z0 ){
//		    zlist.add(s);
//		}
//	    }
//	} else if ( cz < 0 ){
//	    for ( ListIterator ihit=ZSurfAll.listIterator(ZSurfAll.size());
//		  ihit.hasPrevious(); ){
//		RKSurf s = (RKSurf) ihit.previous();
//		if ( s.zc <= z0 ){
//		    zlist.add(s);
//		}
//	    }
//	}
//
//	return zlist;
//    }  
    // Adds all surfaces to the surface list
    private void addAllSurfaces() {
        addDaughterSurfaces(logical);
    }

    private void addDaughterSurfaces(ILogicalVolume logical) {
        if (logical.getDaughters().size() == 0) {
            // need to avoid making surface for target. for now, flag..
            flag++;
            if (flag > 5) {
                surf = shapeDispatcher.getKalmanSurf(logical.getSolid());
                Surf.add(surf);
            }
        }
        for (int n = 0; n < logical.getNumberOfDaughters(); n++) {
            IPhysicalVolume physical = logical.getDaughter(n);
            addDaughterSurfaces(physicalToLogical(physical));
            physicalVolumes.remove(physicalVolumes.size() - 1);
        }
    }

    // Given a point, find the solid that contains it. Doesn't work.
    private ISolid findSolidFromPoint(Point3D hitPoint) {
        ISolid solid = checkDaughterSurfaces(logical, hitPoint);
        return solid;
    }

    private ISolid checkDaughterSurfaces(ILogicalVolume logical, Point3D hitPoint) {
        if (logical.getDaughters().size() == 0) {
            if (pointIsOnSolid(logical.getSolid(), hitPoint)) {
                return logical.getSolid();
            }
        }
        for (int n = 0; n < logical.getNumberOfDaughters(); n++) {
            IPhysicalVolume physical = logical.getDaughter(n);
            checkDaughterSurfaces(physicalToLogical(physical), hitPoint);
            physicalVolumes.remove(physicalVolumes.size() - 1);
        }
        System.out.print("This hit isn't on a solid!");
        return null;
    }

    // Given a TrackerHit, return the surface the hit should be placed on. Doesn't work.
    private KalmanSurface findTrackerHitSurface(TrackerHit thit) {
        double[] position = thit.getPosition();
        Point3D hitPoint = new Point3D(position[0], position[1], position[2]);
        ISolid hitSolid = findSolidFromPoint(hitPoint);
        KalmanSurface hitSurf = getKSurfFromSolid(hitSolid);
        return hitSurf;
    }

    private KalmanSurface getKSurfFromSolid(ISolid hitSolid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Adds the physical volume to the physicalVolumes ArrayList.
    private ILogicalVolume physicalToLogical(IPhysicalVolume physical) {
        physicalVolumes.add(physical.getTransform());
        return physical.getLogicalVolume();
    }

    // Adds intercepts between a HTrack and the detector. Doesn't work.
    public void addIntercepts(HTrack ht, Detector detector) {
        logical = detector.getTrackingVolume().getLogicalVolume();
        addDaughterIntercepts(logical, ht);
    }

    private void addDaughterIntercepts(ILogicalVolume logical, HTrack ht) {
        if (logical.getDaughters().size() == 0) {
            ht = shapeDispatcher.addIntercept(logical.getSolid(), ht);
        }
        for (int n = 0; n < logical.getNumberOfDaughters(); n++) {
            IPhysicalVolume physical = logical.getDaughter(n);
            addDaughterIntercepts(physicalToLogical(physical), ht);
            physicalVolumes.remove(physicalVolumes.size() - 1);
        }
    }

    // Add a hit from an lcsim Track to a trf HTrack.
    public HTrack addTrackerHit(TrackerHit thit, HTrack htrack, HelicalTrackFit track,
            VTrack vtrack) {
        /* This should check the kind of solid that got hit and model it as the
         * correct surface based on the solid dimensions. For now it just
         * assumes XY planes. */

        double[] position = thit.getPosition();
        if (position.length != 3) {
            System.out.println("Position has more than 3 coordinates?!");
        }


//        // For checking surfaces
//        KalmanSurface surface= findTrackerHitSurface(thit);
//        PropStat prop = new PropStat();
//        prop.setForward();
//        double path = (position[2] - track.z0())/track.slope();
//        KalmanHit khit = new KalmanHit(track, surface, vtrack, prop, path);
//        Hit hit = khit.MakeHit();
//        Point3D hitPoint = new Point3D(position[0], position[1], position[2]);
//        ISolid hitSolid = findSolidFromPoint(hitPoint);

        
        //PELLE:
        // The HelicalTrackFit track and VTrack vtrack are not used
        
        
        
        // Need an ETrack to add hits from clusters.
        ETrack tre = htrack.newTrack();

        // This also assumes the hit in question is a Helical Track Cross.
        HelicalTrackCross htrackcross = (HelicalTrackCross) thit;
        HelicalTrackStrip strip1 = (HelicalTrackStrip) htrackcross.getStrips().get(0);
        HelicalTrackStrip strip2 = (HelicalTrackStrip) htrackcross.getStrips().get(1);

        double dist1 = dotProduct(strip1.origin(), strip1.w());
        double dist2 = dotProduct(strip2.origin(), strip2.w());


        double phi1 = Math.atan2(strip1.w().y() * dist1 * strip1.origin().y(), strip1.w().x() * dist1 * strip1.origin().x());
        double phi2 = Math.atan2(strip2.w().y() * dist2 * strip2.origin().y(), strip2.w().x() * dist2 * strip2.origin().x());
        // phi needs to be between 0 and 2PI. Additionally, it seemed like
        // rounding errors caused problems for very small negative values of phi,
        // so these are approximated as 0.
        if (phi1 < 0) {
            if (phi1 > -0.00000001) {
                phi1 = 0;
            } else {
                phi1 = phi1 + 2 * Math.PI;
            }
        }
        if (phi2 < 0) {
            if (phi2 > -0.00000001) {
                phi2 = 0;
            } else {
                phi2 = phi2 + 2 * Math.PI;
            }
        }

        System.out.println("Origin of strip1: " + strip1.origin());
        System.out.println("u,v,w of strip1 : " + strip1.u().toString() + "," + strip1.v().toString() + "," + strip1.w().toString());
        System.out.println("Origin of strip2: " + strip2.origin());
        System.out.println("u,v,w of strip2 : " + strip2.u().toString() + "," + strip2.v().toString() + "," + strip2.w().toString());
        System.out.println("dist1: " + dist1 + ", phi1: " + phi1);
        System.out.println("dist2: " + dist2 + ", phi2: " + phi2);
        // ClusXYPlane needs wz, wv, avz, phi and dist to create a cluster.
        double wz1 = strip1.u().z();
        double wz2 = strip2.u().z();
        double wv1 = strip1.u().x() * (-Math.sin(phi1)) + strip1.u().y() * Math.cos(phi1);
        double wv2 = strip2.u().x() * (-Math.sin(phi2)) + strip2.u().y() * Math.cos(phi2);


        double avz1 = strip1.umeas()
                - (dist1 * (Math.cos(phi1) * strip1.u().x() + Math.sin(phi1) * strip1.u().y())
                - (strip1.origin().x() * strip1.u().x() + strip1.origin().y() * strip1.u().y()
                + strip1.origin().z() * strip1.u().z()));
        double avz2 = strip2.umeas()
                - (dist2 * (Math.cos(phi2) * strip2.u().x() + Math.sin(phi2) * strip2.u().y())
                - (strip2.origin().x() * strip2.u().x() + strip2.origin().y() * strip2.u().y()
                + strip2.origin().z() * strip2.u().z()));
        double davz1 = strip1.du();
        double davz2 = strip2.du();

        System.out.println("avz1 = " + avz1 + " = " + strip1.umeas() + " - " + (dist1 * (Math.cos(phi1) * strip1.u().x() + Math.sin(phi1) * strip1.u().y())
                - (strip1.origin().x() * strip1.u().x() + strip1.origin().y() * strip1.u().y()
                + strip1.origin().z() * strip1.u().z())));
        System.out.println("avz2 = " + avz2 + " = " + strip2.umeas() + " - " + (dist2 * (Math.cos(phi2) * strip2.u().x() + Math.sin(phi2) * strip2.u().y())
                - (strip2.origin().x() * strip2.u().x() + strip2.origin().y() * strip2.u().y()
                + strip2.origin().z() * strip2.u().z())));
        


//        // Just to check and make sure Vtrf makes sense:
//        if((dist1*Math.cos(phi1) - strip1.origin().x())*Math.cos(phi1) ==
//                (dist1*Math.sin(phi1) - strip1.origin().y())*Math.sin(phi1)){
//            System.out.println("Vtrf1 coherent!");
//        } else{
//            System.out.println("Vtrf1 doesn't make sense :(");
//        }
//        if((dist2*Math.cos(phi2) - strip2.origin().x())*Math.cos(phi2) ==
//                (dist2*Math.sin(phi2) - strip2.origin().y())*Math.sin(phi2)){
//            System.out.println("Vtrf2 coherent!");
//        } else{
//            System.out.println("Vtrf2 doesn't make sense :(");
//        }

        System.out.println("wz1 = " + wz1 + ", wv1 = " + wv1 + ", avz1 = " + avz1 + ", davz1 =" + davz1);
        System.out.println("wz2 = " + wz2 + ", wv2 = " + wv2 + ", avz2 = " + avz2 + ", davz2 =" + davz2);

        // Create new clusters and get hit predictions.
        ClusXYPlane1 cluster1 = new ClusXYPlane1(dist1, phi1, wv1, wz1, avz1, davz1);
        ClusXYPlane1 cluster2 = new ClusXYPlane1(dist2, phi2, wv2, wz2, avz2, davz2);
        List hits1 = cluster1.predict(tre);
        List hits2 = cluster2.predict(tre);
        Hit hit1 = (Hit) hits1.get(0);
        Hit hit2 = (Hit) hits2.get(0);

        System.out.println("hit1 predicted vector: " + hit1.predictedVector());
        System.out.println("hit2 predicted vector: " + hit2.predictedVector());
        System.out.println("hit position from trackerhit: [" + thit.getPosition()[0] + ", "
                + thit.getPosition()[1] + ", " + thit.getPosition()[2] + "]");

        hit1.setParentPointer(cluster1);
        hit2.setParentPointer(cluster2);
        htrack.addHit(hit1);
        htrack.addHit(hit2);

        return htrack;

    }
    
    
     // Add a hit from an lcsim Track to a trf HTrack.
    public HTrack addTrackerHit(TrackerHit thit, HTrack htrack) {
        /* This should check the kind of solid that got hit and model it as the
         * correct surface based on the solid dimensions. For now it just
         * assumes XY planes. */
        
        System.out.println("\nAdd Tracker Hit at position " + thit.getPosition()[0]  + "," + thit.getPosition()[1] + "," + thit.getPosition()[2]);
         
        //PELLE:
        // The HelicalTrackFit track and VTrack vtrack are not used so was removed 
        // to avoid confusion.
        // They might be needed to be able to figure out what solid to 
        // to extrapolate between if this gets implemented.
        
        

        double[] position = thit.getPosition();
        if (position.length != 3) {
            System.out.println("Position has more than 3 coordinates?!");
        }


//        // For checking surfaces
//        KalmanSurface surface= findTrackerHitSurface(thit);
//        PropStat prop = new PropStat();
//        prop.setForward();
//        double path = (position[2] - track.z0())/track.slope();
//        KalmanHit khit = new KalmanHit(track, surface, vtrack, prop, path);
//        Hit hit = khit.MakeHit();
//        Point3D hitPoint = new Point3D(position[0], position[1], position[2]);
//        ISolid hitSolid = findSolidFromPoint(hitPoint);

       
        
        
        // Need an ETrack to add hits from clusters.
        ETrack tre = htrack.newTrack();

        // This also assumes the hit in question is a Helical Track Cross.
        HelicalTrackCross htrackcross = (HelicalTrackCross) thit;
        HelicalTrackStrip strip1 = (HelicalTrackStrip) htrackcross.getStrips().get(0);
        HelicalTrackStrip strip2 = (HelicalTrackStrip) htrackcross.getStrips().get(1);

        Hep3Vector u1 = strip1.u();
        Hep3Vector u2 = strip2.u();
        Hep3Vector v1 = strip1.v();
        Hep3Vector v2 = strip2.v();
        Hep3Vector w1 = strip1.w();
        Hep3Vector w2 = strip2.w();
        
        Hep3Vector origin1 = strip1.origin();
        Hep3Vector origin2 = strip2.origin();
        
        double u1meas = strip1.umeas();
        double u2meas = strip2.umeas();
        
        
        System.out.println("strip1 origin: " + origin1);
        System.out.println("strip2 origin: " + origin2);
        System.out.printf("strip1 u=%s\tv=%s\tw=%s\n",u1.toString(),v1.toString(),w1.toString());
        System.out.printf("strip2 u=%s\tv=%s\tw=%s\n",u2.toString(),v2.toString(),w2.toString());
        System.out.println("strip1 umeas: " + u1meas);
        System.out.println("strip2 umeas: " + u2meas);

        System.out.println("Rotate strip2 u,v,w into strip1 frame");
        
        Hep3Matrix strip2ToTrk = getStripToTrackRotation(strip2);
        System.out.println("Strip2ToTrk matrix " + strip2ToTrk.toString());
        System.out.println("Get the rotation matrix for going from track frame to strip1 frame");
        Hep3Matrix trackToStrip1 = getTrackToStripRotation(strip1);        
        Hep3Matrix strip2ToStrip1 = VecOp.mult(trackToStrip1,strip2ToTrk);

        
        u2 = VecOp.mult(strip2ToStrip1, u2);
        v2 = VecOp.mult(strip2ToStrip1, v2);
        w2 = VecOp.mult(strip2ToStrip1, w2);
        
        System.out.printf("strip2 u=%s\tv=%s\tw=%s\n",u2.toString(),v2.toString(),w2.toString());
        
        
        
        double dist1 = dotProduct(origin1, w1);
        double dist2 = dotProduct(origin2, w2);


        double phi1 = Math.atan2(w1.y() * dist1 * origin1.y(), w1.x() * dist1 * origin1.x());
        double phi2 = Math.atan2(w2.y() * dist2 * origin2.y(), w2.x() * dist2 * origin2.x());
        // phi needs to be between 0 and 2PI. Additionally, it seemed like
        // rounding errors caused problems for very small negative values of phi,
        // so these are approximated as 0.
        if (phi1 < 0) {
            if (phi1 > -0.00000001) {
                phi1 = 0;
            } else {
                phi1 = phi1 + 2 * Math.PI;
            }
        }
        if (phi2 < 0) {
            if (phi2 > -0.00000001) {
                phi2 = 0;
            } else {
                phi2 = phi2 + 2 * Math.PI;
            }
        }
        System.out.println("dist1: " + dist1 + ", phi1: " + phi1);
        System.out.println("dist2: " + dist2 + ", phi2: " + phi2);
        // ClusXYPlane needs wz, wv, avz, phi and dist to create a cluster.
        double wz1 = u1.z();
        double wz2 = u2.z();
        double wv1 = u1.x() * (-Math.sin(phi1)) + u1.y() * Math.cos(phi1);
        double wv2 = u2.x() * (-Math.sin(phi2)) + u2.y() * Math.cos(phi2);


        double avz1 = u1meas
                - (dist1 * (Math.cos(phi1) * u1.x() + Math.sin(phi1) * u1.y())
                - (origin1.x() * u1.x() + origin1.y() * u1.y()
                + origin1.z() * u1.z()));
        double avz2 = u2meas
                - (dist2 * (Math.cos(phi2) * u2.x() + Math.sin(phi2) * u2.y())
                - (origin2.x() * u2.x() + origin2.y() * u2.y()
                + origin2.z() * u2.z()));
        double davz1 = strip1.du();
        double davz2 = strip2.du();

        System.out.println("avz1 = " + avz1 + " = " + u1meas + " - " + (dist1 * (Math.cos(phi1) * u1.x() + Math.sin(phi1) * u1.y())
                - (origin1.x() * u1.x() + origin1.y() * u1.y()
                + origin1.z() * u1.z())));
        System.out.println("avz2 = " + avz2 + " = " + u2meas + " - " + (dist2 * (Math.cos(phi2) * u2.x() + Math.sin(phi2) * u2.y())
                - (origin2.x() * u2.x() + origin2.y() * u2.y()
                + origin2.z() * u2.z())));
        


//        // Just to check and make sure Vtrf makes sense:
//        if((dist1*Math.cos(phi1) - origin1.x())*Math.cos(phi1) ==
//                (dist1*Math.sin(phi1) - origin1.y())*Math.sin(phi1)){
//            System.out.println("Vtrf1 coherent!");
//        } else{
//            System.out.println("Vtrf1 doesn't make sense :(");
//        }
//        if((dist2*Math.cos(phi2) - origin2.x())*Math.cos(phi2) ==
//                (dist2*Math.sin(phi2) - origin2.y())*Math.sin(phi2)){
//            System.out.println("Vtrf2 coherent!");
//        } else{
//            System.out.println("Vtrf2 doesn't make sense :(");
//        }

        System.out.println("wz1 = " + wz1 + ", wv1 = " + wv1 + ", avz1 = " + avz1 + ", davz1 =" + davz1);
        System.out.println("wz2 = " + wz2 + ", wv2 = " + wv2 + ", avz2 = " + avz2 + ", davz2 =" + davz2);

        // Create new clusters and get hit predictions.
        ClusXYPlane1 cluster1 = new ClusXYPlane1(dist1, phi1, wv1, wz1, avz1, davz1);
        ClusXYPlane1 cluster2 = new ClusXYPlane1(dist2, phi2, wv2, wz2, avz2, davz2);
        List hits1 = cluster1.predict(tre);
        List hits2 = cluster2.predict(tre);
        Hit hit1 = (Hit) hits1.get(0);
        Hit hit2 = (Hit) hits2.get(0);

        System.out.println("hit1 predicted vector: " + hit1.predictedVector());
        System.out.println("hit2 predicted vector: " + hit2.predictedVector());
        System.out.println("hit position from trackerhit: [" + thit.getPosition()[0] + ", "
                + thit.getPosition()[1] + ", " + thit.getPosition()[2] + "]");

        hit1.setParentPointer(cluster1);
        hit2.setParentPointer(cluster2);
        htrack.addHit(hit1);
        htrack.addHit(hit2);

        return htrack;

    }
    
    
    
    

    private boolean pointIsOnSolid(ISolid solid, Point3D hitPoint) {
        return shapeDispatcher.pointIsOnSolid(solid, hitPoint);
    }

    private double dotProduct(Hep3Vector v1, Hep3Vector v2) {
        double dotProduct = v1.x() * v2.x() + v1.y() * v2.y() + v1.z() * v2.z();
        return dotProduct;
    }


    private Hep3Matrix getStripToTrackRotation(HelicalTrackStrip strip) {
        //This function transforms the vec to the track coordinates that the supplied strip has
        
        //Transform from strip frame (u,v,w) to JLab frame (done through the RawTrackerHit)
        ITransform3D stripToDet = GetLocalToGlobal(strip);
        //Get rotation matrix
        Hep3Matrix stripToDetMatrix = (BasicHep3Matrix) stripToDet.getRotation().getRotationMatrix();
        //Transformation between JLab and tracking coordinates
        Hep3Matrix detToTrackMatrix = (BasicHep3Matrix) HPSTransformations.getMatrix();
        
        if (true) {
            System.out.println("Getting the rotation to go from strip (u,v,w) to track coordinates");
            System.out.println("stripToDet (JLab) translation:");
            System.out.println(stripToDet.getTranslation().toString());
            System.out.println("stripToDet Rotation:");
            System.out.println(stripToDet.getRotation().toString());
            System.out.println("detToTrack Rotation:");
            System.out.println(detToTrackMatrix.toString());
            
            
        }

        return (Hep3Matrix) VecOp.mult(detToTrackMatrix,stripToDetMatrix);
    }

    private ITransform3D GetLocalToGlobal(HelicalTrackStrip strip) {
        //Transform from sensor frame (u,v,w) to tracking frame
        RawTrackerHit rth = (RawTrackerHit) strip.rawhits().get(0);
        IDetectorElement ide = rth.getDetectorElement();
        SiSensor sensor = ide.findDescendants(SiSensor.class).get(0);
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        return electrodes.getLocalToGlobal();
    }
    
    private Hep3Matrix getTrackToStripRotation(HelicalTrackStrip strip) {
        //This function transforms the hit to the sensor coordinates
        
        //Transform from JLab frame to sensor frame (done through the RawTrackerHit)
        ITransform3D detToStrip = GetGlobalToLocal(strip);
        //Get rotation matrix
        Hep3Matrix detToStripMatrix = (BasicHep3Matrix) detToStrip.getRotation().getRotationMatrix();
        //Transformation between the JLAB and tracking coordinate systems
        Hep3Matrix detToTrackMatrix = (BasicHep3Matrix) HPSTransformations.getMatrix();

        if (true) {
            System.out.println("Getting the rotation to go from track to strip (u,v,w)");
            System.out.println("gblToLoc translation:");
            System.out.println(detToStrip.getTranslation().toString());
            System.out.println("gblToLoc Rotation:");
            System.out.println(detToStrip.getRotation().toString());
            System.out.println("detToTrack Rotation:");
            System.out.println(detToTrackMatrix.toString());
        }

        return (Hep3Matrix) VecOp.mult(detToStripMatrix, VecOp.inverse(detToTrackMatrix));
    }

    private ITransform3D GetGlobalToLocal(HelicalTrackStrip strip) {
        //Transform from JLab frame (RawTrackerHit) to sensor frame (i.e. u,v,w)
        RawTrackerHit rth = (RawTrackerHit) strip.rawhits().get(0);
        IDetectorElement ide = rth.getDetectorElement();
        SiSensor sensor = ide.findDescendants(SiSensor.class).get(0);
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        return electrodes.getGlobalToLocal();
    }


}
