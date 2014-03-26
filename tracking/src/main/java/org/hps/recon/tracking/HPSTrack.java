package org.hps.recon.tracking;

import static org.lcsim.constants.Constants.fieldConversion;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.deprecated.BeamSpot;
import org.hps.conditions.deprecated.FieldMap;
import org.hps.util.Pair;
import org.lcsim.event.MCParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.MultipleScatter;
import org.lcsim.spacegeom.CartesianVector;
import org.lcsim.spacegeom.SpacePoint;
import org.lcsim.spacegeom.SpaceVector;
import org.lcsim.util.swim.Helix;
import org.lcsim.util.swim.Line;
import org.lcsim.util.swim.Trajectory;

/**
 * Class HPSTrack: extension of HelicalTrackFit to include HPS-specific
 * variables other useful things.
 *
 * @author mgraham created on 6/27/2011
 */
public class HPSTrack extends HelicalTrackFit {

    private BeamSpot _beam;
    //all of the variables defined below are in the jlab (detector) frame
    //this position & momentum are measured at the DOCA to the Y-axis,
    //which is where the tracking returns it's parameters by default
    private Hep3Vector _pDocaY;
    private Hep3Vector _posDocaY;
    //the position & momentum of the track at the intersection of the target (z=0)
    private Hep3Vector _pTarget;
    private Hep3Vector _posTarget;
    //the position & momentum of the track at DOCA to the beam axis (z)
    private Hep3Vector _pDocaZ;
    private Hep3Vector _posDocaZ;
    private double bField = 0.491;  //  make this set-able
    private boolean _debug = false;
    private boolean _debugForward = false;
    private Trajectory _trajectory;
    Map<Pair<Integer, Integer>, Double> _fieldMap;
    Map<Pair<Integer, Integer>, Pair<Double, Double>> _fieldBins;
    private MCParticle mcParticle;

    public HPSTrack(double[] pars, SymmetricMatrix cov, double[] chisq, int[] ndf,
            Map<HelicalTrackHit, Double> smap, Map<HelicalTrackHit, MultipleScatter> msmap,
            BeamSpot beam) {
        super(pars, cov, chisq, ndf, smap, msmap);
        _beam = beam;
        calculateParametersAtTarget();
        calculateParametersAtDocaY();
        calculateParametersAtDocaZ();
    }

    public HPSTrack(double[] pars, SymmetricMatrix cov, double[] chisq, int[] ndf,
            Map<HelicalTrackHit, Double> smap, Map<HelicalTrackHit, MultipleScatter> msmap) {
        super(pars, cov, chisq, ndf, smap, msmap);
        calculateParametersAtTarget();
        calculateParametersAtDocaY();
        calculateParametersAtDocaZ();
    }

    public HPSTrack(HelicalTrackFit htf, BeamSpot beam) {
        super(htf.parameters(), htf.covariance(), htf.chisq(), htf.ndf(), htf.PathMap(), htf.ScatterMap());
        _beam = beam;
        calculateParametersAtTarget();
        calculateParametersAtDocaY();
        calculateParametersAtDocaZ();
    }

    public HPSTrack(HelicalTrackFit htf) {
        super(htf.parameters(), htf.covariance(), htf.chisq(), htf.ndf(), htf.PathMap(), htf.ScatterMap());
        calculateParametersAtTarget();
        calculateParametersAtDocaY();
        calculateParametersAtDocaZ();
    }

