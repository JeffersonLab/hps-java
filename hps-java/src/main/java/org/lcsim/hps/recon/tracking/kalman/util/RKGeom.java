package org.lcsim.hps.recon.tracking.kalman.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IDetectorElementContainer;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Field;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.field.Solenoid;
import org.lcsim.recon.tracking.trfbase.PropDispatch;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfcyl.PropCyl;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfcylplane.PropCylZ;
import org.lcsim.recon.tracking.trfcylplane.PropZCyl;
import org.lcsim.recon.tracking.trfdca.PropCylDCA;
import org.lcsim.recon.tracking.trfdca.PropDCACyl;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trfzp.PropZZ;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 * 
 * Extract needed information from the geometry system and serve it 
 * to the callers in a convenient form.
 *  
 *
 *@author $Author: jeremy $
 *@version $Id: RKGeom.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class RKGeom {

    // TRF wants distances in cm, not mm.
    private double mmTocm = 0.1;

    //    private Array radius = null;
    private Subdetector sd_tbar  = null;
    private Subdetector sd_vbar  = null;
    private Subdetector sd_tec   = null;
    private Subdetector sd_vec   = null;
    private Subdetector sd_tfor  = null;
    private Subdetector sd_bpipe = null;

    private IDetectorElement de_tbar  = null;
    private IDetectorElement de_vbar  = null;
    private IDetectorElement de_tec   = null;
    private IDetectorElement de_vec   = null;
    private IDetectorElement de_tfor  = null;

    // Nominal magnetic field.
    public double bz = 0.;

    // Lists of interesting surfaces.
    public List<RKSurf> Surf = new ArrayList<RKSurf>();
    public List<RKSurf> ZSurfplus  = new ArrayList<RKSurf>();
    public List<RKSurf> ZSurfminus = new ArrayList<RKSurf>();
    public List<RKSurf> ZSurfAll   = new ArrayList<RKSurf>();

    // Specific surface type A to surface type B propagators.
    private PropCyl propcyl       = null;
    private PropZZ  propzz        = null;
    private PropDCACyl propdcacyl = null;
    private PropCylDCA propcyldca = null;
    private PropZCyl propzcyl     = null;
    private PropCylZ propcylz     = null;
    private PropDCAZ propdcaz     = null;
    private PropZDCA propzdca     = null;

    // The master propagator that can go between any pair of surfaces.
    private PropDispatch pDispatch = null;

    // The run time configuration system.
    //ToyConfig config;

    // Information from the run time configuration system.
    double  respixel;
    double  resstrip;
    boolean trackerbarrel2d;
    double  zres2dTrackerBarrel;
    boolean vtxFwdEquivStrips;

    // Does this detector have forward tracking.
    boolean hasforward = false;

    public RKGeom ( Detector detector ){
	
	System.out.println("New detector: " + detector.getName() );
	if ( detector.getName().compareTo("sid00") != 0 &&
	     detector.getName().compareTo("sid02") != 0    ){
	    System.out.println("This driver works only with sid00 or sid02." );
	    System.exit(-1);
	}

	// Extract information from the run time configuration system.
	try{
	    ToyConfig config = ToyConfig.getInstance();
	    respixel            = config.getDouble("respixel");
	    resstrip            = config.getDouble("resstrip");
	    trackerbarrel2d     = config.getBoolean("trackerbarrel2d");
	    zres2dTrackerBarrel = config.getDouble("zres2dTrackerBarrel");
	    vtxFwdEquivStrips   = config.getBoolean("vtxFwdEquivStrips");

	} catch (ToyConfigException e){
            System.out.println (e.getMessage() );
            System.out.println ("Stopping now." );
            System.exit(-1);
        }



	Map<String, Subdetector> subDetMap = detector.getSubdetectors();
	
	Subdetector sd_tbar = subDetMap.get("TrackerBarrel");
	Subdetector sd_vbar = subDetMap.get("VertexBarrel");
	Subdetector sd_tec  = subDetMap.get("TrackerEndcap");
	Subdetector sd_vec  = subDetMap.get("VertexEndcap");
	Subdetector sd_tfor = subDetMap.get("TrackerForward");
	Subdetector sd_bpipe  = subDetMap.get("BeamPipe");
	System.out.println ("Checking .... " + sd_tbar + " | " + sd_tfor);

	// Check for forward tracking system.
	if ( sd_tfor == null ) {
	    System.out.println ("Checking 1 .... " );
	    if ( detector.getName().compareTo("sid00") != 0 ){
		System.out.println("Expected to find a TrackerForward Subdetector but did not!");
		System.exit(-1);
	    }
	}else{
	    System.out.println ("Checking 2  .... " );

	    hasforward = true;
	}

	// Other parts must be present.
	if ( sd_tbar == null ){
	    System.out.println("Could not find TrackerBarrel Subdetector.");
	    System.exit(-1);
	}
	if (sd_vbar == null ){
	    System.out.println("Could not find VertexBarrel Subdetector.");
	    System.exit(-1);
	}
	if ( sd_tec == null ){
	    System.out.println("Could not find TrackerEndcap Subdetector.");
	    System.exit(-1);
	}
	if ( sd_vec == null ){
	    System.out.println("Could not find VertexEndcap Subdetector.");
	    System.exit(-1);
	}
	if ( sd_bpipe == null ){
	    System.out.println("Could not find BeamPipe Subdetector.");
	    System.exit(-1);
	}

	de_tbar  = sd_tbar.getDetectorElement();
	de_vbar  = sd_vbar.getDetectorElement();
	de_tec   = sd_tec.getDetectorElement();
	de_vec   = sd_vec.getDetectorElement();

	if ( hasforward ){
	    de_tfor = sd_tfor.getDetectorElement();
	}

	if ( de_tbar == null ){
	    System.out.println("Could not find TrackerBarrel Detector Element.");
	    System.exit(-1);
	}
	if (de_vbar == null ){
	    System.out.println("Could not find VertexBarrel Detector Element.");
	    System.exit(-1);
	}
	if ( de_tec == null ){
	    System.out.println("Could not find TrackerEndcap Detector Element.");
	    System.exit(-1);
	}
	if ( de_vec == null ){
	    System.out.println("Could not find VertexEndcap Detector Element.");
	    System.exit(-1);
	}
	if ( de_tfor == null && hasforward ){
	    System.out.println("Could not find TrackerForward Detector Element.");
	    System.exit(-1);
	}


	IDetectorElementContainer tbar_layers = de_tbar.getChildren();
	IDetectorElementContainer vbar_layers = de_vbar.getChildren();
	IDetectorElementContainer tec_layers  = de_tec.getChildren();
	IDetectorElementContainer vec_layers  = de_vec.getChildren();
	IDetectorElementContainer tfor_layers = de_tfor.getChildren();

	// Add the beampipe to the list.
	Surf.add( new RKSurf ( sd_bpipe ) );

	// Add the vertex and tracker barrels to the list.
	for ( IDetectorElement de: vbar_layers ){
	    Surf.add( new RKSurf ( de, 2, RKSurf.ixy_Undef, respixel, respixel ) );
	}
	for ( IDetectorElement de: tbar_layers ){
	    if ( trackerbarrel2d ) {
		Surf.add( new RKSurf ( de, 2, RKSurf.ixy_phi, resstrip, zres2dTrackerBarrel ) );
	    }else{
		Surf.add( new RKSurf ( de, 1, RKSurf.ixy_phi, resstrip ) );
	    }
	}

	System.out.println( "Number of surfaces: " + Surf.size() );
	for ( RKSurf surf : Surf ){
	    surf.Print();
	}

	if ( vec_layers.size() != 2 ){
	    System.out.println ("Expected two z hemispheres for: " + de_vec.getName () );
	    System.exit(-1);
	}

	if ( tec_layers.size() != 2 ){
	    System.out.println ("Expected two z hemispheres for: " + de_vec.getName () );
	    System.exit(-1);
	}

	if ( tfor_layers.size() != 2 ){
	    System.out.println ("Expected two z hemispheres for: " + de_tfor.getName () );
	    System.exit(-1);
	}

	for ( IDetectorElement de: vec_layers.get(0).getChildren() ){
	    if ( vtxFwdEquivStrips ){
		ZSurfplus.add( new RKSurf( de, 1, RKSurf.ixy_x, resstrip) );

		//ZSurfplus.add( new RKSurf( de, 1, RKSurf.ixy_y, resstrip) );
		RKSurf tmp = new RKSurf( de, 1, RKSurf.ixy_y, resstrip);
		tmp.hackZc(-0.1);
		ZSurfplus.add(tmp);

	    }
	    else{
		ZSurfplus.add( new RKSurf( de, 2, RKSurf.ixy_Undef, respixel, respixel) );
	    }

	}

	for ( IDetectorElement de: tfor_layers.get(0).getChildren() ){
	    ZSurfplus.add( new RKSurf( de, 2, RKSurf.ixy_Undef, respixel, respixel) );
	}

	int xy = -1;
	for ( IDetectorElement de: tec_layers.get(0).getChildren() ){
	    if ( (++xy)%2 == 0 ){
		ZSurfplus.add( new RKSurf( de, 1, RKSurf.ixy_x, resstrip) );
	    }else{
		ZSurfplus.add( new RKSurf( de, 1, RKSurf.ixy_y, resstrip) );
	    }
	}

	for ( IDetectorElement de: vec_layers.get(1).getChildren() ){
	    if ( vtxFwdEquivStrips ){
		ZSurfminus.add( new RKSurf( de, 1, RKSurf.ixy_x, resstrip) );
		//ZSurfminus.add( new RKSurf( de, 1, RKSurf.ixy_y, resstrip) );
		RKSurf tmp = new RKSurf( de, 1, RKSurf.ixy_y, resstrip);
		tmp.hackZc(+0.1);
		ZSurfminus.add(tmp);
	    }
	    else{
		ZSurfminus.add( new RKSurf( de, 2, RKSurf.ixy_Undef, respixel, respixel) );
	    }
	}

	for ( IDetectorElement de: tfor_layers.get(1).getChildren() ){
	    ZSurfminus.add( new RKSurf( de, 2, RKSurf.ixy_Undef, respixel, respixel) );
	}


	xy = -1;
	for ( IDetectorElement de: tec_layers.get(1).getChildren() ){
	    if ( (++xy)%2 == 0 ){
		ZSurfminus.add( new RKSurf( de, 1, RKSurf.ixy_x, resstrip) );
	    } else {
		ZSurfminus.add( new RKSurf( de, 1, RKSurf.ixy_y, resstrip) );
	    }
	}

	System.out.println( "Number of +Zsurfaces: " + ZSurfplus.size() );
	for ( RKSurf surf : ZSurfplus ){
	    surf.Print();
	}

	System.out.println( "Number of -Zsurfaces: " + ZSurfminus.size() );
	for ( RKSurf surf : ZSurfminus ){
	    surf.Print();
	}

	// List of all Z Surfaces.
	for ( ListIterator ihit=ZSurfminus.listIterator(ZSurfminus.size());
	      ihit.hasPrevious(); ){
	    ZSurfAll.add((RKSurf)ihit.previous());
	}
	for ( Iterator ihit=ZSurfplus.iterator(); ihit.hasNext(); ){
	    ZSurfAll.add((RKSurf)ihit.next());
	}


	double[] origin = {0.,0.,0.};

	Map<String,Field> fields = detector.getFields();
	Set<String> keys = fields.keySet();
	for ( String key : keys ){
	    Field field = fields.get(key);
	    String classname = field.getClass().getName();
	    String shortname = classname.replaceAll( "org.lcsim.geometry.field.", "");
	    if ( shortname.compareTo("Solenoid") != 0 ){
		System.out.println("Expected, but did not find, a solenoid: " + shortname );
		System.exit(-1);
	    }
	    Solenoid s = (Solenoid)field;
	    bz = s.getInnerField()[2];
	    if ( bz == 0. ){
		System.out.println("This code will not work with a magnetic field of 0: " + shortname );
		System.exit(-1);
	    }
	    break;
	}

	// Instantiate surface-pair specific propagators.
	propcyl    = new PropCyl(bz);
	propzz     = new PropZZ(bz);
	propdcacyl = new PropDCACyl(bz);
	propcyldca = new PropCylDCA(bz);
	propzcyl   = new PropZCyl(bz);
	propcylz   = new PropCylZ(bz);
	propdcaz   = new PropDCAZ(bz);
	propzdca   = new PropZDCA(bz);

	// Instantiate and configure the general purpose propagator.
	pDispatch = new PropDispatch();
	pDispatch.addPropagator( SurfZPlane.staticType(),   SurfZPlane.staticType(),   propzz);
	pDispatch.addPropagator( SurfCylinder.staticType(), SurfCylinder.staticType(), propcyl);
	pDispatch.addPropagator( SurfDCA.staticType(),      SurfCylinder.staticType(), propdcacyl);
	pDispatch.addPropagator( SurfCylinder.staticType(), SurfDCA.staticType(),      propcyldca);
	pDispatch.addPropagator( SurfZPlane.staticType(),   SurfCylinder.staticType(), propzcyl);
	pDispatch.addPropagator( SurfCylinder.staticType(), SurfZPlane.staticType(),   propcylz);
	pDispatch.addPropagator( SurfDCA.staticType(),      SurfZPlane.staticType(),   propdcaz);
	pDispatch.addPropagator( SurfZPlane.staticType(),   SurfDCA.staticType(),      propzdca);

    }

    public Propagator newPropagator(){
	// Clone not supported for pDispatch.
	//return pDispatch.newPropagator();
	return (Propagator)pDispatch;
    }

    public List<RKSurf> getCylinders(){
	return Surf;
    }
    public List<RKSurf> getZplus(){
	return ZSurfplus;
    }
    
    public List<RKSurf> getZMinus(){
	return ZSurfminus;
    }

    public double getBz(){
	return bz;
    }
    

    // Return a list of z surfaces, going foward along the track.
    public List<RKSurf> getZ( double z0, double cz ){
	List<RKSurf> zlist = new ArrayList<RKSurf>();
	if ( cz > 0 ){
	    for ( Iterator ihit=ZSurfAll.iterator(); ihit.hasNext(); ){
		RKSurf s = (RKSurf) ihit.next();
		if ( s.zc >= z0 ){
		    zlist.add(s);
		}
	    }
	} else if ( cz < 0 ){
	    for ( ListIterator ihit=ZSurfAll.listIterator(ZSurfAll.size());
		  ihit.hasPrevious(); ){
		RKSurf s = (RKSurf) ihit.previous();
		if ( s.zc <= z0 ){
		    zlist.add(s);
		}
	    }
	}

	return zlist;
    }
}
