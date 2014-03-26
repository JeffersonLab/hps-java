package org.hps.recon.tracking.kalman.util;

/**
 * 
 * Parameters to control RKTrackGen.
 *
 *@author $Author: jeremy $
 *@version $Id: RKTrackGenParams.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class RKTrackGenParams {

    // Limits for track parameter generation.
    private double ptmin =      1.00;
    private double ptmax =     10.00;
    private double czmin =     -0.95;
    private double czmax =      0.95;
    private double phimin = -Math.PI;
    private double phimax =  Math.PI;
    private double d0min =     -4.00;
    private double d0max =      4.00; 
    private double z0min =    -10.00;
    private double z0max =     10.00;

    // Control of charge generation:
    //  +1  = generate only charge +1
    //  -1  = generate only charge -1
    // else = generate both charges +1 and -1 in equal amounts.
    int chargeControl = 0;

    // Enable end cap mode for cz generation.
    // That is, generate cz uniformly over:
    //         czmin< cz <=  czmax
    //   and  -czmax< cz <= -czmin
    // The default mode is to generate uniformly over:
    //    czmin< cz <=  czmax
    boolean czEndcapMode = false;

    // Strength of solenoidal magnetic field on z axis.
    private double bz = 5.00;

    public RKTrackGenParams(){
    }

    public RKTrackGenParams( ToyConfig config ){

	// Override default values using values specified in the
	// runtime configuration file, if present.
	ptmin   = config.getDouble( "ptmin",  ptmin);
	ptmax   = config.getDouble( "ptmax",  ptmax);
	czmin   = config.getDouble( "czmin",  czmin);
	czmax   = config.getDouble( "czmax",  czmax);
	phimin  = config.getDouble( "phimin", phimin);
	phimax  = config.getDouble( "phimax", phimax);
	d0min   = config.getDouble( "d0min",  d0min);
	d0max   = config.getDouble( "d0max",  d0max);
	z0min   = config.getDouble( "z0min",  z0min);
	z0max   = config.getDouble( "z0max",  z0max);

	bz            = config.getDouble ( "bz",            bz);
	chargeControl = config.getInt    ( "chargeControl", chargeControl);
	czEndcapMode  = config.getBoolean( "czEndcapMode",  czEndcapMode);

    }

    public RKTrackGenParams(
			    double ptmin,
			    double ptmax,
			    double czmin,
			    double czmax,
			    double phimax,
			    double phimin,
			    double d0min,
			    double d0max,
			    double z0min,
			    double z0max,
			    double bz,
			    int chargeControl
			    ){
	this.ptmin  = ptmin;
	this.ptmax  = ptmax;
	this.czmin  = czmin;
	this.czmax  = czmax;
	this.phimax = phimax;
	this.phimin = phimin;
	this.d0min  = d0min;
	this.d0max  = d0max;
	this.z0min  = z0min;
	this.z0max  = z0max;
	this.bz     = bz;
	this.chargeControl = chargeControl;
    }

    public RKTrackGenParams( RKTrackGenParams par ){
	ptmin  = par.ptmin;
	ptmax  = par.ptmax;
	czmin  = par.czmin;
	czmax  = par.czmax;
	phimax = par.phimax;
	phimin = par.phimin;
	d0min  = par.d0min;
	d0max  = par.d0max;
	z0min  = par.z0min;
	z0max  = par.z0max;
	bz     = par.bz;
	chargeControl = par.chargeControl;
    }


    public void setptmin(double ptmin){
	this.ptmin=ptmin;
    }

    public void setptmax(double ptmax){
	this.ptmax=ptmax;
    }

    public void setczmin(double czmin){
	this.czmin=czmin;
    }

    public void setczmax(double czmax){
	this.czmax=czmax;
    }

    public void setphimin(double phimin){
	this.phimin=phimin;
    }

    public void setphimax(double phimax){
	this.phimax=phimax;
    }

    public void setd0min(double d0min){
	this.d0min=d0min;
    }

    public void setd0max(double d0max){
	this.d0max=d0max;
    }

    public void setz0min(double z0min){
	this.z0min=z0min;
    }

    public void setz0max(double z0max){
	this.z0max=z0max;
    }

    public void setbz(double bz){
	this.bz=bz;
    }

    public void setchargeControl(int c){
	chargeControl=c;
    }

    public double ptmin(){ return ptmin;}
    public double ptmax(){ return ptmax;}

    public double czmin(){ return czmin;}
    public double czmax(){ return czmax;}

    public double phimin(){ return phimin;}
    public double phimax(){ return phimax;}

    public double d0min(){ return d0min;}
    public double d0max(){ return d0max;}

    public double z0min(){ return z0min;}
    public double z0max(){ return z0max;}

    public double bz(){ return bz;}

    public double chargeControl(){ return chargeControl;}

    public String toString(){
	StringBuffer s = new StringBuffer();
	s.append("Parameters for track generation: \n");
	s.append(String.format( "Pt:   (%10.4f, %10.4f) (GeV)\n", ptmin,  ptmax ));
	s.append(String.format( "cz:   (%10.4f, %10.4f)\n", czmin,  czmax ));
	s.append(String.format( "Phi0: (%10.4f, %10.4f)\n", phimin, phimax ));
	s.append(String.format( "d0:   (%10.4f, %10.4f) (mm)\n", d0min,  d0max ));
	s.append(String.format( "z0:   (%10.4f, %10.4f) (mm)\n", z0min,  z0max ));
	if ( chargeControl == 1 || chargeControl == -1 ){
	    s.append(String.format( "Charge %3d only.  ", chargeControl ));
	} else{
	    s.append("Charge +1 and -1 equally.  ");
	}
	if ( czEndcapMode ){
	    s.append("Endcap mode for cos(theta).\n");
	} else{
	    s.append("Normal mode for cos(theta).\n");
	}

	s.append(String.format("BField strength %10.2f (T)\n", bz));
	return s.toString();
    }
}
