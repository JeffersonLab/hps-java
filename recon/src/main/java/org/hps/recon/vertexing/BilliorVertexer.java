package org.hps.recon.vertexing;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.CoordinateTransformations;
import org.lcsim.constants.Constants;

/**
 * @version $Id: BilliorVertexer.java,v 1.3 2013/03/13 19:24:20 mgraham Exp $
 * @version Vertex tracks using least-squares method laid out by billior etal
 * used in the HPS Java package.
 */
public class BilliorVertexer {
    // the value of the magnetic field in the vicinity of the vertex
    // default is a constant field along the z axis

    private boolean storeCovTrkMomList = false;
    private boolean _debug = false;
    private final double _bField;
    private boolean _beamspotConstraint;
    private boolean _targetConstraint;
    private String _constraintType;
    private final double[] _beamSize = {0.001, 0.01, 0.01}; //10um in y and z
    private final double[] _beamPosition = {0.0, 0.0, 0.0}; //origin
    private final double[] _referencePosition = {0.0, 0.0, 0.0}; //origin
    private int _ntracks;
    private double[] _v0 = {0.0, 0.0, 0.0}; //initial guess for unconstrained vertex fit
    //    private double[] _vertexPosition = {0., 0.0, 0.0};
    private Matrix _vertexPosition;
    private Matrix _covVtx;
    private List<Matrix> _pFit;

    //theta,phi_v,rho
    private List<Matrix> covVtxMomList;
    private Matrix[][] covMomList;//max 2 tracks...just make this bigger for more
    private double _chiSq;

    public BilliorVertexer(double bField) {
        _bField = bField;
        _constraintType = "Unconstrained";
        _beamspotConstraint = false;
        _targetConstraint = false;
    }

    public BilliorVertexer(double bField, boolean bsConst, boolean constToBS) {
        _bField = bField;
        _beamspotConstraint = bsConst;
        _targetConstraint = constToBS;
        if (_beamspotConstraint && _targetConstraint)
            System.out.println("BilliorVertexer::Warning!!!  Setting both _beamspotConstraint and _targetConstraint to true!");
        if (_beamspotConstraint)
            _constraintType = "BeamspotConstrained";
        if (_targetConstraint)
            _constraintType = "TargetConstrained";
    }

    public void setStoreCovTrkMomList(boolean value) {
        storeCovTrkMomList = value;
    }

    public void setDebug(boolean debug) {
        _debug = debug;
    }

    public BilliorVertex fitVertex(List<BilliorTrack> tracks) {
        _ntracks = tracks.size();
        follow1985Paper(tracks);
        if (_beamspotConstraint)
            applyBSconstraint(true);
        else if (_targetConstraint)
            applyBSconstraint(false);
        Map<Integer, Hep3Vector> pFitMap = new HashMap<Integer, Hep3Vector>();
        for (int i = 0; i < tracks.size(); i++) {
            Hep3Vector pFit = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(this.getFittedMomentum(i)));
            pFitMap.put(i, pFit);
        }
//        Hep3Vector vert = new BasicHep3Vector(_vertexPosition.e(0, 0), _vertexPosition.e(1, 0), _vertexPosition.e(2, 0));
// correct for the beamspot position here instead of HPSReconParticle...
//        Hep3Vector vert = new BasicHep3Vector(_vertexPosition.e(0, 0)+_beamPosition[0], _vertexPosition.e(1, 0)+_beamPosition[1], _vertexPosition.e(2, 0)+_beamPosition[2]);
        Hep3Vector vert = new BasicHep3Vector(_vertexPosition.e(0, 0) + _referencePosition[0], _vertexPosition.e(1, 0) + _referencePosition[1], _vertexPosition.e(2, 0) + _referencePosition[2]);
        Hep3Vector vertDet = CoordinateTransformations.transformVectorToDetector(vert);
        SymmetricMatrix covVtxDet = CoordinateTransformations.transformCovarianceToDetector(new SymmetricMatrix(_covVtx));
        BilliorVertex vertex = new BilliorVertex(vertDet, covVtxDet, _chiSq, getInvMass(), pFitMap, _constraintType);
        vertex.setPositionError(CoordinateTransformations.transformVectorToDetector(this.getVertexPositionErrors()));
        vertex.setMassError(this.getInvMassUncertainty());
        List<Matrix> pcov = new ArrayList<Matrix>();
        List<Matrix> tcov = new ArrayList<Matrix>();
        List<double[]> tpars = new ArrayList<double[]>();
        pcov.add(CoordinateTransformations.transformCovarianceToDetector(new SymmetricMatrix(this.getFittedMomentumCovariance(0))));
        pcov.add(CoordinateTransformations.transformCovarianceToDetector(new SymmetricMatrix(this.getFittedMomentumCovariance(1))));
        pcov.add(CoordinateTransformations.transformCovarianceToDetector(new SymmetricMatrix(this.getFittedTrk1Trk2MomCovariance(0, 1))));
        vertex.setTrackMomentumCovariances(pcov);
        vertex.setStoreCovTrkMomList(storeCovTrkMomList);
        vertex.setV0Momentum(CoordinateTransformations.transformVectorToDetector(getV0Momentum()), CoordinateTransformations.transformVectorToDetector(getV0MomentumError()));
        vertex.setV0TargetXY(getV0Projection(), getV0ProjectionError());
        tpars.add(getFittedTrackParameters(0));
        tpars.add(getFittedTrackParameters(1));
        tcov.add(getFittedTrackCovariance(0));
        tcov.add(getFittedTrackCovariance(1));
        vertex.setFittedTrackParameters(tpars);
        vertex.setFittedTrackCovariance(tcov);
        debugMomentumUncertainty(1);
        return vertex;
    }

    /*  Add the constraint that V0 is at/points back to beamspot
     *  this method is based on progressive least squares fit
     *  using the unconstrained fit result as the (k-1) fit
     *
     *  all notation is taken from:
     * W. Hulsbergen, NIM 552 (2005) 566-575
     */
    private void applyBSconstraint(boolean pointback) {
        String methodName = pointback ? "constrainV0toBS" : "constrainV0toTarget";
        BasicMatrix Ckm1 = new BasicMatrix(3 * (_ntracks + 1), 3 * (_ntracks + 1));
        BasicMatrix Xkm1 = new BasicMatrix(3 * (_ntracks + 1), 1);
        MatrixOp.setSubMatrix(Ckm1, _covVtx, 0, 0);
        MatrixOp.setSubMatrix(Xkm1, _vertexPosition, 0, 0);
        int n = 1;
        for (Matrix covVtxMom : covVtxMomList) {
            if (_debug)
                System.out.println(methodName + "::Track " + n + "  covVtxMom : " + covVtxMom.toString());
            MatrixOp.setSubMatrix(Ckm1, covVtxMom, 0, 3 * n);
            MatrixOp.setSubMatrix(Ckm1, MatrixOp.transposed(covVtxMom), 3 * n, 0);
            n++;
        }
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix pi = (BasicMatrix) _pFit.get(i);
            MatrixOp.setSubMatrix(Xkm1, pi, 3 * (i + 1), 0);
//            if (_debug)
//                System.out.println(methodName + "::Track " + i + "  p : " + pi.toString());
            for (int j = 0; j < _ntracks; j++)
                MatrixOp.setSubMatrix(Ckm1, covMomList[i][j], 3 * (i + 1), 3 * (j + 1));
        }

        //  now calculate the derivative matrix for the beam constraint.
        //  the beamspot is assumed to be at _beamPosition 
        //  the V0 production position is Vbvec=(0,-(ptot_y)/(ptot_x)*Vx+Vy, -(ptot_z)/(ptot_x)*Vx+Vz)
        //  where ptot=sum_i (pi)
        //  need derivites wrt to the vertex position and momentum (theta,phi_v,rho)
        double Vx = _vertexPosition.e(0, 0);
        double Vy = _vertexPosition.e(1, 0);
        double Vz = _vertexPosition.e(2, 0);
        if (_debug) {
            //mg 2/28/19 ... these two should be close for the refit (z reference position (track frame) = 0 is correct)
            System.out.println(methodName + "::unconstrained vertexPosition = (" + Vx + "," + Vy + "," + Vz + ")");
            System.out.println(methodName + "::referencePosition = (" + _referencePosition[0] + "," + _referencePosition[1] + "," + _referencePosition[2] + ")");

        }
