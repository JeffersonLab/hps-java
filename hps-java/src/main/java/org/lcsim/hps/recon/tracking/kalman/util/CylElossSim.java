package org.lcsim.hps.recon.tracking.kalman.util;

import java.util.Random;

import org.lcsim.recon.tracking.trfbase.SimInteractor;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.CylEloss;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfeloss.DeDx;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.util.aida.AIDA;

/**
* Class for simulating Energy Loss in SurfCylinders.
 *
 *@author Norman A. Graf
 *@version 1.0
 *
*/
public class CylElossSim extends SimInteractor
{

    private AIDA aida = AIDA.defaultInstance();

    
    //attributes
    //thickness of the material in the cylinder
    private double _thickness;
    // type of energy loss being simulated
    private DeDx _dedx;

    private Random _ran = null;
    
    //
    
    /**
     *Construct in instance with the
     *appropriate thickness and DeDx class to model
     * 
     *
     * @param   thickness Thickness of the cylindrical shell.
     * @param   dedx The DeDx class representing energy loss.
     */
    public CylElossSim(double thickness, DeDx dedx)
    {
        _thickness = thickness;
        _dedx = dedx;
    }

    public CylElossSim(double thickness, DeDx dedx, Random ran)
    {
        _thickness = thickness;
        _dedx = dedx;
	_ran  = ran;
    }
    
    //
    
    /**
     *Construct an instance from a CylEloss Interactor.
     *
     * @param   inter The CylEloss Interactor to use for simulation.
     */
    public CylElossSim(CylEloss inter)
    {
        _thickness = inter.thickness();
        _dedx = inter.dEdX();
    }
    
    //
    
    /**
     *Interact the given track in this cylindrical shell,
     *using the DeDx model for energy loss.
     *Note that the track parameters are updated to reflect the
     *energy lost by traversing the cylindrical shell of material.
     *
     * @param   vtrk The VTrack to scatter.
     */
    public void interact( VTrack vtrk )
    {
        // This can only be used with cylinders... check that we have one..
        
        Assert.assertTrue( vtrk.surface() instanceof SurfCylinder );
        
        
        TrackVector theVec = vtrk.vector();
        TrackVector newVector = new TrackVector(theVec);
        
        double pionMass = 0.13957; // GeV
        double ptmax = 10000.; // GeV
        
        double pinv = Math.abs(theVec.get(SurfCylinder.IQPT)*Math.cos(Math.atan(theVec.get(SurfCylinder.ITLM))));
        
        // make sure pinv is greater than a threshold (1/ptmax)
        // in this case assume q = 1, otherwise q = q/pt/abs(q/pt)
        
        double sign = 1;
        if(pinv < 1./ptmax)
            pinv = 1./ptmax;
        else
            sign = theVec.get(SurfCylinder.IQPT)/Math.abs(theVec.get(SurfCylinder.IQPT));
        
        // Evaluate the initial energy assuming the particle is a pion.
        
        double trackEnergy = Math.sqrt(1./pinv/pinv+pionMass*pionMass);
        
        double trueLength = _thickness/Math.abs(Math.cos(theVec.get(SurfCylinder.IALF)))/
        Math.cos(Math.atan(theVec.get(SurfCylinder.ITLM)));
	aida.cloud1D("Test/true length").fill(trueLength);
	aida.cloud1D("Test/alpha").fill(theVec.get(SurfCylinder.IALF));
	aida.cloud1D("Test/tanlam").fill(theVec.get(SurfCylinder.ITLM)  );
	aida.histogram1D("Test/true lengthxx",1000,-0.5, 0.5).fill(trueLength);
	aida.histogram1D("Test/tanlamba",100, -1., 1.).fill(theVec.get(SurfCylinder.ITLM)  );

        // assume the energy loss distribution to be Gaussian
        double stdEnergy = _dedx.sigmaEnergy(trackEnergy, trueLength);
        double stdMomentum = stdEnergy;
        // What direction are we going?
        // If forward, that means we lose energy
        // backwards means gain energy

        if(vtrk.isTrackBackward()) {
	    aida.cloud1D("Test/is Backwards").fill(1.);
	}else{
	    aida.cloud1D("Test/is Backwards").fill(-1.);
	}

        if(vtrk.isTrackBackward()) trueLength = -trueLength;

	double eold = trackEnergy;

        trackEnergy=_dedx.loseEnergy(trackEnergy, trueLength);

	aida.cloud1D("Test/DE no smear").fill(trackEnergy-eold);

	if ( _ran != null ) {
	    trackEnergy += stdEnergy*_ran.nextGaussian();
	}
	aida.cloud1D("Test/DE with smear").fill(trackEnergy-eold);
	if ( trackEnergy-eold > -0.005 ){
	    aida.cloud1D("Test/DE with smear, detail").fill(trackEnergy-eold);
	}

        double newMomentum = trackEnergy>pionMass ?
        Math.sqrt(trackEnergy*trackEnergy-
        pionMass*pionMass): 1./pinv;
        // Only vec(SurfCylinder.IQPT) changes due to E loss.
        newVector.set(SurfCylinder.IQPT, 1./newMomentum/Math.cos(Math.atan(theVec.get(SurfCylinder.ITLM)))*sign);
        vtrk.setVectorAndKeepDirection(newVector);
        
    }
    //
    
    /**
     *Return the thickness of material in the cylindrical shell.
     *
     * @return The thickness of the  energy loss material.
     */
    public double thickness()
    { return _thickness;
    }
    //
    
     /**
     *Return the energy loss model used in this Interactor.
     *
     * @return The DeDx class representing energy loss.
     */
    public DeDx dEdX()
    {
        return _dedx; //cng shallow copy!
    }
    
    //
    
    /**
     *Make a clone of this object.
     * Note that new copy will have different random number generator.
     *
     * @return A Clone of this instance.
     */
    public SimInteractor newCopy()
    {
        return new CylElossSim(_thickness,_dedx);
    }
    
    
    /**
     *output stream
     *
     * @return A String representation of this instance.
     */
    public String toString()
    {
        return "CylElossSim with thickness "+_thickness+" and energy loss "+_dedx;
    }
}
