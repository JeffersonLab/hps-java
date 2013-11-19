package  org.lcsim.hps.recon.tracking.kalman.util;
//package  org.lcsim.recon.tracking.trfzp;


import java.util.Random;

import org.lcsim.recon.tracking.trfbase.SimInteractor;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;


/**
 * Class for adding Multiple Scattering to track vectors defined at
 * ZPlanes.  Single point interaction is assumed.
 *
 *@author Norman A. Graf
 *@version 1.0
 *
 */
public class ThinZPlaneMsSim extends SimInteractor
{
    // static attributes
    // Assign track parameter indices.
    
    private static final int IX = SurfZPlane.IX;
    private static final int IY   = SurfZPlane.IY;
    private static final int IDXDZ = SurfZPlane.IDXDZ;
    private static final int IDYDZ = SurfZPlane.IDYDZ;
    private static final int IQP  = SurfZPlane.IQP;
    
    
    // attributes
    
    // random number generator
    private Random _r;
    
    // radiation lengths in material
    private double _radLength;
    
    //
    
    /**
     * Construct an instance from the number of radiation
     * lengths of the thin z plane material.
     * The Interactor is constructed with the
     * appropriate number of radiation lengths.
     *
     * @param   radLengths The thickness of the material in radiation lengths.
     */
    public ThinZPlaneMsSim( double radLengths )
    {
        _radLength = radLengths;
        _r = new Random();
    }
    
    //
    
    /**
     *Interact the given track in this thin z plane,
     *using the thin material approximation for multiple scattering.
     *Note that the track parameters are modified to simulate
     *the effects of multiple scattering in traversing the thin z
     *plane of material.
     *
     * @param   vtrk  The Vrack to scatter.
     */
    public void interact( VTrack vtrk )
    {
        TrackVector trv = new TrackVector( vtrk.vector() );
        // first, how much should the track be scattered:
        // uses radlength from material and angle track - plane (leyer)
        double  trackMomentum = trv.get(IQP);
        
        double f = trv.get(IDXDZ);
        double g = trv.get(IDYDZ);
        
        double theta = Math.atan(Math.sqrt(f*f + g*g));
        double phi = 0.;
        if (f != 0.) phi = Math.atan(Math.sqrt((g*g)/(f*f)));
        if (f == 0.0 && g < 0.0) phi = 3.*Math.PI/2.;
        if (f == 0.0 && g > 0.0) phi = Math.PI/2.;
        if (f == 0.0 && g == 0.0)
        {
            phi = 99.;// that we can go on further.....
            System.out.println(" DXDY and DXDZ both 0");
        }
        if((f<0)&&(g>0))
            phi = Math.PI - phi;
        if((f<0)&&(g<0))
            phi = Math.PI + phi;
        if((f>0)&&(g<0))
            phi = 2*Math.PI - phi;
        
        double trueLength = _radLength/Math.cos(theta);
        
        double scatRMS = (0.0136)*trackMomentum*Math.sqrt(trueLength)*
                (1 + 0.038*Math.log(trueLength));
        
        double zhat = Math.sqrt(1-Math.sin(theta)*Math.sin(theta));
        double xhat = Math.sin(theta)*Math.cos(phi);
        double yhat = Math.sin(theta)*Math.sin(phi);
        
        double[] scatterVec = new double[3];
        double[] finalVec = new double[3];
        double[][] Rotation = new double[3][3];
        
        //set Rotation matrix as given in D0note ????
        double ctrans = Math.sqrt(xhat*xhat + yhat*yhat);
        Rotation[0][0] = -yhat/ctrans;
        Rotation[0][1] = -zhat*xhat/ctrans;
        Rotation[0][2] = xhat;
        Rotation[1][0] = xhat/ctrans;
        Rotation[1][1] = -zhat*yhat/ctrans;
        Rotation[1][2] = yhat;
        Rotation[2][0] = 0;
        Rotation[2][1] = (xhat*xhat+yhat*yhat)/ctrans;
        Rotation[2][2] = zhat;
        
        //now set the Vector after scattering ( (0,0,1) ->( theta1, theta2, 1)*norm)
        scatterVec[0] = scatRMS*_r.nextGaussian();
        scatterVec[1] = scatRMS*_r.nextGaussian();
        scatterVec[2] = 1.0;
        double norm = Math.sqrt(scatterVec[0]*scatterVec[0] + scatterVec[1]*scatterVec[1]
                + scatterVec[2]*scatterVec[2]);
        
        if (norm!=0)
        {
            scatterVec[0] /= norm;
            scatterVec[1] /= norm;
            scatterVec[2] /= norm;
        };
        
        //now go back to the global coordinate system
        //leave that step out if track coodsys = global coordsys
        double finalxz;
        double finalyz;
        if (phi != 99.)
        {
            for (int k = 0; k<3; k++)
            {
                finalVec[k] = 0.;
                for (int l = 0; l<3 ; l++)
                {
                    finalVec[k] += Rotation[k][l]*scatterVec[l];
                }
            }
            finalxz = finalVec[0]/finalVec[2];
            finalyz = finalVec[1]/finalVec[2];
        }
        else
        {
            finalxz = scatterVec[0];
            finalyz = scatterVec[1];
        };
        
        trv.set(IDXDZ, finalxz);
        trv.set(IDYDZ, finalyz);
        
        // assume that we don't encounter back-scattering... which is
        // assumed above anyway.
        vtrk.setVectorAndKeepDirection( trv );
        
    }
    
    
    /**
     *Return the number of radiation lengths of material in this thin z plane.
     *
     * @return The number of radiation lengths.
     */
    public double radLength()
    {
        return _radLength;
    }
    
    //
    
    /**
     * Make a clone of this object.
     * Note that new copy will have a different random number generator.
     *
     * @return A Clone of this instance.
     */
    public SimInteractor newCopy()
    {
        return new ThinZPlaneMsSim(_radLength);
    }
    
    
    
    /**
     *output stream
     *
     * @return A String representation of this instance.
     */
    public String toString()
    {
        return "ThinZPlaneMsSim with "+_radLength+" radiation lengths";
    }
    
}
