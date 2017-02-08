package org.hps.recon.tracking;

import hep.physics.matrix.SymmetricMatrix;

import java.util.HashMap;
import java.util.Map;

import org.lcsim.event.MCParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.MultipleScatter;

/**
 * Extension of {@link HelicalTrackFit} to include HPS-specific information and utilities.
 */
public class HpsHelicalTrackFit extends HelicalTrackFit {

    private MCParticle mcParticle = null;
    private double[] refPoint = {0.,0.};
    
    public HpsHelicalTrackFit(HelicalTrackFit htf) {
        super(htf.parameters(), htf.covariance(), htf.chisq(), htf.ndf(), htf.PathMap(), htf.ScatterMap());
    }

    public HpsHelicalTrackFit(double[] parameters, SymmetricMatrix covariance, double[] chiSquared, int[] ndf, Map<HelicalTrackHit, Double> sMap, Map<HelicalTrackHit, MultipleScatter> msMap) {
        super(parameters, covariance, chiSquared, ndf, sMap, msMap);
    }
    
    public HpsHelicalTrackFit(double[] parameters, SymmetricMatrix covariance, double[] chiSquared, int[] ndf, Map<HelicalTrackHit, Double> sMap, Map<HelicalTrackHit, MultipleScatter> msMap, double[] refPoint) {
        super(parameters, covariance, chiSquared, ndf, sMap, msMap);
        this.refPoint = refPoint;
    }
    

    /**
     * Get map of the the track trajectory within the uniform bfield
     */
    public Map<Integer, Double[]> trackTrajectory(double zStart, double zStop, int nSteps) {
        Map<Integer, Double[]> traj = new HashMap<Integer, Double[]>();
        double step = (zStop - zStart) / nSteps;
        Double zVal = zStart;
        Integer nstep = 0;
        while (zVal < zStop) {
            Double[] xyz = {0.0, 0.0, 0.0};
            double s = HelixUtils.PathToXPlane(this, zVal, 1000.0, 1).get(0);
            xyz[0] = HelixUtils.PointOnHelix(this, s).y();
            xyz[1] = HelixUtils.PointOnHelix(this, s).z();
            xyz[2] = zVal;
            traj.put(nstep, xyz);
            zVal += step;
            nstep++;
        }
        return traj;
    }

    /**
     * Get map of the the track direction within the uniform bfield
     */
    public Map<Integer, Double[]> trackDirection(double zStart, double zStop, int nSteps) {
        Map<Integer, Double[]> traj = new HashMap<Integer, Double[]>();
        double step = (zStop - zStart) / nSteps;
        Double zVal = zStart;

        Integer nstep = 0;
        while (zVal < zStop) {
            Double[] xyz = {0.0, 0.0, 0.0};
            double s = HelixUtils.PathToXPlane(this, zVal, 1000.0, 1).get(0);
            xyz[0] = HelixUtils.Direction(this, s).y();
            xyz[1] = HelixUtils.Direction(this, s).z();
            xyz[2] = zVal;
            traj.put(nstep, xyz);
            zVal += step;
            nstep++;
        }
        return traj;
    }

     

    /**
     * Get the MC Particle associated with the HelicalTrackFit
     *
     * @return mcParticle :
     */
    public MCParticle getMcParticle() {
        return this.mcParticle;
    }

    public void setMcParticle(MCParticle mcParticle) {
        this.mcParticle = mcParticle;
    }

    public double[] getRefPoint() {
        return refPoint;
    }


       
}
