package org.lcsim.hps.recon.tracking.kalman.util;

import org.lcsim.recon.tracking.trfeloss.DeDx;

/**
 *  DeDx code for testing the fitter performance:
 *     - constant energy loss
 *     - gaussian straggling.
 *
 *  The amount of energy loss and the sigma of the gaussian are
 *  controlled c'tor arguments.  

 *  For a large enough sigma, this code will allow "negative" energy 
 *  loss in the tail of the gaussian. This is the desired behaviour to
 *  get a zero offset in the track parameter pull distributions.
 *
 *@author $author$
 *@version $Id: RKDeDxFixed.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 */
public class RKDeDxFixed extends DeDx
{
    
    private double _density;

    // Scale the nominal energy loss by this factor.
    private double _scale = 1.0;

    // Sigma of the gaussian smearing.
    private double _sigma   = 0.15;

    /**
     *Construct an instance given a material density.
     *
     * @param   density The density of the material.
     */
    public RKDeDxFixed(double density)
    {
        _density = density;
    }

    public RKDeDxFixed(double density, double scale, double sigma)
    {
        _density = density;
	_scale   = scale;
	_sigma   = sigma;
    }

    
    /**
     *Return energy loss (dE/dx) for a given energy.
     *
     * @param   energy The energy of the particle.
     * @return The average energy lost by a particle of this energy.
     */
    public double dEdX(double energy)
    {
        double mip = 1.665; // this is an average mip
        double de_dx = mip*_density;   // MeV/cm
        de_dx /= 1000.; // GeV/cm

	de_dx *= _scale;
        return de_dx;
    }
    
    /**
     *Return the uncertainty in the energy lost sigma(E).
     *
     * @param   energy The energy of the particle.
     * @param   x The amount of material.
     * @return The uncertainty in the energy lost sigma(E).
     */
    public double sigmaEnergy(double energy, double x)
    {
	double sigma_e = _sigma*dEdX(energy)*x;
        return sigma_e;
    }
    
    /**
     *Return new energy for a given path distance.
     * Energy increases if x < 0.
     *
     * @param   energy The energy of the particle.
     * @param   x The amount of material.
     * @return New energy for a given path distance.
     */
    public double loseEnergy(double energy, double x)
    {
        double deltaE = dEdX(energy)*x;
        return energy-deltaE;
    }
    
    
    /**
     *output stream
     *
     * @return A String representation of this instance.
     */
    public String toString()
    {
        return "RKDeDxFixed with density "+_density;
    }
}