//add in the reference position about which vertex position is calculated...mg 2/28/19, this was commented out and I think it should be so...good....actually, maybe should be in???  
//But I put it in the matrix calculations, so ok!?
//        double Vx = _vertexPosition.e(0, 0) + _referencePosition[0];
//        double Vy = _vertexPosition.e(1, 0) + _referencePosition[1];;
//        double Vz = _vertexPosition.e(2, 0) + _referencePosition[2];;
        //first, get the sum of momenta...
        double pxtot = 0;
        double pytot = 0;
        double pztot = 0;
//use getFittedMometum here???  Better, getV0Momentum
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix pi = (BasicMatrix) _pFit.get(i);
            double theta = pi.e(0, 0);
            double phiv = pi.e(1, 0);
            double rho = pi.e(2, 0);
            double Pt = Math.abs((1. / rho) * _bField * Constants.fieldConversion);
            double px = Pt * Math.cos(phiv);
            double py = Pt * Math.sin(phiv);
            double pz = Pt * 1 / Math.tan(theta);
            pxtot += px;
            pytot += py;
            pztot += pz;
        }
        //calculate the position of the A' at X=Target
        BasicMatrix rk = makeRk(Vx, Vy, Vz, pxtot, pytot, pztot, pointback);
        if (_debug)
            System.out.println(methodName + "::rk = " + rk);

        //mg...makeHkFixed is for our debugging..replace makeHk with that when we 
        // are sure that it's correct...mg 2/28/19...maybe do that soon? 
        BasicMatrix Hk = makeHkFixed(Vx, pxtot, pytot, pztot, pointback);
        //        BasicMatrix Hk = makeHk(Vx, pxtot, pytot, pztot, pointback);

        if (_debug)
            System.out.println(methodName + "::Hk = " + Hk);

        // the beam covariance...no crossterms but easy to include
        BasicMatrix Vk = new BasicMatrix(3, 3);
        Vk.setElement(0, 0, _beamSize[0] * _beamSize[0]);
        Vk.setElement(1, 1, _beamSize[1] * _beamSize[1]);
        Vk.setElement(2, 2, _beamSize[2] * _beamSize[2]);

        //now do the matrix operations to get the constrained parameters
        BasicMatrix Hkt = (BasicMatrix) MatrixOp.transposed(Hk);
        if (_debug)
            System.out.println(methodName + "::Ckm1Hk = " + MatrixOp.mult(Ckm1, Hk));

        BasicMatrix Rk = (BasicMatrix) MatrixOp.mult(Hkt, MatrixOp.mult(Ckm1, Hk));
        if (_debug)
            System.out.println("Pre Vk:  Rk = " + Rk.toString());
        Rk = (BasicMatrix) MatrixOp.add(Rk, Vk);
        if (_debug)
            System.out.println("Post Vk:  Rk = " + Rk.toString());
        BasicMatrix Rkinv = (BasicMatrix) MatrixOp.inverse(Rk);
        BasicMatrix Kk = (BasicMatrix) MatrixOp.mult(Ckm1, MatrixOp.mult(Hk, Rkinv));

        Matrix _constrainedFit = MatrixOp.mult(Kk, rk);
        _constrainedFit = MatrixOp.add(_constrainedFit, Xkm1);//Xk

        //ok, get the new covariance
        BasicMatrix RkKkt = (BasicMatrix) MatrixOp.mult(Rk, MatrixOp.transposed(Kk));
        BasicMatrix HkCkm1 = (BasicMatrix) MatrixOp.mult(Hkt, Ckm1);
        RkKkt = (BasicMatrix) MatrixOp.mult(1, RkKkt);
        HkCkm1 = (BasicMatrix) MatrixOp.mult(-2, HkCkm1);
        BasicMatrix sumMatrix = (BasicMatrix) MatrixOp.mult(Kk, MatrixOp.add(HkCkm1, RkKkt));
        Matrix _constrainedCov = (BasicMatrix) MatrixOp.add(Ckm1, sumMatrix);

        //update the regular parameter names to the constrained result
        _vertexPosition = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedFit, 0, 0, 3, 1);

        if (_debug)
            System.out.println(methodName + "  Constrained vertex: " + _vertexPosition);

        //update the covariance matrices and fitted momenta
        _covVtx = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedCov, 0, 0, 3, 3);
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix ptmp = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedFit, 3 * (i + 1), 0, 3, 1);
            _pFit.set(i, ptmp);
            covVtxMomList.set(i, (BasicMatrix) MatrixOp.getSubMatrix(_constrainedCov, 0, 3 * (i + 1), 3, 3));
            for (int j = 0; j < _ntracks; j++)
                covMomList[i][j] = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedCov, 3 * (i + 1), 3 * (j + 1), 3, 3);;
        }

        if (_debug) {
            System.out.println(_constraintType + "  Chisq contribution: " + MatrixOp.mult(MatrixOp.transposed(rk), MatrixOp.mult(Rkinv, rk)));
            if (MatrixOp.mult(MatrixOp.transposed(rk), MatrixOp.mult(Rkinv, rk)).e(0, 0) > 1000 && pointback)
                System.out.println(" BIG CHISQ CONTRIBUTION!!!!!!");
        }
        _chiSq += MatrixOp.mult(MatrixOp.transposed(rk), MatrixOp.mult(Rkinv, rk)).e(0, 0);

    }

