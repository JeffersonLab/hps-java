package org.lcsim.hps.recon.tracking.kalman.util;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.HitDerivative;
import org.lcsim.recon.tracking.trfbase.HitError;
import org.lcsim.recon.tracking.trfbase.HitVector;
import org.lcsim.recon.tracking.trfutil.Assert;

/**
 * 
 * A null hit so that I have a way to add pure
 * scattering surfaces to HTracks.
 *
 *@author $Author: jeremy $
 *@version $Id: HitNull.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class HitNull extends Hit
{
    
    private static final int SIZE=0;
    private int _ival;
            
    public String toString()
    {
	return "Dummy hit prediction " + _ival + "\n"
	    + "Cluster address: " + _pclus + "\n"
	    + "Cluster: " + _pclus + "\n";
    }
            
    protected boolean equal(Hit hp)
    {
	Assert.assertTrue( hp.type().equals(type()) );
	// All null hits are equal to each other.
	return true;
    }
            
    // static methods
    // Return the type name.
    public static String typeName()
    { return "HitNull";
    }

    // Return the type.
    public static String staticType()
    { return typeName();
    }
            
    public HitNull()
    {
    }
            
    HitNull( HitNull ht)
    {
    }

    public String type()
    { return staticType();
    }
    
    public int size()
    { return SIZE;
    };

    public HitVector measuredVector()
    {
	return new HitVector();
    }
    public HitError measuredError()
    {
	return new HitError();
    }
    public HitVector predictedVector()
    {
	return new HitVector();
    }
    public HitError predictedError()
    {
	return new HitError();
    }
    public HitDerivative dHitdTrack()
    {
	return new HitDerivative();
    }
    public HitVector differenceVector()
    { return new HitVector();
    }
    public void update(ETrack tre)
    { 
    }
}

