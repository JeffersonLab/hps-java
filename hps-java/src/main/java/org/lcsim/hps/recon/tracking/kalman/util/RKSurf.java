package org.lcsim.hps.recon.tracking.kalman.util;

import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.solids.ISolid;
import org.lcsim.detector.solids.Tube;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.PolyconeSupport;
import org.lcsim.geometry.subdetector.PolyconeSupport.ZPlane;
import org.lcsim.material.Material;
import org.lcsim.recon.tracking.trfcyl.BSurfCylinder;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfcyl.ThinCylMs;
import org.lcsim.recon.tracking.trfcyl.ThinCylMsSim;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 * 
 * Holds summary information about one sensitive surface.  The information
 * is extracted from the org.lcsim geometry system.  This is a routine
 * that needs to know that org.lcsim uses mm for distances but TRF uses cm.
 *
 *
 *@author $Author: jeremy $
 *@version $Id: RKSurf.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class RKSurf{

    // TRF wants distances in cm, not mm.
    private double mmTocm = 0.1;

    // Name of this Subdetector.
    public String name = "";

    // Readout dimension:
    // 0 = inactive
    // 1 = strips
    // 2 = pixels
    public int rodim;

    // References to subdetector and detector element info.
    public Subdetector      sd = null;
    public IDetectorElement de = null;

    public double radius = 0.;
    public double thick  = 0.;
    public double zmin   = 0.;
    public double zmax   = 0.;
    public double zc     = 0.;
    public double rmin   = 0.;
    public double rmax   = 0.;

    // Does a 1D measurement measure the first or second coordinate?
    public int ixy = -1;
    static public int ixy_Undef = -1;
    static public int ixy_x   = 0;
    static public int ixy_y   = 1;
    static public int ixy_phi = 0;
    static public int ixy_z   = 1;

    private double[] resolutions;

    // Different inputs have different places to find materials.
    public Material   material = null;
    public IMaterial imaterial = null;

    // Name of the material.
    public String matname = "Undefined";

    // Radiation length of the material;
    public double radl = 0;

    // Thickness in units of radiation length.
    public double thick_radl = 0.;

    // Density.
    public double density = 0.0;

    // Tolerance for comparing floating point numbers.
    private double tolerance = 1.e-4;

    // Types
    private int type;
    // Types
    static public int type_tube=0;
    static public int type_zdisc=1;

    public SurfCylinder getCylinder(){
	if ( type != type_tube ){
	    System.out.println("Cannot return a cylinder from a non-tube surface: " + name );
	    System.exit(-1);
	}
	SurfCylinder s    = new SurfCylinder( radius*mmTocm );
	ThinCylMs    scat = new ThinCylMs   ( thick_radl );
	ThinCylMsSim sim  = new ThinCylMsSim( thick_radl );
	s.setInteractor(scat);
	s.setSimInteractor(sim);
	return s;
    }

    public BSurfCylinder getBCylinder(){
	if ( type != type_tube ){
	    System.out.println("Cannot return a bounded cylinder from a non-tube surface: " + name );
	    System.exit(-1);
	}
	return new BSurfCylinder( radius*mmTocm, zmin*mmTocm, zmax*mmTocm );
    }

    public boolean inBounds ( double trfarg ){
	if ( type == type_tube ){
	    double z = trfarg/mmTocm;
	    return (z>=zmin)&&(z<=zmax);
	} else if ( type == type_zdisc ){
	    double r = trfarg/mmTocm;
	    return (r>=rmin)&&(r<=rmax);
	}
	return false;
    }

    public int getType(){
	return type;
    }

    public String getTypeAsString(){
	if ( type == type_tube ){
	    return "Tube ";
	} else if ( type == type_zdisc ){
	    return "Zdisc";
	}
	return "Unknown";
    }

    public SurfZPlane getZPlane(){
	if ( type != type_zdisc ){
	    System.out.println("Cannot return a ZPlane from a non-zplane surface: " + name );
	    System.exit(-1);
	}
	return new SurfZPlane( zc*mmTocm );
    }

    public double[] getResolutions(){
	return resolutions;
    }

    public double getResolution0(){
	boolean ok = true;
	if ( resolutions == null ){
	    ok = false;
	} else {
	    if ( resolutions.length < 1 ){
		ok = false;
	    }
	}
	if ( !ok ){
	    System.out.println ("RKSurf: Illegal request for getResolution0 " + name );
	    return 0.;
	}
	return resolutions[0];
    }

    public double getResolution1(){
	boolean ok = true;
	if ( resolutions == null ){
	    ok = false;
	} else {
	    if ( resolutions.length < 1 ){
		ok = false;
	    }
	}
	if ( !ok ){
	    System.out.println ("RKSurf: Illegal request for getResolution1 " + name );
	    return 0.;
	}
	return resolutions[1];
    }


    public RKSurf( IDetectorElement de, int rodim, int ixy, double[] res ){
	if ( de == null ){
	    System.out.println("RKSurf: Cannot instantiate RKSurf with null DetectorElement." );
	    System.exit(-1);
	}

	this.name  = de.getName();
	this.rodim = rodim;
	this.ixy   = ixy;
	resolutions = new double[res.length];
	for ( int i=0; i<res.length; ++i){
	    resolutions[i] = res[i]*mmTocm;
	}
	Build( de);
    }

    public RKSurf( IDetectorElement de, int rodim, int ixy, double res ){
	if ( de == null ){
	    System.out.println("RKSurf: Cannot instantiate RKSurf with null DetectorElement." );
	    System.exit(-1);
	}

	this.name  = de.getName();
	this.rodim = rodim;
	this.ixy   = ixy;
	resolutions = new double[1];
	resolutions[0] = res*mmTocm;
	Build( de);
    }

    public RKSurf( IDetectorElement de, int rodim, int ixy, double res0, double res1 ){
	if ( de == null ){
	    System.out.println("RKSurf: Cannot instantiate RKSurf with null DetectorElement." );
	    System.exit(-1);
	}

	this.name  = de.getName();
	this.rodim = rodim;
	this.ixy   = ixy;
	resolutions = new double[2];
	resolutions[0] = res0*mmTocm;
	resolutions[1] = res1*mmTocm;
	Build( de);
    }



    public RKSurf( Subdetector sd ){
	if ( sd == null ){
	    System.out.println("RKSurf: Cannot instantiate RKSurf with null Subdetector." );
	    System.exit(-1);
	}

	this.sd = sd;
	rodim   = 0;
	name    = sd.getName();
	Build( sd );
    }
    
    // Adjust z position.  Used in the equivstrips model.
    public void hackZc( double dz){
	zc+=dz;
    }

    public void Print(){
	if ( type == type_tube ){
	    System.out.printf ( "%-40s %1d %3d %10.2f %10.2f %10.2f %10.2f %-20s %10.2f %10.6f %10.6f\n",
				name,
				rodim, 
				ixy,
				radius,
				thick,
				zmin,
				zmax,
				matname,
				radl,
				thick_radl,
				density
				);
	} else {
	    System.out.printf ( "%-40s %1d %3d %10.2f %10.2f %10.2f %10.2f %-20s %10.2f %10.6f %10.6f\n",
				name,
				rodim, 
				ixy,
				zc,
				thick,
				rmin,
				rmax,
				matname,
				radl,
				thick_radl,
				density
				);
	}
    }


    private void Build( Subdetector sd ){

	String classname  = sd.getClass().getName();
	String shortname  = classname.replaceAll("org.lcsim.geometry.subdetector.","");

	if ( shortname.compareTo("PolyconeSupport") == 0 ) {
	    PolyconeSupport pc = (PolyconeSupport) sd;
	    AddPolycone(pc);

	} else {
	    System.out.println("RKSurf: Do not know how to add this subdetector: "
			       + name + " " + classname);
	    System.exit(-1);
	}

	matname = material.getName();
	radl    = material.getRadiationLength();

	thick_radl = thick/radl;

    }

    private void Build( IDetectorElement de ){
	
	//System.out.println ("Starting: " + name );
	IGeometryInfo g   = de.getGeometry();
	if ( g == null ){
	    System.out.println("Missing geometry for detector element: " + name );
	    System.exit(-1);
	}
	ILogicalVolume lv = g.getLogicalVolume();
	if ( lv == null ){
	    System.out.println("Missing logical volume for detector element: " + name );
	    System.exit(-1);
	}
	IMaterial mat     = lv.getMaterial();
	ISolid solid      = lv.getSolid();
	Hep3Vector center = g.getPosition();

	String solidname  = getSolidTypeName(solid);

	if ( solidname.compareTo("Tube") == 0  ){
	    Tube tube = (Tube) solid;
	    if ( tube.getZHalfLength() > 1.0 ){
		AddTube( tube, center );
	    }else{
		AddZDisc( tube, center);
	    }
	    
	} else {
	    System.out.println ( "RKSurf: Do not recognize this shape for this DetectorElement: "
				 + solidname + " " + name );
	    System.exit(-1);
	}
	matname = mat.getName();
	radl    = mat.getRadiationLength();
	density = mat.getDensity();

	thick_radl = thick/radl;

    }

    private void AddPolycone ( PolyconeSupport pc ){
	type = type_tube;
	List<ZPlane> zplanes = pc.getZPlanes();
	boolean ok = false;
	for ( int i=1; i<zplanes.size(); ++i ){
	    ZPlane zp1 = zplanes.get(i-1);
	    ZPlane zp2 = zplanes.get(i);
	    if ( zp1.getZ() < 0. && zp2.getZ() > 0. ){
		if ( Math.abs(zp1.getRMax()-zp1.getRMax()) > tolerance ||
		     Math.abs(zp1.getRMin()-zp1.getRMin()) > tolerance    ){
		    System.out.println ("RKSurf: Mismatched radii in beampipe: "
					+ name );
		}
		radius = 0.5*(zp1.getRMax() + zp1.getRMin());
		thick  = zp1.getRMax() - zp1.getRMin();
		zmin   = zp1.getZ();
		zmax   = zp2.getZ();
		ok = true;
	    }
	}

	if ( !ok ){
	    System.out.println( "RKSurf: Could not parse PolyconeSupport for subdetector"
				+ name );
	    System.exit(-1);
	}

	// Depracated but replacement appears not to be in place.
	material = pc.getMaterial();

	radl = material.getRadiationLength();
	density = material.getDensity();

    }

    private void AddTube ( Tube tube, Hep3Vector center ){
	type = type_tube;
	if ( Math.abs( center.x()) > tolerance ||
	     Math.abs( center.y()) > tolerance ||
	     Math.abs( center.z()) > tolerance    ){
	    System.out.println("RKSurf: Only do tubes centered on (0.0,0.0,0.) " );
	    System.exit(-1);
	}

	zmin   = -tube.getZHalfLength();
	zmax   = tube.getZHalfLength();
	radius = 0.5*(tube.getOuterRadius()+tube.getInnerRadius());
	thick  = tube.getOuterRadius()-tube.getInnerRadius();

    }

    private void AddZDisc ( Tube tube, Hep3Vector center ){
	type   = type_zdisc;
	zmin   = center.z()-tube.getZHalfLength();
	zmax   = center.z()+tube.getZHalfLength();
	radius = 0.5*(tube.getOuterRadius()+tube.getInnerRadius());
	thick  = 2.*tube.getZHalfLength();
	
	zc     = center.z();
	rmin   = tube.getInnerRadius();
	rmax   = tube.getOuterRadius();
    }



    // Utility function to extract a short version of the class name. 
    private String getSolidTypeName( ISolid s ){
	String fullname = s.getClass().getName();
	String name     = fullname.replaceAll( "org.lcsim.detector.solids.", "");
	return name;
    }

}
