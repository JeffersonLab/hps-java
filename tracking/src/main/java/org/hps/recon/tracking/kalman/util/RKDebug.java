package org.hps.recon.tracking.kalman.util;

import org.lcsim.recon.tracking.trfbase.PropDir;

/**
 * 
 * A way to propagate debug info to low level routines.
 *
 *@author $Author: jeremy $
 *@version $Id: RKDebug.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class RKDebug {

    static private int track = -1;
    static private PropDir pdir;
    static private RKTrack rkt;
    static private int start_type;

    static private double msfac    = 1.;
    static private boolean doms    = true;
    static private boolean doeloss = true;

    static private boolean printoutliers = false;

    static private RKDebug instance = null;

    // A "common block" to pass info around.
    static private double degen     = 0.;

    private RKDebug(){
    }

    static public RKDebug Instance(){
	if ( instance == null ){
	    instance = new RKDebug();
	}
	return instance;
    }

    static public int getTrack(){
	return track;
    }
    static public void setTrack( int trk){
	track = trk;
    }

    static public PropDir getPropDir(){
	return pdir;
    }
    static public void setPropDir( PropDir dir){
	pdir = dir;
    }

    static public RKTrack getRKTrack(){
	return rkt;
    }
    static public void setRKTrack( RKTrack t){
	rkt = t;
    }

    static public int getStartType(){
	return start_type;
    }
    static public void setStartType( int type){
	start_type = type;
    }

    static public double getMsFac(){
	return msfac;
    }
    static public void setMsFac( double f){
	msfac = f;
    }

    static public boolean getDoMs(){
	return doms;
    }
    static public void setDoMs( boolean b){
	doms = b;
    }

    static public boolean getDoEloss(){
	return doeloss;
    }
    static public void setDoEloss( boolean b){
	doeloss = b;
    }

    static public boolean getPrintOutliers(){
	return printoutliers;
    }
    static public void setPrintOutliers( boolean b){
	printoutliers = b;
    }

    static public double getDeGen(){
	return degen;
    }
    static public void setDeGen( double b){
	degen = b;
    }

}
