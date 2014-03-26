package org.hps.users.mgraham;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.recon.tracking.kalman.KalmanSurface;
import org.hps.recon.tracking.kalman.PropDCAXY;
import org.hps.recon.tracking.kalman.PropXYDCA;
import org.hps.recon.tracking.kalman.ShapeDispatcher;
import org.hps.recon.tracking.kalman.util.PropDCAZ;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IReadout;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.identifier.IIdentifier;
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
import org.lcsim.recon.tracking.trfxyp.ClusXYPlane2;
import org.lcsim.recon.tracking.trfxyp.HitXYPlane2;
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
 *@author $Author: mgraham $
 *@version $Id: KalmanGeom.java,v 1.5 2011/11/16 18:00:04 mgraham Exp $
 *
 * Date $Date: 2011/11/16 18:00:04 $
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
    boolean _DEBUG=false;
//      boolean _DEBUG=true;
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

        // Need an ETrack to add hits from clusters.
        ETrack tre = htrack.newTrack();

        // This also assumes the hit in question is a Helical Track Cross.
        HelicalTrackCross htrackcross = (HelicalTrackCross) thit;
        HelicalTrackStrip strip1 = (HelicalTrackStrip) htrackcross.getStrips().get(0);
        HelicalTrackStrip strip2 = (HelicalTrackStrip) htrackcross.getStrips().get(1);

        double dist1 = dotProduct(strip1.origin(), strip1.w());
        double dist2 = dotProduct(strip2.origin(), strip2.w());
      
            if(_DEBUG)System.out.println("TrackerHit Position = [" + thit.getPosition()[0] + "," + thit.getPosition()[1] + "," + thit.getPosition()[2]);




        /*
        double phi1 = Math.atan2(strip1.w().y()*dist1*strip1.origin().y(), strip1.w().x()*dist1*strip1.origin().x());
        double phi2 = Math.atan2(strip2.w().y()*dist2*strip2.origin().y(), strip2.w().x()*dist2*strip2.origin().x());
        // phi needs to be between 0 and 2PI. Additionally, it seemed like
        // rounding errors caused problems for very small negative values of phi,
        // so these are approximated as 0.
        if (phi1 < 0){
        if (phi1 > -0.00000001){
        phi1 = 0;
        } else{
        phi1 = phi1 + 2*Math.PI;
        }
        }
        if(phi2 < 0 ){
        if(phi2 > -0.00000001) {
        phi2 = 0;
        } else{
        phi2 = phi2 + 2*Math.PI;
        }
        }
         */
        //set phis to 0 ... normal parallel to x-axis (mis-alignment can change this)
        double phi1 = 0;
        double phi2 = 0;

        if(_DEBUG){
        System.out.println("Origin of strip1: " + strip1.origin());
        System.out.println("Origin of strip2: " + strip2.origin());
        System.out.println("dist1: " + dist1 + ", phi1: " + phi1);
        System.out.println("dist2: " + dist2 + ", phi2: " + phi2);
        System.out.println("Strip 1 Measurement   "+strip1.umeas());
        System.out.println("Strip 2 Measurement   "+strip2.umeas());
        }
        /*
        // ClusXYPlane needs wz, wv, avz, phi and dist to create a cluster.
        double wz1 = strip1.u().z();
        double wz2 = strip2.u().z();
        double wv1 = strip1.u().x() * (-Math.sin(phi1)) + strip1.u().y() * Math.cos(phi1);
        double wv2 = strip2.u().x() * (-Math.sin(phi2)) + strip2.u().y() * Math.cos(phi2);         
         */