    public HPSTrack(double[] parameters, SymmetricMatrix covariance, double[] chiSquared, int[] ndf,
            Map<HelicalTrackHit, Double> sMap, Map<HelicalTrackHit, MultipleScatter> msMap, MCParticle mcParticle) {
        super(parameters, covariance, chiSquared, ndf, sMap, msMap);

        // Set the MC particle associated with this fit
        this.mcParticle = mcParticle;
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

    private void calculateParametersAtTarget() {
        double pTot = this.p(bField);
        //currently, PathToXPlane only returns a single path (no loopers!)
        List<Double> paths = HelixUtils.PathToXPlane(this, 0.0, 100.0, 1);
        Hep3Vector posTargetTrkSystem = HelixUtils.PointOnHelix(this, paths.get(0));
        Hep3Vector dirTargetTrkSystem = HelixUtils.Direction(this, paths.get(0));
        _posTarget = HPSTransformations.transformVectorToDetector(posTargetTrkSystem);
        _pTarget = VecOp.mult(pTot, HPSTransformations.transformVectorToDetector(dirTargetTrkSystem));

    }

    private void calculateParametersAtDocaY() {
        double pTot = this.p(bField);
        Hep3Vector posDocaYTrkSystem = HelixUtils.PointOnHelix(this, 0);
        Hep3Vector dirDocaYTrkSystem = HelixUtils.Direction(this, 0);
        _posDocaY = HPSTransformations.transformVectorToDetector(posDocaYTrkSystem);
        _pDocaY = VecOp.mult(pTot, HPSTransformations.transformVectorToDetector(dirDocaYTrkSystem));

    }

    private void calculateParametersAtDocaZ() {
        double pTot = this.p(bField);
        double sAtDocaZ = findPathToDocaZ();
        Hep3Vector posDocaZTrkSystem = HelixUtils.PointOnHelix(this, sAtDocaZ);
        Hep3Vector dirDocaZTrkSystem = HelixUtils.Direction(this, sAtDocaZ);
        _posDocaZ = HPSTransformations.transformVectorToDetector(posDocaZTrkSystem);
        _pDocaZ = VecOp.mult(pTot, HPSTransformations.transformVectorToDetector(dirDocaZTrkSystem));
    }

//    public Hep3Vector getPositionAtZ(double xFinal, double fringeHalfWidth, double step) {
//        double startFringeUpstream = -2 * fringeHalfWidth;
//        double stopFringeUpstream = 0;
//        if (_debug)
//            System.out.println(this.toString());
//
//
//    }
    public Hep3Vector getPositionAtZ(double xFinal, double start, double stop, double step) {

        double startFringe = start;
        double stopFringe = stop;
        //       _debugForward = false;
        //       if (xFinal > 900)
        //           _debugForward = true;
        // if looking upstream, we'll be propagating backwards
        if (xFinal < 0) {
            step = -step;
            startFringe = stop;
            stopFringe = start;
        }
        double fringeHalfWidth = Math.abs(stopFringe - startFringe) / 2;
        double fringeMid = (stopFringe + startFringe) / 2;
        if (_debugForward) {
            System.out.println(this.toString());
        }

        double sStartFringe = HelixUtils.PathToXPlane(this, startFringe, 1000.0, 1).get(0);
        if (_debugForward) {
            System.out.println("path to end of fringe = " + sStartFringe + "; xFinal = " + xFinal);
        }
        double xtmp = startFringe;
        double ytmp = HelixUtils.PointOnHelix(this, sStartFringe).y();
        double ztmp = HelixUtils.PointOnHelix(this, sStartFringe).z();
        double Rorig = this.R();
        double xCtmp = this.xc();
        double yCtmp = this.yc();
        double q = Math.signum(this.curvature());
        double phitmp = getPhi(xtmp, ytmp, xCtmp, yCtmp, q);
        if (_debugForward) {
            System.out.println("\nOriginal xtmp = " + xtmp
                    + "; ytmp = " + ytmp
                    + "; ztmp = " + ztmp
                    + "; phitmp = " + phitmp);

            System.out.println("nOriginal Rorig = " + Rorig
                    + "; xCtmp = " + xCtmp
                    + "; yCtmp = " + yCtmp);
        }
        if (_debugForward) {
            System.out.println("Original Direction at Fringe: " + HelixUtils.Direction(this, startFringe).toString());
        }
        double Rtmp = Rorig;
        // now start stepping through the fringe field
        Hep3Vector r0Tmp = HelixUtils.PointOnHelix(this, sStartFringe);
        SpacePoint r0 = new SpacePoint(r0Tmp);
        double pTot = this.p(bField);
        Hep3Vector dirOrig = HelixUtils.Direction(this, sStartFringe);
        Hep3Vector p0 = VecOp.mult(pTot, dirOrig);
        Hep3Vector dirTmp = dirOrig;
        SpacePoint rTmp = r0;
        Hep3Vector pTmp = p0;
        double pXOrig = p0.x();
        double pXTmp = pXOrig;
        double totalS = sStartFringe;
        if (_debugForward) {
            double tmpdX = xFinal - startFringe;
            Hep3Vector fooExt = extrapolateStraight(dirOrig, tmpdX);
            Hep3Vector fooFinal = VecOp.add(fooExt, r0Tmp);
            System.out.println("Extrpolating straight back from startFringe = (" + fooFinal.x() + "," + fooFinal.y() + "," + fooFinal.z() + ")");

        }
        //follow trajectory while:  in fringe field, before end point, and we don't have a looper
        while (Math.signum(step) * xtmp < Math.signum(step) * stopFringe && Math.signum(step) * xtmp < Math.signum(step) * xFinal && Math.signum(pXOrig * pXTmp) > 0) {
            if (_debugForward) {
                System.out.println("New step in Fringe Field");
                System.out.println("rTmp = " + rTmp.toString());
                System.out.println("pTmp = " + pTmp.toString());
                System.out.println("OriginalHelix pos = " + HelixUtils.PointOnHelix(this, totalS));
                System.out.println("OriginalHelix Momentum = " + VecOp.mult(pTot, HelixUtils.Direction(this, totalS)));
            }

            double fringeFactor = getFringe(Math.signum(step) * (fringeMid - rTmp.x()), fringeHalfWidth);
//            double myBField=bField * fringeFactor;
            double myBField = FieldMap.getFieldFromMap(rTmp.x(), rTmp.y());
            if (_debugForward) {
                System.out.println("rTmp.x() = " + rTmp.x() + " field = " + myBField);
            }
            setTrack(pTmp, rTmp, q, myBField);
            rTmp = _trajectory.getPointAtDistance(step);
            pTmp = VecOp.mult(pTot, _trajectory.getUnitTangentAtLength(step));
            pXTmp = pTmp.x();
            xtmp = rTmp.x();
            if (_debugForward) {
                System.out.println("##############   done...    #############");

                System.out.println("\n");
            }
            totalS += step;
        }
        //ok, done with field...extrapolate straight back...
        Hep3Vector pointInTrking;
        if (Math.signum(step) * xtmp < Math.signum(step) * xFinal) {
            //get direction of the track before it hits fringe field
            double deltaX = xFinal - xtmp;
            Hep3Vector dir = _trajectory.getUnitTangentAtLength(0);
//            double deltaY = deltaX * dir.y();
//            double deltaZ = deltaX * dir.z();
            Hep3Vector delta = extrapolateStraight(dir, deltaX);
//            double deltaZ = Math.sqrt(deltaX*deltaX+deltaY*deltaY)* dir.z();
            pointInTrking = new BasicHep3Vector(xFinal, delta.y() + ytmp, delta.z() + ztmp);

            if (_debugForward) {
                System.out.println("Pointing straight forward from xtmp = " + xtmp
                        + "; ytmp = " + ytmp
                        + "; ztmp = " + ztmp
                        + "; deltaX= " + deltaX);
                System.out.println("Directions:  " + dir.toString());
                System.out.println("Position at ECal:  x = " + xFinal + "; y = " + pointInTrking.y() + "; z = " + pointInTrking.z());

            }
        } else {  // still in the fringe field...just return the current position
//            pointInTrking = new BasicHep3Vector(xFinal, ytmp, ztmp);
            pointInTrking = new BasicHep3Vector(rTmp.x(), rTmp.y(), rTmp.z());
        }
        return HPSTransformations.transformVectorToDetector(pointInTrking);
    }

    /**
     * Get the position and direction on the track using B-field map for
     * extrapolation
     *
     * @param start = starting z-position of extrapolation
     * @param zFinal = final z-position
     * @param step = step size
     * @return position[0] and direction[1] at Z=zfinal
     */
    public Hep3Vector[] getPositionAtZMap(double start, double xFinal, double step) {
        return this.getPositionAtZMap(start, xFinal, step, true);
    }

    public Hep3Vector[] getPositionAtZMap(double start, double xFinal, double step, boolean debugOk) {

        double startFringe = start;

        _debugForward = false;
        if (xFinal > 900) {
            _debugForward = debugOk ? true : false;
        }
        // if looking upstream, we'll be propagating backwards
        if (xFinal < 0) {
            step = -step;
        }
        if (_debugForward) {
            System.out.println(this.toString());
        }

        double sStartFringe = HelixUtils.PathToXPlane(this, startFringe, 1000.0, 1).get(0);
        if (_debugForward) {
            System.out.println("path to end of fringe = " + sStartFringe + "; xFinal = " + xFinal);
        }
        double xtmp = startFringe;
        double ytmp = HelixUtils.PointOnHelix(this, sStartFringe).y();
        double ztmp = HelixUtils.PointOnHelix(this, sStartFringe).z();
        double Rorig = this.R();
        double xCtmp = this.xc();
        double yCtmp = this.yc();
        double q = Math.signum(this.curvature());
        double phitmp = getPhi(xtmp, ytmp, xCtmp, yCtmp, q);
        if (_debugForward) {
            System.out.println("\nOriginal xtmp = " + xtmp
                    + "; ytmp = " + ytmp
                    + "; ztmp = " + ztmp
                    + "; phitmp = " + phitmp);

            System.out.println("nOriginal Rorig = " + Rorig
                    + "; xCtmp = " + xCtmp
                    + "; yCtmp = " + yCtmp);
        }
        if (_debugForward) {
            System.out.println("Original Direction at Fringe: " + HelixUtils.Direction(this, startFringe).toString());
        }
        double Rtmp = Rorig;
        // now start stepping through the fringe field
        Hep3Vector r0Tmp = HelixUtils.PointOnHelix(this, sStartFringe);
        SpacePoint r0 = new SpacePoint(r0Tmp);
        double pTot = this.p(bField);
        Hep3Vector dirOrig = HelixUtils.Direction(this, sStartFringe);
        Hep3Vector p0 = VecOp.mult(pTot, dirOrig);
        Hep3Vector dirTmp = dirOrig;
        SpacePoint rTmp = r0;
        Hep3Vector pTmp = p0;
        double pXOrig = p0.x();
        double pXTmp = pXOrig;
        double totalS = sStartFringe;
        if (_debugForward) {
            double tmpdX = xFinal - startFringe;
            Hep3Vector fooExt = extrapolateStraight(dirOrig, tmpdX);
            Hep3Vector fooFinal = VecOp.add(fooExt, r0Tmp);
            System.out.println("Extrpolating straight back from startFringe = (" + fooFinal.x() + "," + fooFinal.y() + "," + fooFinal.z() + ")");

        }
        //follow trajectory while:  in fringe field, before end point, and we don't have a looper
        while (Math.signum(step) * xtmp < Math.signum(step) * xFinal && Math.signum(pXOrig * pXTmp) > 0) {
            if (_debugForward) {
                System.out.println("New step in Fringe Field");
                System.out.println("rTmp = " + rTmp.toString());
                System.out.println("pTmp = " + pTmp.toString());
                System.out.println("OriginalHelix pos = " + HelixUtils.PointOnHelix(this, totalS));
                System.out.println("OriginalHelix Momentum = " + VecOp.mult(pTot, HelixUtils.Direction(this, totalS)));
            }

            double myBField = FieldMap.getFieldFromMap(rTmp.x(), rTmp.y());
            if (_debugForward) {
                System.out.println("rTmp.x() = " + rTmp.x() + " field = " + myBField);
            }
            setTrack(pTmp, rTmp, q, myBField);
            rTmp = _trajectory.getPointAtDistance(step);
            pTmp = VecOp.mult(pTot, _trajectory.getUnitTangentAtLength(step));
            pXTmp = pTmp.x();
            xtmp = rTmp.x();
            if (_debugForward) {
                System.out.println("##############   done...    #############");

                System.out.println("\n");
            }
            totalS += step;
        }

        //Go with finer granularity in the end
        rTmp = _trajectory.getPointAtDistance(0);
        xtmp = rTmp.x();
        pTmp = VecOp.mult(pTot, _trajectory.getUnitTangentAtLength(step));
        pXTmp = pTmp.x();
        step = step / 10.0;

        while (Math.signum(step) * xtmp < Math.signum(step) * xFinal && Math.signum(pXOrig * pXTmp) > 0) {
            if (_debugForward) {
                System.out.println("New step in Fringe Field");
                System.out.println("rTmp = " + rTmp.toString());
                System.out.println("pTmp = " + pTmp.toString());
                System.out.println("OriginalHelix pos = " + HelixUtils.PointOnHelix(this, totalS));
                System.out.println("OriginalHelix Momentum = " + VecOp.mult(pTot, HelixUtils.Direction(this, totalS)));
            }

            double myBField = FieldMap.getFieldFromMap(rTmp.x(), rTmp.y());
            if (_debugForward) {
                System.out.println("rTmp.x() = " + rTmp.x() + " field = " + myBField);
            }
            setTrack(pTmp, rTmp, q, myBField);
            rTmp = _trajectory.getPointAtDistance(step);
            pTmp = VecOp.mult(pTot, _trajectory.getUnitTangentAtLength(step));
            pXTmp = pTmp.x();
            xtmp = rTmp.x();
            if (_debugForward) {
                System.out.println("##############   done...    #############");

                System.out.println("\n");
            }
            totalS += step;
        }

        //ok, done with field.
        Hep3Vector pointInTrking = new BasicHep3Vector(rTmp.x(), rTmp.y(), rTmp.z());
        if (_debugForward) {
            System.out.println("Position xfinal (tracking) :  x = " + xFinal + "; y = " + pointInTrking.y() + "; z = " + pointInTrking.z());
        }
        Hep3Vector[] out = {HPSTransformations.transformVectorToDetector(pointInTrking), HPSTransformations.transformVectorToDetector(pTmp)};

        return out;
    }

    private double getPhi(double x, double y, double xc, double yc, double sign) {
        //      System.out.println("Math.atan2(y - yc, x - xc)="+Math.atan2(y - yc, x - xc));
        return Math.atan2(y - yc, x - xc) - sign * Math.PI / 2;
    }

    private Hep3Vector extrapolateStraight(Hep3Vector dir, double deltaX) {
        double deltaY = deltaX * dir.y();
        double deltaZ = deltaX * dir.z();
        return new BasicHep3Vector(deltaX, deltaY, deltaZ);
    }

    //field that changes linearly from Bmax->0
    private double getFringe(double x, double halfWidth) {
//        System.out.println("x = " + x + "; halfWidth = " + halfWidth);
        if (x / halfWidth > 1) {
            return 1;
        }
        if (x / halfWidth < -1) {
            return 0;
        }

        return (1.0 / 2.0) * (1 + x / halfWidth);
    }

    private Hep3Vector getDirection(double phi, double sign) {
        double ux = Math.cos(phi) * this.sth();
        double uy = Math.sin(phi) * this.sth();
        double uz = this.cth();
        //  Return the direction unit vector
        return new BasicHep3Vector(ux, uy, uz);
    }

    private double findPathToDocaZ() {
        double step = 0.1;//100 um step size
        double maxS = 100.0; //go to 10cm
        double s = -30;
        double minDist = 999999;
        double minS = s;
        double dist = 999998;
        //once the distance starts increasing, quit and return
        while (dist < minDist) {
            Hep3Vector pos = HelixUtils.PointOnHelix(this, s);
            dist = pos.y() * pos.y() + pos.z() * pos.z();
            s += step;
        }
        return minS;
    }

    private void setTrack(Hep3Vector p0, SpacePoint r0, double q, double B) {
        SpaceVector p = new CartesianVector(p0.v());
        double phi = Math.atan2(p.y(), p.x());
        double lambda = Math.atan2(p.z(), p.rxy());
        double field = B * fieldConversion;

        if (q != 0 && field != 0) {
            double radius = p.rxy() / (q * field);
            _trajectory = new Helix(r0, radius, phi, lambda);
        } else {
            _trajectory = new Line(r0, phi, lambda);
        }
    }

    public Trajectory getTrajectory() {
        return this._trajectory;
    }

    /**
     * Get the MC Particle associated with the HelicalTrackFit
     *
     * @return mcParticle :
     */
    public MCParticle getMCParticle() {
        return this.mcParticle;
    }

    /**
     * Set the MC Particle associated with the HelicalTrackFit
     *
     * @param mcParticle :
     */
    public void setMCParticle(MCParticle mcParticle) {
        this.mcParticle = mcParticle;
    }
}
