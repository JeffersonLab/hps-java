package org.hps.recon.tracking.kalman.util;

import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 * 
 * Define integer codes, and corresponding strings, to identify a type of surface.
 *
 *@author $Author: jeremy $
 *@version $Id: SurfaceCode.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class SurfaceCode {

    static final String[] namelist = { "Unknown", "Cylinder", "ZPlane", "DCA" };

    private int code=0;

    public SurfaceCode ( Surface s ){
	if ( s.pureType().equals( SurfCylinder.staticType() ) ){
	    code = 1;
	} else if ( s.pureType().equals( SurfZPlane.staticType() ) ){
	    code = 2;
	} else if ( s.pureType().equals( SurfDCA.staticType() ) ){
	    code = 3;
	}
	
    }

    public int getCode(){
	return code;
    }

    public String getName(){
	return namelist[code];
    }
}