/*

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

*/
//        if(_DEBUG)System.out.println("wz1 = " + wz1 + ", wv1 = " + wv1 + ", avz1 = " + avz1 + ", davz1 =" + davz1);
//        if(_DEBUG)System.out.println("wz2 = " + wz2 + ", wv2 = " + wv2 + ", avz2 = " + avz2 + ", davz2 =" + davz2);
//        ClusXYPlane1 cluster1 = new ClusXYPlane1(dist1, phi1, wv1, wz1, avz1, davz1);
//        ClusXYPlane1 cluster2 = new ClusXYPlane1(dist2, phi2, wv2, wz2, avz2, davz2);

        //Creat a ClusXYPlane2 ... needs v(=y),z, dy,dz
        Hep3Vector sorigin1 = strip1.origin();
        Hep3Vector u1 = strip1.u();
        double umeas1 = strip1.umeas();
        Hep3Vector uvec1 = VecOp.mult(umeas1, u1);
        Hep3Vector clvec1 = VecOp.add(sorigin1, uvec1);
        double dumeas1 = strip1.du();
        double dustrip1 = (strip1.vmax() - strip1.vmin()) / Math.sqrt(12);

        Hep3Vector sorigin2 = strip2.origin();
        Hep3Vector u2 = strip2.u();
        double umeas2 = strip2.umeas();
        Hep3Vector uvec2 = VecOp.mult(umeas2, u2);
        Hep3Vector clvec2 = VecOp.add(sorigin2, uvec2);
        double dumeas2 = strip2.du();
        double dustrip2 = (strip2.vmax() - strip2.vmin()) / Math.sqrt(12);

        double z1 = clvec1.z();
        double z2 = clvec2.z();
        //this is the non-z distance of the cluster (in our case, phi=0, so it's just y)
        double v1 = clvec1.x() * (-Math.sin(phi1)) + clvec1.y() * Math.cos(phi1);
        double v2 = clvec2.x() * (-Math.sin(phi2)) + clvec2.y() * Math.cos(phi2);

        //transform umeas, ustrip to z and v
        ITransform3D t1 = getLocalToGlobal((RawTrackerHit) strip1.rawhits().get(0));
        ITransform3D t2 = getLocalToGlobal((RawTrackerHit) strip2.rawhits().get(0));
        SymmetricMatrix tmpcov1 = new SymmetricMatrix(3);
        tmpcov1.setElement(0, 0, dumeas1 * dumeas1);
        tmpcov1.setElement(2, 2, dustrip1 * dustrip1);
        SymmetricMatrix tmpcov2 = new SymmetricMatrix(3);
        tmpcov2.setElement(0, 0, dumeas2 * dumeas2);
        tmpcov2.setElement(2, 2, dustrip2 * dustrip2);



        t1.rotate(tmpcov1);
        t2.rotate(tmpcov2);
        double dz1 = tmpcov1.e(2, 2);
        double dz2 = tmpcov2.e(2, 2);
        double dv1 = tmpcov1.e(1, 1);
        double dv2 = tmpcov2.e(1, 1);

        double dzv1 = tmpcov1.e(1, 2);
        double dzv2 = tmpcov2.e(1, 2);
        if(_DEBUG){
            System.out.println("z1 = " + z1 + "+/-" + Math.sqrt(dz1) + ", v1 = " + v1 + "+/-" + Math.sqrt(dv1) + ", cov(z,v) = " + dzv1);
            System.out.println("z2 = " + z2 + "+/-" + Math.sqrt(dz2) + ", v2 = " + v2 + "+/-" + Math.sqrt(dv2) + ", cov(z,v) = " + dzv2);
        }
//        ClusXYPlane2 cluster1 = new ClusXYPlane2(dist1, phi1, v1, z1, dv1, dz1,dzv1);
//        ClusXYPlane2 cluster2 = new ClusXYPlane2(dist2, phi2, v2, z2, dv2, dz2,dzv2);

        ClusXYPlane2 cluster1 = new ClusXYPlane2(dist1 * mmTocm, phi1, v1 * mmTocm, z1 * mmTocm, dv1 * mmTocm * mmTocm, dz1 * mmTocm * mmTocm, dzv1 * mmTocm * mmTocm);
        ClusXYPlane2 cluster2 = new ClusXYPlane2(dist2 * mmTocm, phi2, v2 * mmTocm, z2 * mmTocm, dv2 * mmTocm * mmTocm, dz2 * mmTocm * mmTocm, dzv2 * mmTocm * mmTocm);
        // Create new clusters and get hit predictions.
        
        List hits1 = cluster1.predict(tre);
        List hits2 = cluster2.predict(tre);
        HitXYPlane2 hit1 = (HitXYPlane2) hits1.get(0);
        HitXYPlane2 hit2 = (HitXYPlane2) hits2.get(0);
        hit1.setParentPointer(cluster1);
        hit2.setParentPointer(cluster2);
        if(_DEBUG){
            System.out.println("Cluster 1:\n"+cluster1.toString());
        System.out.println("Cluster 2:\n"+cluster2.toString());
           System.out.println("hit1 predicted vector: " + hit1.toString());
          System.out.println("hit2 predicted vector: " + hit2.toString());
            System.out.println("hit position from trackerhit: [" + thit.getPosition()[0] + ", " +
                thit.getPosition()[1] + ", " + thit.getPosition()[2] + "]");
        }

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

    private ITransform3D getLocalToGlobal(RawTrackerHit raw_hit) {
        ITransform3D foobar = null;
        IIdentifier id = raw_hit.getIdentifier();

        IDetectorElement detector_de = detector.getDetectorElement();
        Set<SiSensor> _process_sensors = new HashSet<SiSensor>();
        _process_sensors.addAll(detector_de.findDescendants(SiSensor.class));
        for (SiSensor sensor : _process_sensors) {
            SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
            IReadout readout = sensor.getReadout();
            List<RawTrackerHit> raw_hits = readout.getHits(RawTrackerHit.class);
            for (RawTrackerHit rh : raw_hits)
                if (rh.equals(raw_hit))
                    return ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal();
        }
        System.out.println("KalmanGeom:  Didn't find the hit!");
        return foobar;
    }
}
