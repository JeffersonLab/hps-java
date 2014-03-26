/*
 * HelixParameterCalculator.java
 *
 * Created on July 7th, 2008, 11:09 AM
 *
 * 
 */
package org.hps.recon.tracking.kalman;

import hep.physics.vec.BasicHep3Vector;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;

/**
 * Class used for calculating MC particle track paramters
 * @author Pelham Keahey
 * 
 */
public class HelixParamCalculator  {

    MCParticle mcp;
    private double R,BField,theta,arclength;
    //Some varibles are usesd in other calculations, thus they are global
    /*
    xc, yc --coordinates
    mcdca -- MC distance of closest approach 
    mcphi0 --azimuthal angle
    tanL -- Slope SZ plane ds/dz
    x0,y0 are the position of the particle at the dca
    */
    private double xc,yc,mcdca,mcphi0,tanL;
    private double x0,y0;
    /**
     * Constructor that is fed a magnetic field and MCPARTICLE
     * @param mcpc
     * @param cBField
     */
    public HelixParamCalculator(MCParticle mcpc,double cBField)
    {
        //mc and event global varibles used for calculation
        mcp=mcpc;
        
        //Returns the MagneticField at point (0,0,0), assumes constant magfield
        BField = cBField;
        
        //Calculate theta, the of the helix projected into an SZ plane, from the z axis
        double px = mcp.getPX();
        double py = mcp.getPY();
        double pz = mcp.getPZ();
        double pt = Math.sqrt(px*px + py*py);
        double p = Math.sqrt(pt*pt + pz*pz);
        double cth = pz / p;
        theta = Math.acos(cth);
       
        //Calculate Radius of the Helix
        R = ((mcp.getCharge())*(mcp.getMomentum().magnitude()*Math.sin(theta))/(.0003*BField));
        
        //Slope in the Dz/Ds sense, tanL Calculation
        tanL = mcp.getPZ()/(Math.sqrt(mcp.getPX()*mcp.getPX()+mcp.getPY()*mcp.getPY()));
       
        //Distance of closest approach Calculation
        xc   = mcp.getOriginX() + R * Math.sin(Math.atan2(mcp.getPY(),mcp.getPX()));
        yc   = mcp.getOriginY() - R * Math.cos(Math.atan2(mcp.getPY(),mcp.getPX()));
        double xcyc = Math.sqrt(xc*xc + yc*yc);
            if(mcp.getCharge()>0)
            {
            mcdca = R - xcyc;
            }
            else
            {
            mcdca = R + xcyc;      
            }
        
        
        //azimuthal calculation of the momentum at the DCA, phi0, Calculation
        mcphi0 = Math.atan2(xc/(R-mcdca), -yc/(R-mcdca));
            if(mcphi0<0)
            {
                mcphi0 += 2*Math.PI;
            }
        //z0 Calculation, z position of the particle at dca
        x0 = -mcdca*Math.sin(mcphi0);
        y0 = mcdca*Math.sin(mcphi0);
        arclength  = (((mcp.getOriginX()-x0)*Math.cos(mcphi0))+((mcp.getOriginY()-y0)*Math.sin(mcphi0)));
    
    }
    /**
     * Calculates the B-Field from event
     * @param mcpc
     * @param eventc
     */
    public HelixParamCalculator(MCParticle mcpc,EventHeader eventc)
    {
        this(mcpc,eventc.getDetector().getFieldMap().getField(new BasicHep3Vector(0.,0.,0.)).z());
    }
    /**
     * Return the magneticfield at point 0,0,0
     * @return double BField
     */
    public double getMagField()
    {
        return BField;
    }
    /**
     * Return the radius of the Helix track
     * @return double R
     */
    public double getRadius()
    {
        return R;
    }
    /**
     * Return the theta angle for the projection of the helix in the SZ plane 
     * from the  z axis
     * @return double theta
     */
    public double getTheta()
    {
        return theta;
    }
    /**
     * Return the particle's momentum
     * @return double mcp momentum
     */
    public double getMCMomentum()
    {
        return mcp.getMomentum().magnitude();
    }
    /**
     * Return the curvature (omega)
     * @return double omega
     */
    public double getMCOmega()
    {     
        return mcp.getCharge()/((mcp.getMomentum().magnitude()*Math.sin(theta))/(.0003*BField));
    }
    /**
     * Return the transvers momentum of the MC particle, Pt
     * @return double Pt
     */
    public double getMCTransverseMomentum()
    {
        return (mcp.getMomentum().magnitude())*Math.sin(theta);
    }
    /**
     * Return the slope of the helix in the SZ plane, tan(lambda)
     * @return double tanL
     */
    public double getSlopeSZPlane()
    {
        return tanL;
    }
    /**
     * Return the distance of closest approach
     * @return double mcdca
     */
    public double getDCA()
    {
      return mcdca;
    }
    /**
     * Return the azimuthal angle of the momentum when at the position of closest approach
     * @return double mcphi0
     */
    public double getPhi0()
    {
      return mcphi0;
    }
    /**
     * Return the z position at the distance of closest approach
     * @return double z0 position
     */
    public double getZ0()
    {
        double x0 = -mcdca*Math.sin(mcphi0);
        double y0 = mcdca*Math.sin(mcphi0);
        double s  = (((mcp.getOriginX()-x0)*Math.cos(mcphi0))+((mcp.getOriginY()-y0)*Math.sin(mcphi0)));
        return mcp.getOriginZ()-(s*tanL);
    }
    /**
     * Return the arclength of the helix from the ORIGIN TO THE DCA
     * @return double arclength
     */
    public double getArcLength()
    {
        return arclength;
    }
    /**
     * Return the x position of the particle when at the dca
     * @return double arclength
     */
    public double getX0()
    {
        return x0;
    }
    /**
     * Return the y position of the particle at the dca
     * @return double arclength
     */
    public double getY0()
    {
        return y0;
    }
    
}
