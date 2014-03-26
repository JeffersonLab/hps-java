package org.hps.recon.tracking.kalman.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *  
 * Holds a list of RKHits.  Can return the hits sorted in different
 * orders.  Can return properties of the ensemble of hits.
 *
 *@author $Author: jeremy $
 *@version $Id: RKHitList.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

public class RKHitList {

    // Original list.
    private List<RKHit> list = new ArrayList<RKHit>();

    // Sorted copies of the lists.
    private List<RKHit> forward  = null;
    private List<RKHit> backward = null;

    private int nmeas  = 0;
    private int nzmeas = 0;

    private int ncyl = 0;
    private int nzp  = 0;

    RKHitList (){
    }
    
    public void addHit( RKHit h ){
	list.add(h);
	nmeas  += h.getRodim();
	nzmeas += h.getZdim();

	int type = h.getSurface().getType();
	if ( type == RKSurf.type_tube ){
	    ncyl += 1;
	} else if ( type == RKSurf.type_zdisc ){
	    nzp += 1;
	}

	// Any existing sorts are no longer valid.
	forward  = null;
	backward = null;
    }

    public RKHit get( int i){
	return list.get(i);
    }

    public List<RKHit> getForward(){
	if( forward !=  null ) return forward;
	forward = new ArrayList<RKHit>(list);
	Collections.sort( forward, new CompareRKHit<RKHit>(+1) );	
	return forward;
    }

    public List<RKHit> getBackward(){
	if ( backward != null ) return backward;
	backward = new ArrayList<RKHit>(list);
	Collections.sort( backward, new CompareRKHit<RKHit>(-1) );	
	return backward;
    }

    public int nHits(){ return list.size(); }
    public int nDof(){ return nDof(5); }
    public int nDof( int n){ return nmeas-n; }

    public int nZ(){ return nzmeas;}

    public int nCyl(){ return ncyl;}
    public int nZp() { return nzp;}

    // Does the track have enough measurements to be fittable.
    // This is an arbitrary definition.  
    public boolean fitable(){
	if ( nDof() < 1 ) return false;
	if ( nzmeas < 3 ) return false;
	return true;
    }


    public RKHit outerMostHit(){
	for ( RKHit h : getBackward() ){
	    if ( h.getRodim() > 0 ) return h;
	}
	return null;
    }

    public RKHit innerMostHit(){
	for ( RKHit h : getForward() ){
	    if ( h.getRodim() > 0 ) return h;
	}
	return null;
    }

    public int nLeadingStrips(){
	int n = 0;
	int nLeadingStrips = 0;
	int firstZType = 0;
	boolean firstZ = false;
	for ( RKHit h: getBackward() ){
	    RKSurf s  = h.getSurface();
	    int rodim = h.getRodim();
	    int zdim  = h.getZdim();
	    if ( zdim > 0 ){
		firstZ = true;
		if ( s.getType() == RKSurf.type_tube ){
		    firstZType = 1;
		} else{
		    firstZType = 2;
		}
		break;
	    }
	    if ( rodim == 1 & 
		 s.getType() == RKSurf.type_tube ){
		++nLeadingStrips;
	    }
	    
	}
	return nLeadingStrips;
    }


    public int firstZType(){
	int type = 0;
	for ( RKHit h: getBackward() ){
	    RKSurf s  = h.getSurface();
	    int zdim  = h.getZdim();
	    if ( zdim > 0 ){
		if ( s.getType() == RKSurf.type_tube ){
		    type = 1;
		} else{
		    type = 2;
		}
		break;
	    }
	}
	return type;
    }

    public void PrintBackward(){
	for ( RKHit h: getBackward() ){
	    RKSurf s  = h.getSurface();
	    s.Print();
	    /*
	    int zdim  = h.getZdim();
	    int rodim = h.getRodim();
	    String ctype = "";
	    double zc = 0.;
	    if ( rodim > 0 ){
		if ( s.getType() == RKSurf.type_tube ){
		    ctype = "Barrel";
		} else{
		    ctype = "Endcap";
		    zc = s.zc;
		}
		System.out.printf ( "Hit: %6s %4d %4d %9.2f %9.2f\n", 
				    ctype,
				    rodim,
				    s.ixy,
                                    h.getPath(),
				    zc
				    );
		
	    } else{
		s.Print();
	    }
	    */
	}
    }

    public double deltaZ(){
	RKHit  h0  = getBackward().get(0); 
	RKSurf s0  = h0.getSurface();
	int    ro0 = h0.getRodim();

	// Only consider tracks that start with zdisc strips.
	if ( s0.getType() != RKSurf.type_zdisc ) return 0.;
	if ( ro0 != 1 ) return 0.;

	// z of first z measuring surface.
	double z0  = s0.zc;

	// z of first z measuring surface with enough lever arm to be useful.
	double z1  = z0;

	int n = 0;
	for ( RKHit h: getBackward() ){
	    
	    // Start looking at the third measurement.
            // ie skip second plane if it is part of a doublet.
	    if ( ++n < 3 ) continue;

	    RKSurf s  = h.getSurface();
	    if ( s.getType() == RKSurf.type_zdisc  ){
		// Accept any z disc.
		z1 = s.zc;
		break;
	    } else if ( h.getRodim() > 1 ) {
		// Accept barrel pixels.
		z1 = h.getVTrack().vector(1);
		break;
	    }

	}

	return Math.abs(z0-z1);
    }

    // Look for patterns of z hits that can cause problems.
    public int Pattern(){

	// Number of endcap hits before the first barrel hit.
	int nec = 0;
	for ( RKHit h: getBackward() ){
	    RKSurf s  = h.getSurface();
	    if ( s.getType() == RKSurf.type_zdisc  ){
		++nec;
	    }else{
		break;
	    }
	}

	// The first hit is a barrel hit.
	if ( nec == 0 ) return 0;

	// There is are hits from at least 2 z stations so at least
	// one slope will be well defined.
	if ( nec > 2  ) return 0;

	int nbar  = 0;
	int nskip = 0;
	for ( RKHit h: getBackward() ){
	    if ( ++nskip <= nec ) continue;

	    RKSurf s  = h.getSurface();
	    if ( s.getType() == RKSurf.type_tube  ){
		++nbar;
	    }else{
		break;
	    }
	}

	// Number of barrel hits between the first z hit and 
	return nbar;

    }

    // ??? This should be in the RKHits definition, not in this file????
    //
    // Class to compare two RKHits, sorted by path length.
    // If the argument is positive, then the sort is done in increasing order.
    // If the argument is negative, then the sort is done in decreasing order.
    private class CompareRKHit<T> implements Comparator<T>{
	private int sign = 1;
	public CompareRKHit(){
	}
	public CompareRKHit(int sign){
	    this.sign = sign;
	}
	public int compare ( Object o1, Object o2 ){
	    RKHit h1 = (RKHit)o1;
	    RKHit h2 = (RKHit)o2;
	    double l1 = h1.getPath();
	    double l2 = h2.getPath();
	    if ( l1 == l2 ) {
		return 0;
	    } else if ( l1 > l2 ) {
		return +1*sign;
	    } else{
		return -1*sign;
	    }
	}
    }
    

}