///// this is not correct;  use makeHkFixed...leave here for posterity 
    private BasicMatrix makeHk(double Vx, double pxtot, double pytot, double pztot, boolean bscon) {
        BasicMatrix Hk = new BasicMatrix(3 * (_ntracks + 1), 3);
        //  ok, can set the derivitives wrt to V
        if (bscon) {
            Hk.setElement(0, 0, 0);
            Hk.setElement(0, 1, pytot / pxtot);
            Hk.setElement(0, 2, pztot / pxtot);
        } else {
            Hk.setElement(0, 0, 1);
            Hk.setElement(0, 1, 0);
            Hk.setElement(0, 2, 0);
        }
        Hk.setElement(1, 0, 0);
        Hk.setElement(1, 1, 1);
        Hk.setElement(1, 2, 0);
        Hk.setElement(2, 0, 0);
        Hk.setElement(2, 1, 0);
        Hk.setElement(2, 2, 1);
        //ok, loop over tracks again to set the derivitives wrt track momenta (theta,phi,rho)
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix pi = (BasicMatrix) _pFit.get(i);
            double theta = pi.e(0, 0);
            double phiv = pi.e(1, 0);
            double rho = pi.e(2, 0);
            double Pt = Math.abs((1. / rho) * _bField * Constants.fieldConversion);
            //            double px = Pt * Math.cos(phiv);
            //            double py = Pt * Math.sin(phiv);
            //            double pz = Pt * 1 / Math.tan(theta);
            //derivities wrt theta
            Hk.setElement(3 * (i + 1), 0, 0);
            Hk.setElement(3 * (i + 1), 1, 0);
            if (bscon)
                Hk.setElement(3 * (i + 1), 2,
                        -Pt / Math.pow(sin(theta), 2) * (Vx - _beamPosition[0]));
            else
                Hk.setElement(3 * (i + 1), 2, 0);
            //derivities wrt phi
            Hk.setElement(3 * (i + 1) + 1, 0, 0);
            if (bscon) {
                Hk.setElement(3 * (i + 1) + 1, 1,
                        (Pt * Pt * cos(phiv) * sin(phiv) / (pxtot * pxtot)) * (Vx - _beamPosition[0]));
                Hk.setElement(3 * (i + 1) + 1, 2,
                        (Pt * sin(phiv) / (pxtot * pxtot)) * (Vx - _beamPosition[0]) * pztot);
            } else {
                Hk.setElement(3 * (i + 1) + 1, 1, 0);
                Hk.setElement(3 * (i + 1) + 1, 2, 0);
            }
            //derivities wrt rho
            Hk.setElement(3 * (i + 1) + 2, 0, 0);
            //            Hk.setElement(3 * (i + 1) + 2, 1,
            //                    (pytot / pxtot - 1) * (Pt / rho) * (1 / pxtot) * Vx);
            //            Hk.setElement(3 * (i + 1) + 2, 2,
            //                    (pztot / pxtot - 1) * (Pt / rho) * (1 / pxtot) * Vx);
            if (bscon) {
                Hk.setElement(3 * (i + 1) + 2, 1,
                        (cos(phiv) * pytot / pxtot - sin(phiv)) * (Pt / rho) * (1 / pxtot) * (Vx - _beamPosition[0]));
                Hk.setElement(3 * (i + 1) + 2, 2,
                        (cos(phiv) * pztot / pxtot - sin(phiv)) * (Pt / rho) * (1 / pxtot) * (Vx - _beamPosition[0]));
            } else {
                Hk.setElement(3 * (i + 1) + 2, 1, 0);
                Hk.setElement(3 * (i + 1) + 2, 2, 0);
            }
            //                   if(_debug)System.out.println("pxtot = "+pxtot+"; rho = "+rho+"; Pt = "+Pt);
            //                   if(_debug)System.out.println("cos(phiv)*pytot / pxtot - sin(phiv) = "+(cos(phiv)*pytot / pxtot - sin(phiv)));
            //                   if(_debug)System.out.println("Pt/(rho*pxtot) = "+(Pt / rho) * (1 / pxtot));
        }
        return Hk;
    }

    private BasicMatrix makeHkFixed(double Vx, double pxtot, double pytot, double pztot, boolean bscon) {
        BasicMatrix Hk = new BasicMatrix(3 * (_ntracks + 1), 3);
        //derivitives wrt to V
        if (bscon) {
            Hk.setElement(0, 0, 0);
            Hk.setElement(0, 1, -pytot / pxtot);
            Hk.setElement(0, 2, -pztot / pxtot);
        } else {
            Hk.setElement(0, 0, 1);
            Hk.setElement(0, 1, 0);
            Hk.setElement(0, 2, 0);
        }
        Hk.setElement(1, 0, 0);
        Hk.setElement(1, 1, 1);
        Hk.setElement(1, 2, 0);
        Hk.setElement(2, 0, 0);
        Hk.setElement(2, 1, 0);
        Hk.setElement(2, 2, 1);

        //store the track parameters
        double theta[] = {0, 0};
        double phiv[] = {0, 0};
        double rho[] = {0, 0};
        double Pt[] = {0, 0};
        double px[] = {0, 0};
        double py[] = {0, 0};
        double pz[] = {0, 0};

        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix pi = (BasicMatrix) _pFit.get(i);
            theta[i] = pi.e(0, 0);
            phiv[i] = pi.e(1, 0);
            rho[i] = pi.e(2, 0);
            Pt[i] = Math.abs((1. / rho[i]) * _bField * Constants.fieldConversion);
            px[i] = Pt[i] * Math.cos(phiv[i]);
            py[i] = Pt[i] * Math.sin(phiv[i]);
            pz[i] = Pt[i] * 1 / Math.tan(theta[i]);
        }

        //derivatives wrt theta
        Hk.setElement(3, 0, 0);
        Hk.setElement(3, 1, 0);
        Hk.setElement(6, 0, 0);
        Hk.setElement(6, 1, 0);
        if (bscon) {
//            Hk.setElement(3, 2, -(((_beamPosition[0] - Vx) * Math.pow(1 / Math.sin(theta[0]), 2)) / (px[1] * rho[0] + Math.cos(phiv[0]))));
//            Hk.setElement(6, 2, -(((_beamPosition[0] - Vx) * Math.pow(1 / Math.sin(theta[1]), 2)) / (px[0] * rho[1] + Math.cos(phiv[1]))));
//            Hk.setElement(3, 2, -(((_beamPosition[0] + _referencePosition[0] - Vx) * Math.pow(1 / Math.sin(theta[0]), 2)) / (px[1] * rho[0] + Math.cos(phiv[0]))));
//.            Hk.setElement(6, 2, -(((_beamPosition[0] + _referencePosition[0] - Vx) * Math.pow(1 / Math.sin(theta[1]), 2)) / (px[0] * rho[1] + Math.cos(phiv[1]))));
            Hk.setElement(3, 2, -(((_beamPosition[0] - (Vx + _referencePosition[0])) * Math.pow(1 / Math.sin(theta[0]), 2)) / (px[1] * rho[0] + Math.cos(phiv[0]))));
            Hk.setElement(6, 2, -(((_beamPosition[0] - (Vx + _referencePosition[0])) * Math.pow(1 / Math.sin(theta[1]), 2)) / (px[0] * rho[1] + Math.cos(phiv[1]))));

//          Hk.setElement(3, 2, (Vx - _beamPosition[0])/(pxtot)*Pt[0]*Math.pow(1./Math.sin(theta[0]), 2));
// Hk.setElement(6, 2, (Vx - _beamPosition[0])/(pxtot)*Pt[1]*Math.pow(1./Math.sin(theta[1]), 2)); 
        } else {
            Hk.setElement(3, 2, 0);
            Hk.setElement(6, 2, 0);
        }

        //derivatives wrt phi
        Hk.setElement(4, 0, 0);
        Hk.setElement(4, 1, 0);
        Hk.setElement(7, 0, 0);
        Hk.setElement(7, 1, 0);
        if (bscon) {
//            Hk.setElement(4, 1, ((_beamPosition[0] - Vx) * (1 + px[1] * rho[0] * Math.cos(phiv[0]) + py[1] * rho[0] * Math.sin(phiv[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(4, 2, ((_beamPosition[0] - Vx) * (pz[1] * rho[0] + 1 / Math.tan(theta[0])) * Math.sin(phiv[0])) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(7, 1, ((_beamPosition[0] - Vx) * (1 + px[0] * rho[1] * Math.cos(phiv[1]) + py[0] * rho[1] * Math.sin(phiv[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
//            Hk.setElement(7, 2, ((_beamPosition[0] - Vx) * (pz[0] * rho[1] + 1 / Math.tan(theta[1])) * Math.sin(phiv[1])) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
//            Hk.setElement(4, 1, ((_beamPosition[0] + _referencePosition[0] - Vx) * (1 + px[1] * rho[0] * Math.cos(phiv[0]) + py[1] * rho[0] * Math.sin(phiv[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(4, 2, ((_beamPosition[0] + _referencePosition[0] - Vx) * (pz[1] * rho[0] + 1 / Math.tan(theta[0])) * Math.sin(phiv[0])) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(7, 1, ((_beamPosition[0] + _referencePosition[0] - Vx) * (1 + px[0] * rho[1] * Math.cos(phiv[1]) + py[0] * rho[1] * Math.sin(phiv[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
//            Hk.setElement(7, 2, ((_beamPosition[0] + _referencePosition[0] - Vx) * (pz[0] * rho[1] + 1 / Math.tan(theta[1])) * Math.sin(phiv[1])) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
            Hk.setElement(4, 1, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (1 + px[1] * rho[0] * Math.cos(phiv[0]) + py[1] * rho[0] * Math.sin(phiv[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
            Hk.setElement(4, 2, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (pz[1] * rho[0] + 1 / Math.tan(theta[0])) * Math.sin(phiv[0])) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
            Hk.setElement(7, 1, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (1 + px[0] * rho[1] * Math.cos(phiv[1]) + py[0] * rho[1] * Math.sin(phiv[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
            Hk.setElement(7, 2, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (pz[0] * rho[1] + 1 / Math.tan(theta[1])) * Math.sin(phiv[1])) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));

        } else {
            Hk.setElement(4, 1, 0);
            Hk.setElement(7, 1, 0);
            Hk.setElement(4, 2, 0);
            Hk.setElement(7, 2, 0);
        }

        //derivatives wrt rho
        Hk.setElement(5, 0, 0);
        Hk.setElement(5, 1, 0);
        Hk.setElement(8, 0, 0);
        Hk.setElement(8, 1, 0);
        if (bscon) {
//            Hk.setElement(5, 1, ((_beamPosition[0] - Vx) * (py[1] * Math.cos(phiv[0]) - px[1] * Math.sin(phiv[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(5, 2, ((_beamPosition[0] - Vx) * (pz[1] * Math.cos(phiv[0]) - px[1] * 1 / Math.tan(theta[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(8, 1, ((_beamPosition[0] - Vx) * (py[0] * Math.cos(phiv[1]) - px[0] * Math.sin(phiv[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
//            Hk.setElement(8, 2, ((_beamPosition[0] - Vx) * (pz[0] * Math.cos(phiv[1]) - px[0] * 1 / Math.tan(theta[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
//            
//            Hk.setElement(5, 1, ((_beamPosition[0] + _referencePosition[0] - Vx) * (py[1] * Math.cos(phiv[0]) - px[1] * Math.sin(phiv[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(5, 2, ((_beamPosition[0] + _referencePosition[0] - Vx) * (pz[1] * Math.cos(phiv[0]) - px[1] * 1 / Math.tan(theta[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
//            Hk.setElement(8, 1, ((_beamPosition[0] + _referencePosition[0] - Vx) * (py[0] * Math.cos(phiv[1]) - px[0] * Math.sin(phiv[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
//            Hk.setElement(8, 2, ((_beamPosition[0] + _referencePosition[0] - Vx) * (pz[0] * Math.cos(phiv[1]) - px[0] * 1 / Math.tan(theta[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
            Hk.setElement(5, 1, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (py[1] * Math.cos(phiv[0]) - px[1] * Math.sin(phiv[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
            Hk.setElement(5, 2, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (pz[1] * Math.cos(phiv[0]) - px[1] * 1 / Math.tan(theta[0]))) / Math.pow(px[1] * rho[0] + Math.cos(phiv[0]), 2));
            Hk.setElement(8, 1, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (py[0] * Math.cos(phiv[1]) - px[0] * Math.sin(phiv[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));
            Hk.setElement(8, 2, ((_beamPosition[0] - (Vx + _referencePosition[0])) * (pz[0] * Math.cos(phiv[1]) - px[0] * 1 / Math.tan(theta[1]))) / Math.pow(px[0] * rho[1] + Math.cos(phiv[1]), 2));

            //            Hk.setElement(5, 1, -(Vx - _beamPosition[0]) / Math.pow(pxtot * rho[0], 2) * (-pxtot * Math.sin(phi[0]) + pytot * Math.cos(phi[0])));
            //            Hk.setElement(8, 1, -(Vx - _beamPosition[0]) / Math.pow(pxtot * rho[1], 2) * (-pxtot * Math.sin(phi[1]) + pytot * Math.cos(phi[1])));
            //            Hk.setElement(5, 2, -(Vx - _beamPosition[0]) / Math.pow(pxtot * rho[0], 2) * (-pxtot / Math.tan(theta[0]) + pztot * Math.cos(phi[0])));
            //            Hk.setElement(8, 2, -(Vx - _beamPosition[0]) / Math.pow(pxtot * rho[0], 2) * (-pxtot / Math.tan(theta[0]) + pztot * Math.cos(phi[0])));
        } else {
            Hk.setElement(5, 1, 0);
            Hk.setElement(8, 1, 0);
            Hk.setElement(5, 2, 0);
            Hk.setElement(8, 2, 0);
        }

        return Hk;
    }

    /*
    *    rK is the residual of the projected beamspot to the target
     */
    private BasicMatrix makeRk(double Vx, double Vy, double Vz, double pxtot, double pytot, double pztot, boolean bscon) {
        //calculate the position of the A' at X=beamspot
        BasicMatrix rk = new BasicMatrix(3, 1);
        if (_debug) {
            System.out.println("makeRk::Vx = " + Vx + "; Vy = " + Vy + "; Vz = " + Vz + "; pxtot = " + pxtot + "; pytot = " + pytot + "; pztot = " + pztot);
            System.out.println("makeRk::beamspot = (" + _beamPosition[0] + ", " + _beamPosition[1] + ", " + _beamPosition[2] + ")");
        }
        if (bscon) {
            rk.setElement(0, 0, 0);  ///!!!!  Ah...I think this is right!  we are projecting to the target X (Z in detector, so no residual here)           
//            rk.setElement(1, 0, _beamPosition[1] - (Vy - pytot / pxtot * (Vx - _beamPosition[0])));
//            rk.setElement(2, 0, _beamPosition[2] - (Vz - pztot / pxtot * (Vx - _beamPosition[0])));
// mg 2/28/19 ... these should be referenced to reference position...???
            rk.setElement(1, 0, _beamPosition[1] - ((Vy + _referencePosition[1]) - pytot / pxtot * (Vx - _beamPosition[0] + _referencePosition[0])));
            rk.setElement(2, 0, _beamPosition[2] - ((Vz + _referencePosition[2]) - pztot / pxtot * (Vx - _beamPosition[0] + _referencePosition[0])));
        } else {
            rk.setElement(0, 0, _beamPosition[0] - Vx);
            rk.setElement(1, 0, _beamPosition[1] - Vy);
            rk.setElement(2, 0, _beamPosition[2] - Vz);
        }
        if (_debug)
            System.out.println("makeRk::rk = (" + rk.e(0, 0) + ", " + rk.e(1, 0) + ", " + rk.e(2, 0) + ")");
        return rk;
    }
//
//    public void setV0(double[] v0) {
//        _v0 = v0;
//    }

    public void setBeamSize(double[] bs) {
        _beamSize[0] = bs[0];
        _beamSize[1] = bs[1];
        _beamSize[2] = bs[2];
    }

    public void setBeamPosition(double[] bp) {
        _beamPosition[0] = bp[0];
        _beamPosition[1] = bp[1];
        _beamPosition[2] = bp[2];
    }

    public void setReferencePosition(double[] rp) {
        _referencePosition[0] = rp[0];
        _referencePosition[1] = rp[1];
        _referencePosition[2] = rp[2];
    }

    public void doBeamSpotConstraint(boolean bsconst) {
        _beamspotConstraint = bsconst;
        _targetConstraint = false;
        if (bsconst == true)
            _constraintType = "BeamspotConstrained";

    }

    public void doTargetConstraint(boolean tconst) {
        _beamspotConstraint = false;
        _targetConstraint = tconst;
        if (tconst == true)
            _constraintType = "TargetConstrained";
    }

    public double getChiSq() {
        return _chiSq;
    }

    //double[]...should fix this to return Hep3Vector?
    public double[] getFittedMomentum(int index) {
        BasicMatrix pi = (BasicMatrix) _pFit.get(index);
        double[] mom = {0, 0, 0};
        double theta = pi.e(0, 0);
        double phiv = pi.e(1, 0);
        double rho = pi.e(2, 0);
        double Pt = Math.abs((1. / rho) * _bField * Constants.fieldConversion);
        mom[0] = Pt * Math.cos(phiv);
        mom[1] = Pt * Math.sin(phiv);
        mom[2] = Pt * 1 / Math.tan(theta);
//        if (_debug) {
//            System.out.println("getFittedMomentum::  " + mom[0] + "; " + mom[1] + "; " + mom[2]);
//            System.out.println("pT= " + Pt + "; phi = " + phiv + "; B = " + _bField);
//        }
        return mom;
    }

    private void debugMomentumUncertainty(int index) {
        BasicMatrix pi = (BasicMatrix) _pFit.get(index);
        double[] mom = getFittedMomentum(index);
        double theta = pi.e(0, 0);
        double phiv = pi.e(1, 0);
        double rho = pi.e(2, 0);
        BasicMatrix covpi = (BasicMatrix) covMomList[index][index];
        double c20 = covpi.e(2, 0);
        double c22 = covpi.e(2, 2);

        double B = _bField * Constants.fieldConversion;
        double pz2c22 = c22 * B * B / (Math.pow(Math.tan(theta), 2) * Math.pow(rho, 4));
        double pz2c20 = c20 * B * B / (Math.pow(Math.sin(theta), 4) * Math.pow(rho, 2));

        double sigmaRhoOverRho = Math.sqrt(c22) / rho;
        double sigmaPzOverPz = Math.sqrt(pz2c22 + pz2c20) / mom[0];//mom[] is in tracking coordinates
        double pzErrFromHere = Math.sqrt(pz2c22 + pz2c20);
        Matrix fitMomCov = getFittedMomentumCovariance(index);
        double pzErrFromMethod = Math.sqrt(fitMomCov.e(2, 2));

        //System.out.println("debugMomentumUncertainty::(theta,phiv,rho) =  (" + theta + "; " + phiv + "; " + rho + ")");
        //System.out.println("debugMomentumUncertainty::  " + mom[0] + "; " + mom[1] + "; " + mom[2]);
        //System.out.println("debugMomentumUncertainty::(c20,c22) =  (" + c20 + "; " + c22 + ")");
        //System.out.println("debugMomentumUncertainty::(pz2c20,pz2c22) =  (" + pz2c20 + "; " + pz2c22 + ")");
        //System.out.println("debugMomentumUncertainty::(pzErrFromHere,pzErrFromMethod) = ("+pzErrFromHere+"; "+pzErrFromMethod+")");
        //System.out.println("debugMomentumUncertainty::(sigma(rho)/rho,sigma(pz)/pz) =  (" + sigmaRhoOverRho + "; " + sigmaPzOverPz + ")");
    }

    //return fitted track parameters (theta,phiv,rho) for track index i
    public double[] getFittedTrackParameters(int index) {
        BasicMatrix pi = (BasicMatrix) _pFit.get(index);
        double[] mom = {pi.e(0, 0), pi.e(1, 0), pi.e(2, 0)};
        return mom;
    }

    public BasicMatrix getFittedTrackCovariance(int index) {
        return (BasicMatrix) covMomList[index][index];
    }

    public Matrix getFittedMomentumCovariance(int index) {
        BasicMatrix pi = (BasicMatrix) _pFit.get(index);
        BasicMatrix covpi = (BasicMatrix) covMomList[index][index]; //off diagonal matrices are the track-track covariances
//        System.out.println("covpi "+covpi.toString());
        double theta = pi.e(0, 0);
        double phiv = pi.e(1, 0);
        double rho = pi.e(2, 0);
        BasicMatrix Jac = (BasicMatrix) getJacobianThetaPhiRhoToPxPyPz(theta, phiv, rho);
        BasicMatrix JacT = (BasicMatrix) MatrixOp.transposed(Jac);
//        System.out.println("Jac "+Jac.toString());       
        return MatrixOp.mult(Jac, MatrixOp.mult(covpi, JacT));  //I think this is the correct way
    }

    public Matrix getFittedTrk1Trk2MomCovariance(int ind1, int ind2) {
        BasicMatrix p1 = (BasicMatrix) _pFit.get(ind1);
        BasicMatrix p2 = (BasicMatrix) _pFit.get(ind2);
        BasicMatrix covpi = (BasicMatrix) covMomList[ind1][ind2]; //off diagonal matrices are the track-track covariances
//        System.out.println("covpi "+covpi.toString());
        double theta1 = p1.e(0, 0);
        double phiv1 = p1.e(1, 0);
        double rho1 = p1.e(2, 0);
        double theta2 = p2.e(0, 0);
        double phiv2 = p2.e(1, 0);
        double rho2 = p2.e(2, 0);
        BasicMatrix Jac1 = (BasicMatrix) getJacobianThetaPhiRhoToPxPyPz(theta1, phiv1, rho1);
        BasicMatrix Jac2 = (BasicMatrix) getJacobianThetaPhiRhoToPxPyPz(theta2, phiv2, rho2);
        BasicMatrix Jac2T = (BasicMatrix) MatrixOp.transposed(Jac2);
//        System.out.println("Jac "+Jac.toString());
        return MatrixOp.mult(Jac1, MatrixOp.mult(covpi, Jac2T));
//        return MatrixOp.mult(Jac1, MatrixOp.mult(covpi, Jac2T));

    }

    public Matrix getFittedVertexCovariance() {
        return _covVtx;
    }

    public Hep3Vector getVertexPositionErrors() {
        return new BasicHep3Vector(Math.sqrt(_covVtx.e(0, 0)),
                Math.sqrt(_covVtx.e(1, 1)), Math.sqrt(_covVtx.e(2, 2)));
    }

    public double getInvMass() {
        double esum = 0.;
        double pxsum = 0.;
        double pysum = 0.;
        double pzsum = 0.;
        double me = 0.000511;
        // Loop over tracks
        for (int i = 0; i < _ntracks; i++) {
            double[] p = getFittedMomentum(i);
            double p1x = p[0];
            double p1y = p[1];
            double p1z = p[2];
            double p1mag2 = p1x * p1x + p1y * p1y + p1z * p1z;
            double e1 = Math.sqrt(p1mag2 + me * me);
            pxsum += p1x;
            pysum += p1y;
            pzsum += p1z;
            esum += e1;
        }
        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0)
            return Math.sqrt(evtmass);
        else
            return -99;
    }

    //get the uncertaintly on the invariant mass 
    //curretnly only uses the diagonal terms in covaraince...   
    //I repeat a lot of code above; should just calculate these
    //as part of vertex fit, have a private _mass; _massErr variables
    //and use getters just to forward them 
    public double getInvMassUncertainty() {
        //get what we need
        double[] p1 = getFittedMomentum(0);
        double[] p2 = getFittedMomentum(1);
        Matrix cov1 = getFittedMomentumCovariance(0);
        Matrix cov2 = getFittedMomentumCovariance(1);
        //fill in some local variables
        double esum = 0.;
        double pxsum = 0.;
        double pysum = 0.;
        double pzsum = 0.;
        double me = 0.000511;
        double p1x = p1[0];
        double p1y = p1[1];
        double p1z = p1[2];
        double p2x = p2[0];
        double p2y = p2[1];
        double p2z = p2[2];
        // some useful quantities
        double p1mag2 = p1x * p1x + p1y * p1y + p1z * p1z;
        double e1 = Math.sqrt(p1mag2 + me * me);
        double p2mag2 = p2x * p2x + p2y * p2y + p2z * p2z;
        double e2 = Math.sqrt(p2mag2 + me * me);
        pxsum = p1x + p2x;
        pysum = p1y + p2y;
        pzsum = p1z + p2z;
        esum = e1 + e2;
        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;
        //lots of terms here...do in pieces
        double mErrOverMSq = Math.pow((p2x * e1 - p1x * e2) / e1, 2) * cov1.e(0, 0);
        mErrOverMSq += Math.pow((p2x * e1 - p1x * e2) / e2, 2) * cov2.e(0, 0);
        mErrOverMSq += Math.pow((p2y * e1 - p1y * e2) / e1, 2) * cov1.e(1, 1);
        mErrOverMSq += Math.pow((p2y * e1 - p1y * e2) / e2, 2) * cov2.e(1, 1);
        mErrOverMSq += Math.pow((p2z * e1 - p1z * e2) / e1, 2) * cov1.e(2, 2);
        mErrOverMSq += Math.pow((p2z * e1 - p1z * e2) / e2, 2) * cov2.e(2, 2);
        mErrOverMSq /= 4 * Math.pow(p1x * p2x + p1y * p2y + p1z * p2z - (me * me + e1 * e2), 2);
//        System.out.println("getInvMass() = "+getInvMass()+"; evtmass="+Math.sqrt(evtmass)+";  error ="+Math.sqrt(mErrOverMSq*evtmass));
        //return the uncertainty
        return Math.sqrt(mErrOverMSq * evtmass);
    }

    /*   mg  5/7/2018
        get V0 momentum 
     */
    public Hep3Vector getV0Momentum() {
        double[] p1 = getFittedMomentum(0);
        double[] p2 = getFittedMomentum(1);
        //System.out.println("getFittedMomentum track1::  " + p1[0] + "; " + p1[1] + "; " + p1[2]);
        //System.out.println("getFittedMomentum track2::  " + p2[0] + "; " + p2[1] + "; " + p2[2]);
        return new BasicHep3Vector(p1[0] + p2[0], p1[1] + p2[1], p1[2] + p2[2]);

    }

    /*   mg  5/7/2018
        get V0 momentum uncertainty (just sigma_x,y,z...no correlations)
     */
    public Hep3Vector getV0MomentumError() {
        Matrix covMom1 = getFittedMomentumCovariance(0);
        Matrix covMom2 = getFittedMomentumCovariance(1);
        Matrix covMom12 = getFittedTrk1Trk2MomCovariance(0, 1);
        double pxErr = Math.sqrt(covMom1.e(0, 0) + covMom2.e(0, 0) + 2 * covMom12.e(0, 0));
        double pyErr = Math.sqrt(covMom1.e(1, 1) + covMom2.e(1, 1) + 2 * covMom12.e(1, 1));
        double pzErr = Math.sqrt(covMom1.e(2, 2) + covMom2.e(2, 2) + 2 * covMom12.e(2, 2));
        //System.out.println("px1Err = " + Math.sqrt(covMom1.e(0, 0)) + "; px2Err = " + Math.sqrt(covMom2.e(0, 0)) + "; px12Err = " + covMom12.e(0, 0));
        //System.out.println("py1Err = " + Math.sqrt(covMom1.e(1, 1)) + "; py2Err = " + Math.sqrt(covMom2.e(1, 1)) + "; py12Err = " + covMom12.e(1, 1));
        //System.out.println("pz1Err = " + Math.sqrt(covMom1.e(2, 2)) + "; pz2Err = " + Math.sqrt(covMom2.e(2, 2)) + "; pz12Err = " + covMom12.e(2, 2));
        return new BasicHep3Vector(pxErr, pyErr, pzErr);
    }

    /*   mg  5/4/2018
        get the V0 xy projection back to the target    
        ... flip the coordinate systems here (tracking->detector:  x->z, y->x, z->y)
     */
    public double[] getV0Projection() {
        double[] p1 = getFittedMomentum(0);
        double[] p2 = getFittedMomentum(1);
        //calculate a few useful quantities  (remember!  flipping coordinates!)
        double pvZ = p1[0] + p2[0];
        double pvX = p1[1] + p2[1];
        double pvY = p1[2] + p2[2];
        double sX = pvX / pvZ;
        double sY = pvY / pvZ;
        double vZ = _vertexPosition.e(0, 0) + _referencePosition[0];
        double vX = _vertexPosition.e(1, 0) + _referencePosition[1];
        double vY = _vertexPosition.e(2, 0) + _referencePosition[2];
        double delZ = _beamPosition[0] - vZ;
        double[] tXY = {delZ * sX + vX, delZ * sY + vY};
        //System.out.println(_constraintType + ";  delZ = " + delZ + "; sX = " + sX + "; sY = " + sY);
        //System.out.println("vertX = " + vX + ";vertY = " + vY);
        //System.out.println("v0 projection X = " + tXY[0] + "; Y = " + tXY[1]);
        return tXY;
    }

    /*   mg  5/4/2018
        get the V0 xy error on the projection back to the target
        this is done "by hand" so only works for two tracks ...
        >2 tracks, and this calculation gets very long
     */
    public double[] getV0ProjectionError() {
        // get the track momenta
        double[] p1 = getFittedMomentum(0);
        double[] p2 = getFittedMomentum(1);
        //calculate a few useful quantities   (remember!  flipping coordinates!)
        double pvZ = p1[0] + p2[0];
        double pvX = p1[1] + p2[1];
        double pvY = p1[2] + p2[2];
        double sX = pvX / pvZ;
        double sY = pvY / pvZ;
//        double vZ = _vertexPosition.e(0, 0);
//        double vX = _vertexPosition.e(1, 0);
//        double vY = _vertexPosition.e(2, 0);
//   mg 11/1/2018...forgot to add on reference positions!
        double vZ = _vertexPosition.e(0, 0) + _referencePosition[0];
        double vX = _vertexPosition.e(1, 0) + _referencePosition[1];
        double vY = _vertexPosition.e(2, 0) + _referencePosition[2];
        double delZ = _beamPosition[0] - vZ;
        // get all of the covariance matrices we need
        Matrix covMom1 = getFittedMomentumCovariance(0);
        Matrix covMom2 = getFittedMomentumCovariance(1);
        Matrix covMom12 = getFittedTrk1Trk2MomCovariance(0, 1);
        Matrix covVtx = getFittedVertexCovariance();
        Matrix covVtxMom1 = covVtxMomList.get(0);
        Matrix covVtxMom2 = covVtxMomList.get(1);
        int zInd = 0;
        int xInd = 1;
        int yInd = 2;

        //calculate the variance terms for sigmaX^2        
        double sigX2 = sX * sX * covVtx.e(zInd, zInd) + Math.pow(delZ / pvZ, 2) * (covMom1.e(xInd, xInd) + covMom2.e(xInd, xInd)
                + sX * sX * (covMom1.e(zInd, zInd) + covMom2.e(zInd, zInd))) + covVtx.e(xInd, xInd);
        // Vz-p covariances
//        sigX2 += 2 * (sX * delZ / pvZ * (covVtxMom1.e(zInd, xInd) + covVtxMom2.//e(zInd, xInd)
//                + sX * (covVtxMom1.e(zInd, zInd) + covVtxMom2.e(zInd, zInd))));
        sigX2 += 2 * (sX * delZ / pvZ * (-covVtxMom1.e(zInd, xInd) - covVtxMom2.e(zInd, xInd) //  signs get flipped because of my (vz-zt)-->(zt-vz) mistake
                + sX * (covVtxMom1.e(zInd, zInd) + covVtxMom2.e(zInd, zInd))));
        // p-p covariances  (I'm assuming covMom12 == covMom21
        //       sigX2 += 2 * (delZ / Math.pow(pvZ, 2) * (covMom1.e(zInd, xInd) - sX * (covMom1.e(xInd, zInd) + covMom12.e(xInd, zInd)
        //               + covMom12.e(zInd, xInd) + covMom2.e(xInd, zInd)) + Math.pow(sX, 2) * covMom12.e(zInd, zInd)));
        sigX2 += 2 * (delZ / Math.pow(pvZ, 2) * (covMom12.e(xInd, xInd) - sX * (covMom1.e(xInd, zInd) + covMom12.e(xInd, zInd)//  signs get flipped because of my (vz-zt)-->(zt-vz) mistake
                + covMom12.e(zInd, xInd) + covMom2.e(xInd, zInd)) + Math.pow(sX, 2) * covMom12.e(zInd, zInd)));

        // Vx-Vz and Vx-p covariances
        sigX2 += 2 * (-sX * covVtx.e(xInd, zInd) + delZ / pvZ * (covVtxMom1.e(xInd, xInd) + covVtxMom2.e(xInd, xInd)
                - sX * (covVtxMom1.e(xInd, zInd) + covVtxMom2.e(xInd, zInd))));

        //calculate the variance terms for sigmaY^2        
//        double sigY2 = sY * sY * covVtx.e(zInd, zInd) + Math.pow(delZ / pvZ, 2) * (covMom1.e(yInd, yInd) + covMom2.e(yInd, yInd)
//                + sY * sY * (covMom1.e(zInd, zInd) + covMom2.e(zInd, zInd))) + covVtx.e(yInd, yInd);
//        ////calculate the variance terms for sigmaY^2  
//        // Vz-p covariances
//        sigY2 += 2 * (sY * delZ / pvZ * (covVtxMom1.e(zInd, yInd) + covVtxMom2.e(zInd, yInd)
//                + sY * (covVtxMom1.e(zInd, zInd) + covVtxMom2.e(zInd, zInd))));
//        // p-p covariances  (I'm assuming covMom12 == covMom21
//        sigY2 += 2 * (delZ / Math.pow(pvZ, 2) * (covMom1.e(zInd, yInd) - sY * (covMom1.e(yInd, zInd) + covMom12.e(yInd, zInd)
//                + covMom12.e(zInd, yInd) + covMom2.e(yInd, zInd)) + Math.pow(sY, 2) * covMom12.e(zInd, zInd)));
//        // Vy-Vz and Vx-p covariances
//        sigY2 += 2 * (sY * covVtx.e(yInd, zInd) + delZ / pvZ * (covVtxMom1.e(yInd, yInd) + covVtxMom2.e(yInd, yInd)
//                - sY * (covVtxMom1.e(yInd, zInd) + covVtxMom2.e(yInd, zInd))));
//  double sigY2 = sY * sY * covVtx.e(zInd, zInd) + Math.pow(delZ / pvZ, 2) * (covMom1.e(yInd, yInd) + covMom2.e(yInd, yInd)
        //               + sY * sY * (covMom1.e(zInd, zInd) + covMom2.e(zInd, zInd))) + covVtx.e(yInd, yInd);
        double sigY2 = sY * sY * covVtx.e(zInd, zInd) + Math.pow(delZ / pvZ, 2) * (covMom1.e(yInd, yInd) + covMom2.e(yInd, yInd)
                + sY * sY * (covMom1.e(zInd, zInd) + covMom2.e(zInd, zInd))) + covVtx.e(yInd, yInd);
        // Vz-p covariances
//        sigY2 += 2 * (sY * delZ / pvZ * (covVtxMom1.e(zInd, yInd) + covVtxMom2.//e(zInd, yInd)
//                + sY * (covVtxMom1.e(zInd, zInd) + covVtxMom2.e(zInd, zInd))));
        sigY2 += 2 * (sY * delZ / pvZ * (-covVtxMom1.e(zInd, yInd) - covVtxMom2.e(zInd, yInd) //  signs get flipped because of my (vz-zt)-->(zt-vz) mistake
                + sY * (covVtxMom1.e(zInd, zInd) + covVtxMom2.e(zInd, zInd))));
        // p-p covariances  (I'm assuming covMom12 == covMom21
        //       sigY2 += 2 * (delZ / Math.pow(pvZ, 2) * (covMom1.e(zInd, yInd) - sY * (covMom1.e(yInd, zInd) + covMom12.e(yInd, zInd)
        //               + covMom12.e(zInd, yInd) + covMom2.e(yInd, zInd)) + Math.pow(sY, 2) * covMom12.e(zInd, zInd)));
        sigY2 += 2 * (delZ / Math.pow(pvZ, 2) * (covMom12.e(yInd, yInd) - sY * (covMom1.e(yInd, zInd) + covMom12.e(yInd, zInd)//  signs get flipped because of my (vz-zt)-->(zt-vz) mistake
                + covMom12.e(zInd, yInd) + covMom2.e(yInd, zInd)) + Math.pow(sY, 2) * covMom12.e(zInd, zInd)));

        // Vx-Vz and Vx-p covariances
        sigY2 += 2 * (-sY * covVtx.e(yInd, zInd) + delZ / pvZ * (covVtxMom1.e(yInd, yInd) + covVtxMom2.e(yInd, yInd)
                - sY * (covVtxMom1.e(yInd, zInd) + covVtxMom2.e(yInd, zInd))));

        double[] sigXY = {Math.sqrt(sigX2), Math.sqrt(sigY2)};
        return sigXY;
    }

    @Override
    public String toString() {
        String sb = "Vertex at : \nx= " + _vertexPosition.e(0, 0) + " +/- " + Math.sqrt(_covVtx.e(0, 0)) + "\ny= " + _vertexPosition.e(1, 0) + " +/- " + Math.sqrt(_covVtx.e(1, 1)) + "\nz= " + _vertexPosition.e(2, 0) + " +/- " + Math.sqrt(_covVtx.e(2, 2));
        return sb;
    }

    /*
    *  The method here follows the 1985 paper from 
    *  Billoir, P., Fruhwirth, R., & Regler, M. (1985). 
    *  "Track element merging strategy and vertex fitting 
    *  in complex modular detectors."
    *  Nucl. Instrum. Methods Phys. Res., A, 241115–131. 42 p.
    *  http://cds.cern.ch/record/1330744
     */
    private void follow1985Paper(List<BilliorTrack> tracks) {

        //initial guess for the vertex
        BasicMatrix v0 = new BasicMatrix(3, 1);
        v0.setElement(0, 0, _v0[0]);
        v0.setElement(1, 0, _v0[1]);
        v0.setElement(2, 0, _v0[2]);
        //  make some arrays we'll need
        List<Matrix> Gs = new ArrayList<Matrix>();
        List<Matrix> Ds = new ArrayList<Matrix>();
        List<Matrix> Es = new ArrayList<Matrix>();
        List<Matrix> As = new ArrayList<Matrix>();
        List<Matrix> Bs = new ArrayList<Matrix>();
        List<Matrix> pis = new ArrayList<Matrix>();
        List<Matrix> cis = new ArrayList<Matrix>();

        BasicMatrix D0 = new BasicMatrix(3, 3);
        boolean firstTrack = true;

        for (BilliorTrack bt : tracks) {
            double[] par = bt.parameters();
//          measured track parameters, but them in a matrix
            BasicMatrix tmpPar = new BasicMatrix(5, 1);
            tmpPar.setElement(0, 0, par[0]);
            tmpPar.setElement(1, 0, par[1]);
            tmpPar.setElement(2, 0, par[2]);
            tmpPar.setElement(3, 0, par[3]);
            tmpPar.setElement(4, 0, par[4]);
//          the measured quantities in terms of the  vertex, v0. 
//          parameterization taken from: 
//          Billoir & Qian, "Fast vertex fitting witha  local parameterization of tracks", 
//          NIM A311 (1992) 139-150. 
//          BE CAREFUL, this paper has a sign error in the dz/dtheta derivative 
//          corrected in erratum NIM A350 (1994) 624.  
            double theta = par[2];
            double rho = par[4];
            double cotth = 1. / tan(par[2]);
            double uu = v0.e(0, 0) * cos(par[3]) + v0.e(1, 0) * sin(par[3]);//Q
            double vv = v0.e(1, 0) * cos(par[3]) - v0.e(0, 0) * sin(par[3]);//R
            double eps = -vv - .5 * uu * uu * par[4];
            double zp = v0.e(2, 0) - uu * (1 - vv * par[4]) * cotth;
            // * phi at vertex with these parameters
//            double phiVert = par[3] + uu * par[4]; // MG--10/30/18:  is this sign right?  isn't is phi-Qrho?  
            double phiVert = par[3] - uu * par[4];  // MG--10/30/18:  I think the (-) sign is right, but doesn't make a difference since initial vertex guess is always (0,0,0)
//          measured track parameters moved to v0           
            BasicMatrix p0 = new BasicMatrix(5, 1);
            p0.setElement(0, 0, eps);
            p0.setElement(1, 0, zp);
            p0.setElement(2, 0, theta);
            p0.setElement(3, 0, phiVert);
            p0.setElement(4, 0, rho);

//          zeroth approximation of track direction & curvature at the vertex
            BasicMatrix q0 = new BasicMatrix(3, 1);
            q0.setElement(0, 0, theta);
            q0.setElement(1, 0, phiVert);
            q0.setElement(2, 0, rho);

            double cosf = cos(phiVert);
            double sinf = sin(phiVert);
            BasicMatrix tmpA = new BasicMatrix(5, 3);
            tmpA.setElement(0, 0, sinf);
            tmpA.setElement(0, 1, -cosf);
            tmpA.setElement(1, 0, -cotth * cosf);
            tmpA.setElement(1, 1, -cotth * sinf);
            tmpA.setElement(1, 2, 1);
            tmpA.setElement(3, 0, -par[4] * cosf);
            tmpA.setElement(3, 1, -par[4] * sinf);

            BasicMatrix tmpB = new BasicMatrix(5, 3);
            tmpB.setElement(0, 1, uu);    // deps/dphiv
            tmpB.setElement(0, 2, -uu * uu / 2);  //deps/drho
            tmpB.setElement(1, 0, uu * (1 + cotth * cotth));  //dzp/dtheta --  the sign is wrong in 1992 paper; fixed in erratum
            tmpB.setElement(1, 1, -vv * cotth);//  dzp/dphiv
            tmpB.setElement(1, 2, uu * vv * cotth);  //dzp/drho
            tmpB.setElement(3, 1, 1); //dphip/dphiv
            tmpB.setElement(3, 2, -uu); //dphip/drho
            tmpB.setElement(2, 0, 1);  //partial(theta)/dtheta
            tmpB.setElement(4, 2, 1); //partial (rho)/drho
            As.add(tmpA);
            Bs.add(tmpB);

            BasicMatrix ci = (BasicMatrix) MatrixOp.add(p0, MatrixOp.mult(-1, MatrixOp.mult(tmpA, v0)));
            ci = (BasicMatrix) MatrixOp.add(ci, MatrixOp.mult(-1, MatrixOp.mult(tmpB, q0)));
            cis.add(ci);
            pis.add(MatrixOp.add(tmpPar, MatrixOp.mult(-1, ci)));

            BasicMatrix tmpG = (BasicMatrix) MatrixOp.inverse(bt.covariance());
            Gs.add(tmpG); // Gs are the weight matrices == inverse of covariance

            // 
            if (firstTrack)
                D0 = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpA), MatrixOp.mult(tmpG, tmpA));
            else
                D0 = (BasicMatrix) MatrixOp.add(D0, MatrixOp.mult(MatrixOp.transposed(tmpA), MatrixOp.mult(tmpG, tmpA)));

            BasicMatrix tmpDi = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpA), MatrixOp.mult(tmpG, tmpB));
            BasicMatrix tmpEi = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpB), MatrixOp.mult(tmpG, tmpB));
            Ds.add(tmpDi);
            Es.add(tmpEi);
            firstTrack = false;
        }

        //ok, now compute the vertex fit.
        BasicMatrix tmpCovVtx = D0;
        BasicMatrix bigsum = new BasicMatrix(3, 1);
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix a = (BasicMatrix) As.get(i);
            BasicMatrix b = (BasicMatrix) Bs.get(i);
            BasicMatrix d = (BasicMatrix) Ds.get(i);
            BasicMatrix e = (BasicMatrix) Es.get(i);
            BasicMatrix g = (BasicMatrix) Gs.get(i);
            BasicMatrix p = (BasicMatrix) pis.get(i);
            BasicMatrix sub = (BasicMatrix) MatrixOp.mult(d, MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.transposed(d)));
            tmpCovVtx = (BasicMatrix) MatrixOp.add(tmpCovVtx, MatrixOp.mult(-1, sub));// calculate C00=D0 - (Di)(Ei-1)(DiT)

            BasicMatrix aTg = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(a), g);
            BasicMatrix beIbTg = (BasicMatrix) MatrixOp.mult(b, MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.mult(MatrixOp.transposed(b), g)));
            BasicMatrix MinusaTgbeIbTg = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(aTg, beIbTg));

            if (firstTrack)
                bigsum = (BasicMatrix) MatrixOp.mult(MatrixOp.add(aTg, MinusaTgbeIbTg), p);
            else
                bigsum = (BasicMatrix) MatrixOp.add(bigsum, MatrixOp.mult(MatrixOp.add(aTg, MinusaTgbeIbTg), p));
        }
        BasicMatrix covVtx = (BasicMatrix) MatrixOp.inverse(tmpCovVtx);
        BasicMatrix xtilde = (BasicMatrix) MatrixOp.mult(covVtx, bigsum);
        if (_debug)
            System.out.println(_constraintType + "  follow1985Paper::Vertex at : \nx= " + xtilde.e(0, 0) + " +/- " + Math.sqrt(covVtx.e(0, 0)) + "\ny= " + xtilde.e(1, 0) + " +/- " + Math.sqrt(covVtx.e(1, 1)) + "\nz= " + xtilde.e(2, 0) + " +/- " + Math.sqrt(covVtx.e(2, 2)));

        //ok, now the momentum
        //        List<Matrix> qtildes = new ArrayList<Matrix>();
        //        List<Matrix> ptildes = new ArrayList<Matrix>();
        List<Matrix> C0j = new ArrayList<Matrix>();
        List<Matrix> pfit = new ArrayList<Matrix>();
        Matrix[][] Cij = new Matrix[2][2];//max 2 tracks...just make this bigger for more
        double chisq = 0;
        for (int j = 0; j < _ntracks; j++) {
            BasicMatrix a = (BasicMatrix) As.get(j);
            BasicMatrix b = (BasicMatrix) Bs.get(j);
            BasicMatrix d = (BasicMatrix) Ds.get(j);
            BasicMatrix e = (BasicMatrix) Es.get(j);
            BasicMatrix g = (BasicMatrix) Gs.get(j);
            BasicMatrix p = (BasicMatrix) pis.get(j);
            BasicMatrix c = (BasicMatrix) cis.get(j);
            //calculate qtilde & ptilde (vertex fitted track parameters) :  equations 22b and 22d from 1985 NIM
            BasicMatrix first = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.transposed(d)));
            first = (BasicMatrix) MatrixOp.mult(first, xtilde);
            BasicMatrix second = (BasicMatrix) MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.mult(MatrixOp.transposed(b), g));
            second = (BasicMatrix) MatrixOp.mult(second, p);
            BasicMatrix qtilde = (BasicMatrix) MatrixOp.add(first, second);
            BasicMatrix ptilde = (BasicMatrix) MatrixOp.add(MatrixOp.mult(a, xtilde), MatrixOp.mult(b, qtilde));
            //caluclate unconstrained chi^2 vertex fit. 
            chisq += MatrixOp.mult(MatrixOp.transposed(MatrixOp.add(p, MatrixOp.mult(-1, ptilde))), MatrixOp.mult(g, MatrixOp.add(p, MatrixOp.mult(-1, ptilde)))).e(0, 0);
            if (_debug)
                System.out.println("\n\n" + _constraintType + "  follow1985Paper::Track #" + j);
            if (_debug)
                System.out.println("eps(meas)    = " + p.e(0, 0) + "        eps(fit)   =" + ptilde.e(0, 0));
            if (_debug)
                System.out.println("zp(meas)     = " + p.e(1, 0) + "        zp(fit)    =" + ptilde.e(1, 0));
            if (_debug)
                System.out.println("theta(meas)  = " + p.e(2, 0) + "        theta(fit) =" + ptilde.e(2, 0));
            if (_debug)
                System.out.println("phi(meas)    = " + p.e(3, 0) + "        phi(fit)   =" + ptilde.e(3, 0));
            if (_debug)
                System.out.println("rho(meas)    = " + p.e(4, 0) + "        rho(fit)   =" + ptilde.e(4, 0));

            BasicMatrix tmpC0j = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(covVtx, MatrixOp.mult(d, MatrixOp.inverse(e))));
            C0j.add(tmpC0j);
            for (int i = 0; i < _ntracks; i++) {
// MG 10/30/18:    previously, only the two lines below (commented out) were used...this doesn't seem right...seems like it's missing the delta_ij x E^
//                BasicMatrix tmpCij = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.mult(MatrixOp.transposed(d), tmpC0j)));
//               Cij[i][j] = tmpCij;
                BasicMatrix di = (BasicMatrix) Ds.get(i);
                BasicMatrix ei = (BasicMatrix) Es.get(i);
                BasicMatrix tmpCij = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(MatrixOp.inverse(ei), MatrixOp.mult(MatrixOp.transposed(di), tmpC0j)));
                if (i == j)
                    tmpCij = (BasicMatrix) MatrixOp.add(MatrixOp.inverse(ei), tmpCij);
                Cij[i][j] = tmpCij;
            }
            BasicMatrix tmppfit = new BasicMatrix(3, 1);
            tmppfit.setElement(0, 0, qtilde.e(0, 0) + c.e(2, 0));
            tmppfit.setElement(1, 0, qtilde.e(1, 0) + c.e(3, 0));
            tmppfit.setElement(2, 0, qtilde.e(2, 0) + c.e(4, 0));
            pfit.add(tmppfit);
        }

        if (_debug)
            System.out.println(_constraintType + "  follow1985Paper::chi^2 = " + chisq);

        _chiSq = chisq;
        _covVtx = covVtx;
        _vertexPosition = xtilde;
        _pFit = pfit;
        covMomList = Cij;
        covVtxMomList = C0j;

    }

    private Matrix getJacobianThetaPhiRhoToPxPyPz(double theta, double phiv, double rho) {
        BasicMatrix v0 = new BasicMatrix(3, 3);
        double B = _bField * Constants.fieldConversion;
        //mg...I hope I got all of the signs right...
        //pt = abs(B/rho) , so that should never bring in a negative sign        
        v0.setElement(0, 0, 0);
        v0.setElement(0, 1, -(Math.abs(B / rho) * Math.sin(phiv)));
        v0.setElement(0, 2, -(B * Math.cos(phiv) / Math.pow(rho, 2)));//rho^2 will take care of absolute value 
        v0.setElement(1, 0, 0);
        v0.setElement(1, 1, Math.abs(B / rho) * Math.cos(phiv));
        v0.setElement(1, 2, -(B * Math.sin(phiv) / Math.pow(rho, 2)));//rho^2 will take care of absolute value 
        v0.setElement(2, 0, -(Math.abs(B / rho) * Math.pow(1 / Math.sin(theta), 2)));
        v0.setElement(2, 1, 0);
        v0.setElement(2, 2, -(B * (1 / Math.tan(theta)) / Math.pow(rho, 2)));//rho^2 will take care of absolute value 
        return v0;
    }

}
