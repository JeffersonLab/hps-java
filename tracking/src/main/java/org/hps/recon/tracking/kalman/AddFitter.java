package org.hps.recon.tracking.kalman;
import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trffit.HTrack;
import org.lcsim.recon.tracking.trfutil.Assert;
/**
 * Adds a hit prediction to a track, refits the track and uses the
 * new track parameters to update the hit prediction.
 *<p>
 * Two methods are provided: one updates an ETrack and its chi-square
 * and the other updates an HTrack.
 *<p>
 * This base class will always return an error.  Subclasses will typically
 * implement the ETrack method which is then invoked by the HTrack method
 * defined here.
 *
 *@author Norman A. Graf
 *@version 1.0
 *
 */

public class AddFitter
{
    
    // Static methods.
    
    //
    
    /**
     *Return String representation of this class' type name.
     *Included for completeness with the C++ versin.
     *
     * @return String representation of this class' type.
     */
    public static String typeName()
    { return "AddFitter"; }
    
    //
    
    /**
     *Return String representation of this class' type name.
     *Included for completeness with the C++ versin.
     *
     * @return String representation of this class' type.
     */
    public static String staticType()
    { return typeName(); }
    
    // workaround
    private double _chsq;
    
    // methods
    
    //
    
    /**
     *Construct a default instance.
     *
     */
    public AddFitter()
    {
        _chsq = 0.;
    }
    
    //
    
    /**
     *Return the generic type.
     * This is only needed at this level.
     *
     * @return String representation of this class' type.
     */
    public String genericType()
    { return staticType(); }
    
    //
    
    /**
     *Add a hit and fit with the new hit.
     * Return status 0 if fit is successful, negative value for a local
     * error and positive for an error in add_hit_fit.
     * The default method calls add_hit_fit and return its status.
     * If the fit is successful, then the track fit is updated and the hit
     * is added to the end of its list.
     *
     * @param   trh The HTrack to which the hit will be added.
     * @param   hit The Hit to add.
     * @return 0 if hit update and fit are successful.
     */
    public  int addHit(HTrack trh,  Hit hit)
    {
        // Fetch the starting fit and chi-square.
        ETrack tre = trh.newTrack();
        double chsq = trh.chisquared();
        
        // check the track and hit are at the same surface
        Surface tsrf = tre.surface();
        Surface hsrf = hit.surface();
        Assert.assertTrue( tsrf.pureEqual(hsrf) );
        if ( ! tsrf.pureEqual(hsrf) ) return -1;
        
        // Check the track is fully fit before adding hit.
        // Unless this is the first hit.
        if ( trh.hits().size() !=0 )
        {
            Assert.assertTrue( trh.isFit() );
            if ( ! trh.isFit() ) return -2;
        }
        // Fit with the new point; exit if error occurs.
        int stat = addHitFit(tre,chsq,hit); //chsq is return argument in c++
        // need to fix this
        if ( stat != 0 ) return stat;
        
        
        // Update the track with the new fit and hit.
        trh.addHit(hit);
        trh.setFit(tre,_chsq);
        
        return 0;
        
    }
    
    
    /**
     *Set the chi-squared for the fit.
     *
     * @param   chsq The value of chi-square to set for this fit.
     */
    public void setChisquared(double chsq)
    {
        _chsq = chsq;
    }
    
    
    /**
     *Return the chi-squared for the fit.
     *
     * @return  The value of chi-square for this fit.
     */
    public double chisquared()
    {
        return _chsq;
    }
    //
    
    
    /**
     *Refit a track and update its chi-square by adding the specified hit.
     * Return status 0 if fit is successful, positive value for error.
     * This is the method implemented by subclasses.
     * If the fit fails, the track and chi-square may return any value
     * and the hit may be updated with any track.
     * If the fit is successful, the fit track and chi-square are
     * returned and the hit is updated with the fit track.
     * Normally this is not invoked directly but is called by add_hit.
     * The default method here returns an error and throws and AssertException.
     *
     * @param   tre The ETrack to which the hit will be added.
     * @param   chisq The value of chi-square to set for this fit.
     * @param   phit The Hit to add.
     * @return 1.
     */
    public int addHitFit(ETrack tre, double chisq,  Hit phit)
    {  Assert.assertTrue( false );
       return 1;}
    
    
    /**
     *output stream
     *
     * @return AString representation of this object.
     */
    public String toString()
    {
        return getClass().getName();
    }
}
