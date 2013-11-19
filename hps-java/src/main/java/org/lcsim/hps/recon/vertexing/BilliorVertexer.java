package org.lcsim.hps.recon.vertexing;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.constants.Constants;

/**
 * @version $Id: BilliorVertexer.java,v 1.3 2013/03/13 19:24:20 mgraham Exp $
 * @version Vertex tracks using least-squares method laid out by billior etal used in the HPS Java package.
 */
public class BilliorVertexer {
    // the value of the magnetic field in the vicinity of the vertex
    // default is a constant field along the z axis

    private boolean _debug = false;
    private double _bField;
    private boolean _beamspotConstraint = true;
    private boolean _targetConstraint = false;
    private double[] _beamSize = {0.001, 0.01, 0.01}; //10um in y and z
    private int _ntracks;
    private List<Matrix> paramList = new ArrayList<Matrix>();
    private List<Matrix> WList = new ArrayList<Matrix>();
    private List<Matrix> DList = new ArrayList<Matrix>();
    private List<Matrix> EList = new ArrayList<Matrix>();
    private Matrix A;
    private Matrix T;
    private List<Matrix> BList = new ArrayList<Matrix>();
    private List<Matrix> CinvList = new ArrayList<Matrix>();
    private List<Matrix> CList = new ArrayList<Matrix>();
    private List<Matrix> UList = new ArrayList<Matrix>();
    private List<Matrix> dqList = new ArrayList<Matrix>();
    private double[] _v0 = {0.0, 0.0, 0.0};
//    private double[] _vertexPosition = {0., 0.0, 0.0};
    private Matrix _vertexPosition = new BasicMatrix(3, 1);
    private Matrix _covVtx = new BasicMatrix(3, 3);
    private List<Matrix> _pFit = new ArrayList<Matrix>();

    ;//theta,phi_v,rho
    private List<Matrix> covVtxMomList = new ArrayList<Matrix>();
    private Matrix[][] covMomList = new Matrix[2][2];//max 2 tracks...just make this bigger for more
    private Matrix _constrainedFit;
    private Matrix _constrainedCov;
    private double _chiSq;
     private String _constraintType="Unspecified";
    // constructor
    public BilliorVertexer() {
    }

    public BilliorVertexer(double bField) {
        _bField = bField;
        _constraintType="Unconstrained";
        _beamspotConstraint =false;
        _targetConstraint = false;
    }
    
    public BilliorVertexer(double bField,boolean bsConst, boolean constToBS) {
        _bField = bField;
        _beamspotConstraint =bsConst;
        _targetConstraint = constToBS;
        if(_beamspotConstraint&&_targetConstraint)
            System.out.println("BilliorVertexer::Warning!!!  Setting both _beamspotConstraint and _targetConstraint to true!");
        if(_beamspotConstraint)
            _constraintType="BeamspotConstrained";
        if(_targetConstraint)
            _constraintType="TargetConstrained";
    }

    public BilliorVertex fitVertex(List<BilliorTrack> tracks) {
        _ntracks = tracks.size();
        follow1985Paper(tracks);
        if (_beamspotConstraint)
            addV0fromBSConstraint();
        else if (_targetConstraint)
            constrainV0toBS();
        Map<Integer,Hep3Vector> pFitMap=new HashMap<Integer,Hep3Vector>();
        for(int i=0;i<tracks.size();i++){
            Hep3Vector pFit=new BasicHep3Vector(this.getFittedMomentum(i));
            pFitMap.put(i, pFit);            
        }
        Hep3Vector vert=new BasicHep3Vector(_vertexPosition.e(0, 0),_vertexPosition.e(1, 0),_vertexPosition.e(2, 0));
        return new BilliorVertex(vert,_covVtx,_chiSq,getInvMass(),pFitMap,_constraintType);
    }

    public BilliorVertex fitFastVertex(List<BilliorTrack> tracks) {
        _ntracks = tracks.size();
        fastVertex(tracks);
     Hep3Vector vert=new BasicHep3Vector(_vertexPosition.e(0, 0),_vertexPosition.e(1, 0),_vertexPosition.e(2, 0));
     return new BilliorVertex((Hep3Vector)_vertexPosition,_covVtx,_chiSq,getInvMass());
    }

    private void calculateCovariance() {
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix b = (BasicMatrix) BList.get(i);
            BasicMatrix cinv = (BasicMatrix) CinvList.get(i);
            BasicMatrix bt = (BasicMatrix) MatrixOp.transposed(b);
            covVtxMomList.add((MatrixOp.mult(-1, MatrixOp.mult(_covVtx, MatrixOp.mult(b, cinv)))));
            for (int j = 0; j < _ntracks; j++) {
                BasicMatrix bj = (BasicMatrix) BList.get(j);
                BasicMatrix cjinv = (BasicMatrix) CinvList.get(j);
                BasicMatrix tmp = (BasicMatrix) MatrixOp.mult(cinv, MatrixOp.mult(bt, MatrixOp.mult(_covVtx, MatrixOp.mult(bj, cjinv))));
                if (i == j)
                    tmp = (BasicMatrix) MatrixOp.add(tmp, cinv);
                covMomList[i][j] = tmp;
            }
        }
    }

    private void calculateMomenta() {

        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix params = (BasicMatrix) paramList.get(i);
            BasicMatrix b = (BasicMatrix) BList.get(i);
            BasicMatrix cinv = (BasicMatrix) CinvList.get(i);
            BasicMatrix u = (BasicMatrix) UList.get(i);
            //not sure following line is correct...mg 10/21/10
            BasicMatrix CinvU = (BasicMatrix) MatrixOp.mult(cinv, u);
            BasicMatrix CinvBTdV = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(cinv, MatrixOp.mult(MatrixOp.transposed(b), _vertexPosition)));
//            if(_debug)System.out.println(" B = "+b.toString());
//            if(_debug)System.out.println(" cinv = "+cinv.toString());
//            if(_debug)System.out.println(" u = "+u.toString());
//            if(_debug)System.out.println(" CinvU = "+CinvU.toString());
//            if(_debug)System.out.println(" CinvBTdV = "+CinvBTdV.toString());
            BasicMatrix tmpP = (BasicMatrix) MatrixOp.add(CinvBTdV, CinvU);
            tmpP.setElement(0, 0, tmpP.e(0, 0) + params.e(2, 0));
            tmpP.setElement(1, 0, tmpP.e(1, 0) + params.e(3, 0));
            tmpP.setElement(2, 0, tmpP.e(2, 0) + params.e(4, 0));
            _pFit.add(tmpP);
//            if(_debug)System.out.println("Track "+i+" orig parameters  = "+params);
//            if(_debug)System.out.println("Track "+i+" deltaP  = "+MatrixOp.add(CinvBTdV, CinvU));
//            if(_debug)System.out.println("Track " + i + " _pFit  = " + tmpP);
        }
    }

    private void calculateVertexPosition() {
        BasicMatrix tmpcov = new BasicMatrix(3, 3);
        BasicMatrix tmp = new BasicMatrix(3, 1);
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix b = (BasicMatrix) BList.get(i);
            BasicMatrix cinv = (BasicMatrix) CinvList.get(i);
            BasicMatrix u = (BasicMatrix) UList.get(i);
//            if(_debug)System.out.println("Cinv matrix " + cinv.toString());
//            if(_debug)System.out.println("B matrix " + b.toString());
//            if(_debug)System.out.println("U matrix " + u.toString());
            BasicMatrix bt = (BasicMatrix) MatrixOp.transposed(b);
            //           if(_debug)System.out.println("Adding this to tmpcov : " + MatrixOp.mult(-1, MatrixOp.mult(b, MatrixOp.mult(cinv, bt))));
            if (i == 0) {
                tmpcov = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(b, MatrixOp.mult(cinv, bt)));
                tmp = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(b, MatrixOp.mult(cinv, u)));
            } else {
                tmpcov = (BasicMatrix) MatrixOp.add(tmpcov, MatrixOp.mult(-1, MatrixOp.mult(b, MatrixOp.mult(cinv, bt))));
                tmp = (BasicMatrix) MatrixOp.add(tmp, MatrixOp.mult(-1, MatrixOp.mult(b, MatrixOp.mult(cinv, u))));
            }
//            if(_debug)System.out.println("tmpCov matrix " + tmpcov.toString());
//            if(_debug)System.out.println("tmp matrix " + tmp.toString());
        }
//
//        if(_debug)System.out.println("A matrix " + A.toString());
//        if(_debug)System.out.println("tmpCov matrix " + tmpcov.toString());
//        if(_debug)System.out.println("sum of A and tmpCov = " + MatrixOp.add(A, tmpcov).toString());
        _covVtx = MatrixOp.inverse(MatrixOp.add(A, tmpcov));
//        if(_debug)System.out.println("_covVtx matrix " + _covVtx.toString());
//        if(_debug)System.out.println("T matrix " + T.toString());
        _vertexPosition = (BasicMatrix) MatrixOp.mult(_covVtx, MatrixOp.add(T, tmp));

    }

    private void makeOtherMatrices() {
        BasicMatrix tmpA = new BasicMatrix(3, 3);
        BasicMatrix tmpT = new BasicMatrix(3, 1);

        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix tmpD = (BasicMatrix) DList.get(i);
            BasicMatrix tmpE = (BasicMatrix) EList.get(i);
            BasicMatrix dq = (BasicMatrix) dqList.get(i);
            BasicMatrix tmpW = (BasicMatrix) WList.get(i);

            if (i == 0) {
                tmpA = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpD), MatrixOp.mult(tmpW, tmpD));
                tmpT = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpD), MatrixOp.mult(tmpW, dq));
            } else {
                tmpT = (BasicMatrix) MatrixOp.add(tmpT, MatrixOp.mult(MatrixOp.transposed(tmpD), MatrixOp.mult(tmpW, dq)));
                tmpA = (BasicMatrix) MatrixOp.add(tmpA, MatrixOp.mult(MatrixOp.transposed(tmpD), MatrixOp.mult(tmpW, tmpD)));
            }
            BList.add(MatrixOp.mult(MatrixOp.transposed(tmpD), MatrixOp.mult(tmpW, tmpE)));
            BasicMatrix tmpC = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpE), MatrixOp.mult(tmpW, tmpE));
            CList.add(tmpC);
            CinvList.add(MatrixOp.inverse(tmpC));
            UList.add(MatrixOp.mult(MatrixOp.transposed(tmpE), MatrixOp.mult(tmpW, dq)));

        }
        A = tmpA;
        T = tmpT;
    }

    private void calculateChisq() {
        _chiSq = 0;
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix params = (BasicMatrix) paramList.get(i);
            BasicMatrix d = (BasicMatrix) DList.get(i);
            BasicMatrix e = (BasicMatrix) EList.get(i);
            BasicMatrix w = (BasicMatrix) WList.get(i);
            BasicMatrix pi = (BasicMatrix) _pFit.get(i);
            BasicMatrix Vtilde = (BasicMatrix) MatrixOp.mult(d, _vertexPosition);
            BasicMatrix Trtilde = (BasicMatrix) MatrixOp.mult(e, pi);
            BasicMatrix ptilde = (BasicMatrix) MatrixOp.add(Vtilde, Trtilde);
            //          if(_debug)System.out.println("Vtilde = "+Vtilde);
            //          if(_debug)System.out.println("Trtilde = "+Trtilde);
            BasicMatrix resid = (BasicMatrix) MatrixOp.add(params, MatrixOp.mult(-1, ptilde));
            BasicMatrix residT = (BasicMatrix) MatrixOp.transposed(resid);
//            if(_debug)System.out.println("ptilde = "+ptilde);
//              if(_debug)System.out.println("params = "+params);
//            if(_debug)System.out.println("resid = "+resid);
//            if(_debug)System.out.println("Covariance = "+MatrixOp.inverse(w));
//             if(_debug)System.out.println("Weight = "+w);
            _chiSq = _chiSq + (MatrixOp.mult(residT, MatrixOp.mult(w, resid))).e(0, 0);
//            if(_debug)System.out.println("_chiSq = "+_chiSq);
        }
    }

    private void fastVertex(List<BilliorTrack> tracks) {
        boolean firstTrack = true;
        BasicMatrix sumwi = new BasicMatrix(3, 3);
        BasicMatrix sumwiXi = new BasicMatrix(3, 1);
        BasicMatrix dX = new BasicMatrix(3, 1);

        for (BilliorTrack bt : tracks) {
            double[] par = bt.parameters();
//            if(_debug)System.out.println("Track parameters = (" + par[0] + ", " + par[1] + ", " + par[2] + ", " + par[3] + ", " + par[4] + ")");
            double cotth = 1. / tan(par[2]);
            double phiv = par[3];
            double cosf = cos(phiv);
            double sinf = sin(phiv);

            double xi = par[0] * sin(par[3]);
            double yi = -par[0] * cos(par[3]);
            double zi = par[1];

            dX.setElement(0, 0, xi);
            dX.setElement(1, 0, yi);
            dX.setElement(2, 0, zi);

            BasicMatrix tmpD = new BasicMatrix(2, 3);
            tmpD.setElement(0, 0, sinf);
            tmpD.setElement(0, 1, -cosf);
            tmpD.setElement(1, 0, -cotth * cosf);
            tmpD.setElement(1, 1, -cotth * sinf);
            tmpD.setElement(1, 2, 1);
            BasicMatrix trkCov = new BasicMatrix(2, 2);
            trkCov.setElement(0, 0, bt.covariance().e(0, 0));
            trkCov.setElement(0, 1, bt.covariance().e(0, 1));
            trkCov.setElement(1, 0, bt.covariance().e(1, 0));
            trkCov.setElement(1, 1, bt.covariance().e(1, 1));
            BasicMatrix tmpW = (BasicMatrix) MatrixOp.inverse(trkCov);
            BasicMatrix wi = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpD), MatrixOp.mult(tmpW, tmpD));
            if (firstTrack) {
                sumwi = wi;
                sumwiXi = (BasicMatrix) MatrixOp.mult(wi, dX);
            } else {
                sumwi = (BasicMatrix) MatrixOp.add(sumwi, wi);
                sumwiXi = (BasicMatrix) MatrixOp.add(sumwiXi, MatrixOp.mult(wi, dX));
            }
            firstTrack = false;
        }
        _covVtx = MatrixOp.inverse(sumwi);
        if (_debug)
            System.out.println("fastVertex::_covVtx matrix " + _covVtx.toString());
        _vertexPosition = (BasicMatrix) MatrixOp.mult(_covVtx, sumwiXi);
        _chiSq = 0;
        //get the chisq
        for (BilliorTrack bt : tracks) {
            double[] par = bt.parameters();
//            if(_debug)System.out.println("Track parameters = (" + par[0] + ", " + par[1] + ", " + par[2] + ", " + par[3] + ", " + par[4] + ")");
            double cotth = 1. / tan(par[2]);
            double phiv = par[3];
            double cosf = cos(phiv);
            double sinf = sin(phiv);

            double xi = par[0] * sin(par[3]);
            double yi = -par[0] * cos(par[3]);
            double zi = par[1];
            //this is xi - fitted vertex now
            dX.setElement(0, 0, xi - _vertexPosition.e(0, 0));
            dX.setElement(1, 0, yi - _vertexPosition.e(1, 0));
            dX.setElement(2, 0, zi - _vertexPosition.e(2, 0));

            BasicMatrix tmpD = new BasicMatrix(2, 3);
            tmpD.setElement(0, 0, sinf);
            tmpD.setElement(0, 1, -cosf);
            tmpD.setElement(1, 0, -cotth * cosf);
            tmpD.setElement(1, 1, -cotth * sinf);
            tmpD.setElement(1, 2, 1);
            BasicMatrix trkCov = new BasicMatrix(2, 2);
            trkCov.setElement(0, 0, bt.covariance().e(0, 0));
            trkCov.setElement(0, 1, bt.covariance().e(0, 1));
            trkCov.setElement(1, 0, bt.covariance().e(1, 0));
            trkCov.setElement(1, 1, bt.covariance().e(1, 1));
            BasicMatrix tmpW = (BasicMatrix) MatrixOp.inverse(trkCov);
            BasicMatrix wi = (BasicMatrix) MatrixOp.mult(MatrixOp.transposed(tmpD), MatrixOp.mult(tmpW, tmpD));
            _chiSq += MatrixOp.mult(MatrixOp.transposed(dX), MatrixOp.mult(wi, dX)).e(0, 0);
        }
    }

    private void makeDerivativeMatrices(List<BilliorTrack> tracks) {

        //DList.clear();
        //EList.clear();
        //paramList.clear();
        //dqList.clear();
        //WList.clear();
        BasicMatrix dq = new BasicMatrix(5, 1);
        BasicMatrix tmpW = new BasicMatrix(5, 5);
        for (BilliorTrack bt : tracks) {
            double[] par = bt.parameters();
            BasicMatrix tmpPar = new BasicMatrix(5, 1);
            tmpPar.setElement(0, 0, par[0]);
            tmpPar.setElement(1, 0, par[1]);
            tmpPar.setElement(2, 0, par[2]);
            tmpPar.setElement(3, 0, par[3]);
            tmpPar.setElement(4, 0, par[4]);
            paramList.add(tmpPar);
            double cotth = 1. / tan(par[2]);
            double uu = _v0[0] * cos(par[3]) + _v0[1] * sin(par[3]);//Q
            double vv = _v0[1] * cos(par[3]) - _v0[0] * sin(par[3]);//R
            double eps = -vv - .5 * uu * uu * par[4];
            double zp = _v0[2] - uu * (1 - vv * par[4]) * cotth;
            // * phi at vertex with these parameters
            double phiv = par[3] + uu * par[4];
            double cosf = cos(phiv);
            double sinf = sin(phiv);

            BasicMatrix tmpD = new BasicMatrix(5, 3);
            tmpD.setElement(0, 0, sinf);
            tmpD.setElement(0, 1, -cosf);
            tmpD.setElement(1, 0, -cotth * cosf);
            tmpD.setElement(1, 1, -cotth * sinf);
            tmpD.setElement(1, 2, 1);
            tmpD.setElement(3, 0, -par[4] * cosf);
            tmpD.setElement(3, 1, -par[4] * sinf);

            BasicMatrix tmpE = new BasicMatrix(5, 3);
            tmpE.setElement(0, 1, uu);
            tmpE.setElement(0, 2, -uu * uu / 2);
            tmpE.setElement(1, 0, uu * (1 + cotth * cotth));
            tmpE.setElement(1, 1, -vv * cotth);
            tmpE.setElement(1, 2, uu * vv * cotth);
            tmpE.setElement(3, 1, 1);
            tmpE.setElement(3, 2, -uu);
            tmpE.setElement(2, 0, 1);  //partial(theta)/dtheta
            tmpE.setElement(4, 2, 1); //partial (rho)/drho
            DList.add(tmpD);
            EList.add(tmpE);

            double deps = par[0] - eps;
            double dzp = par[1] - zp;
            double dphi = par[3] - phiv;

            dq.setElement(0, 0, deps);
            dq.setElement(1, 0, dzp);
            dq.setElement(3, 0, dphi);
            dqList.add(dq);
            tmpW = (BasicMatrix) MatrixOp.inverse(bt.covariance());
            WList.add(tmpW);

            if (_debug)
                System.out.println("makeDerivativeMatrices::Params = \n" + tmpPar);
            if (_debug)
                System.out.println("D = \n" + tmpD);
            if (_debug)
                System.out.println("E = \n" + tmpE);
            if (_debug)
                System.out.println("dq = \n" + dq);
            if (_debug)
                System.out.println("W = \n" + tmpW);
        }

    }

    /*  Add the constraint that V0 points back to beamspot
     *  this method is based on progressive least squares fit
     *  using the unconstrained fit result as the (k-1) fit
     *
     *  all notation is taken from:
     * W. Hulsbergen, NIM 552 (2005) 566-575
     */
    private void addV0fromBSConstraint() {
        BasicMatrix Hk = new BasicMatrix(3 * (_ntracks + 1), 3);
        BasicMatrix Ckm1 = new BasicMatrix(3 * (_ntracks + 1), 3 * (_ntracks + 1));
        BasicMatrix Xkm1 = new BasicMatrix(3 * (_ntracks + 1), 1);
        MatrixOp.setSubMatrix(Ckm1, _covVtx, 0, 0);
        MatrixOp.setSubMatrix(Xkm1, _vertexPosition, 0, 0);
        int n = 1;
        for (Matrix covVtxMom : covVtxMomList) {
            if (_debug)
                System.out.println("addV0fromBSConstraint::Track " + n + "  covVtxMom : " + covVtxMom.toString());
            MatrixOp.setSubMatrix(Ckm1, covVtxMom, 0, 3 * n);
            MatrixOp.setSubMatrix(Ckm1, MatrixOp.transposed(covVtxMom), 3 * n, 0);
            n++;
        }
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix pi = (BasicMatrix) _pFit.get(i);
            MatrixOp.setSubMatrix(Xkm1, pi, 3 * (i + 1), 0);
            if (_debug)
                System.out.println("addV0fromBSConstraint::Track " + i + "  p : " + pi.toString());
            for (int j = 0; j < _ntracks; j++)
                MatrixOp.setSubMatrix(Ckm1, covMomList[i][j], 3 * (i + 1), 3 * (j + 1));
        }

        //  now calculate the derivative matrix for the beam constraint.
        //  the beamspot is assumed to be at bvec=(0,0,0)
        //  the V0 production position is Vbvec=(0,-(ptot_y)/(ptot_x)*Vx+Vy, -(ptot_z)/(ptot_x)*Vx+Vz)
        //  where ptot=sum_i (pi)
        //  need derivites wrt to the vertex position and momentum (theta,phi_v,rho)
        double Vx = _vertexPosition.e(0, 0);
        double Vy = _vertexPosition.e(1, 0);
        double Vz = _vertexPosition.e(2, 0);
        //first, get the sum of momenta...
        double pxtot = 0;
        double pytot = 0;
        double pztot = 0;
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
        //calculate the position of the A' at X=0
        BasicMatrix rk = new BasicMatrix(3, 1);
        if (_debug)
            System.out.println("addV0fromBSConstraint::Vx = " + Vx + "; Vy = " + Vy + "; Vz = " + Vz + "; pxtot = " + pxtot + "; pytot = " + pytot + "; pztot = " + pztot);
        rk.setElement(0, 0, 0);
        rk.setElement(1, 0, 0 - (Vy - pytot / pxtot * Vx));
        rk.setElement(2, 0, 0 - (Vz - pztot / pxtot * Vx));

//  ok, can set the derivitives wrt to V
        Hk.setElement(0, 0, 0);
        Hk.setElement(0, 1, pytot / pxtot);
        Hk.setElement(0, 2, pztot / pxtot);
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
            double px = Pt * Math.cos(phiv);
            double py = Pt * Math.sin(phiv);
            double pz = Pt * 1 / Math.tan(theta);
            //derivities wrt theta
            Hk.setElement(3 * (i + 1), 0, 0);
            Hk.setElement(3 * (i + 1), 1, 0);
            Hk.setElement(3 * (i + 1), 2, -Pt / Math.pow(sin(theta), 2) * Vx);
            //derivities wrt phi
            Hk.setElement(3 * (i + 1) + 1, 0, 0);
            Hk.setElement(3 * (i + 1) + 1, 1,
                    (Pt * Pt * cos(phiv) * sin(phiv) / (pxtot * pxtot)) * Vx);
            Hk.setElement(3 * (i + 1) + 1, 2, (Pt * sin(phiv) / (pxtot * pxtot)) * Vx * pztot);
            //derivities wrt rho
            Hk.setElement(3 * (i + 1) + 2, 0, 0);
//            Hk.setElement(3 * (i + 1) + 2, 1,
//                    (pytot / pxtot - 1) * (Pt / rho) * (1 / pxtot) * Vx);
//            Hk.setElement(3 * (i + 1) + 2, 2,
//                    (pztot / pxtot - 1) * (Pt / rho) * (1 / pxtot) * Vx);
            Hk.setElement(3 * (i + 1) + 2, 1,
                    (cos(phiv) * pytot / pxtot - sin(phiv)) * (Pt / rho) * (1 / pxtot) * Vx);
            Hk.setElement(3 * (i + 1) + 2, 2,
                    (cos(phiv) * pztot / pxtot - sin(phiv)) * (Pt / rho) * (1 / pxtot) * Vx);
            //                   if(_debug)System.out.println("pxtot = "+pxtot+"; rho = "+rho+"; Pt = "+Pt);
            //                   if(_debug)System.out.println("cos(phiv)*pytot / pxtot - sin(phiv) = "+(cos(phiv)*pytot / pxtot - sin(phiv)));
            //                   if(_debug)System.out.println("Pt/(rho*pxtot) = "+(Pt / rho) * (1 / pxtot));
        }
        // the beam covariance
        BasicMatrix Vk = new BasicMatrix(3, 3);
        Vk.setElement(0, 0, _beamSize[0] * _beamSize[0]);
        Vk.setElement(1, 1, _beamSize[1] * _beamSize[1]);
        Vk.setElement(2, 2, _beamSize[2] * _beamSize[1]);

        //now do the matrix operations to get the constrained parameters
        BasicMatrix Hkt = (BasicMatrix) MatrixOp.transposed(Hk);
        if (_debug)
            System.out.println("addV0fromBSConstraint::Ckm1Hk = " + MatrixOp.mult(Ckm1, Hk));

        BasicMatrix Rk = (BasicMatrix) MatrixOp.mult(Hkt, MatrixOp.mult(Ckm1, Hk));
        if (_debug)
            System.out.println("Pre Vk:  Rk = " + Rk.toString());
        Rk = (BasicMatrix) MatrixOp.add(Rk, Vk);
        if (_debug)
            System.out.println("Post Vk:  Rk = " + Rk.toString());
        BasicMatrix Rkinv = (BasicMatrix) MatrixOp.inverse(Rk);
        BasicMatrix Kk = (BasicMatrix) MatrixOp.mult(Ckm1, MatrixOp.mult(Hk, Rkinv));

//        if(_debug)System.out.println("Ckm1 = " + Ckm1.toString());
//        if(_debug)System.out.println("Hk = " + Hk.toString());
//        if(_debug)System.out.println("Rk = " + Rk.toString());
//        if(_debug)System.out.println("Vk = " + Vk.toString());
//        if(_debug)System.out.println("rk = " + rk.toString());
//        if(_debug)System.out.println("Kk = " + Kk.toString());
        _constrainedFit = MatrixOp.mult(Kk, rk);
        _constrainedFit = MatrixOp.add(_constrainedFit, Xkm1);//Xk

        //ok, get the new covariance
        BasicMatrix RkKkt = (BasicMatrix) MatrixOp.mult(Rk, MatrixOp.transposed(Kk));
        BasicMatrix HkCkm1 = (BasicMatrix) MatrixOp.mult(Hkt, Ckm1);
        RkKkt = (BasicMatrix) MatrixOp.mult(1, RkKkt);
        HkCkm1 = (BasicMatrix) MatrixOp.mult(-2, HkCkm1);
        BasicMatrix sumMatrix = (BasicMatrix) MatrixOp.mult(Kk, MatrixOp.add(HkCkm1, RkKkt));
        _constrainedCov = (BasicMatrix) MatrixOp.add(Ckm1, sumMatrix);

        //update the regular parameter names to the constrained result
//        if(_debug)System.out.println("Without Constraint : " + _vertexPosition.toString());
//        if(_debug)System.out.println("Without Constraint:  x= "+_vertexPosition.e(0,0));
        //        if(_debug)System.out.println(_constrainedFit.toString());
//         if(_debug)System.out.println("Without Constraint : " + _covVtx.toString());
        _vertexPosition = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedFit, 0, 0, 3, 1);
        _covVtx = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedCov, 0, 0, 3, 3);
//        if(_debug)System.out.println("With Constraint : " + _vertexPosition.toString());
//        if(_debug)System.out.println("With Constraint : " + _covVtx.toString());

        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix ptmp = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedFit, 3 * (i + 1), 0, 3, 1);
            _pFit.set(i, ptmp);
        }

//        if(_debug)System.out.println("Unconstrained chi^2 = "+_chiSq);
        //ok...add to the chi^2
        if (_debug)
            System.out.println(MatrixOp.mult(MatrixOp.transposed(rk), MatrixOp.mult(Rkinv, rk)));
        _chiSq += MatrixOp.mult(MatrixOp.transposed(rk), MatrixOp.mult(Rkinv, rk)).e(0, 0);
//        if(_debug)System.out.println("Constrained chi^2 = "+_chiSq);
    }

    private void constrainV0toBS() {
        BasicMatrix Hk = new BasicMatrix(3 * (_ntracks + 1), 3);
        BasicMatrix Ckm1 = new BasicMatrix(3 * (_ntracks + 1), 3 * (_ntracks + 1));
        BasicMatrix Xkm1 = new BasicMatrix(3 * (_ntracks + 1), 1);
        MatrixOp.setSubMatrix(Ckm1, _covVtx, 0, 0);
        MatrixOp.setSubMatrix(Xkm1, _vertexPosition, 0, 0);

        int n = 1;
        for (Matrix covVtxMom : covVtxMomList) {
            if (_debug)
                System.out.println("constrainV0toBS::Track " + n + "  covVtxMom : " + covVtxMom.toString());
            MatrixOp.setSubMatrix(Ckm1, covVtxMom, 0, 3 * n);
            MatrixOp.setSubMatrix(Ckm1, MatrixOp.transposed(covVtxMom), 3 * n, 0);
            n++;
        }
        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix pi = (BasicMatrix) _pFit.get(i);
            MatrixOp.setSubMatrix(Xkm1, pi, 3 * (i + 1), 0);
            //           if(_debug)System.out.println("Track "+i+"  p : " + pi.toString());
            for (int j = 0; j < _ntracks; j++)
                MatrixOp.setSubMatrix(Ckm1, covMomList[i][j], 3 * (i + 1), 3 * (j + 1));
        }
        //  now calculate the derivative matrix for the beam constraint.
        //  the beamspot is assumed to be at bvec=(0,0,0)
        //  the V0 production position is Vbvec=(0,0,0)
        //  where ptot=sum_i (pi)
        //  need derivites wrt to the vertex position and momentum (theta,phi_v,rho)
        double Vx = _vertexPosition.e(0, 0);
        double Vy = _vertexPosition.e(1, 0);
        double Vz = _vertexPosition.e(2, 0);
        //first, get the sum of momenta...
        double pxtot = 0;
        double pytot = 0;
        double pztot = 0;
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
        //calculate the position of the A' at X=0
        BasicMatrix rk = new BasicMatrix(3, 1);
        //       if(_debug)System.out.println("Vx = " + Vx + "; Vy = " + Vy + "; Vz = " + Vz + "; pxtot = " + pxtot + "; pytot = " + pytot + "; pztot = " + pztot);
        rk.setElement(0, 0, -Vx);
        rk.setElement(1, 0, -Vy);
        rk.setElement(2, 0, -Vz);

//  ok, can set the derivitives wrt to V
        Hk.setElement(0, 0, 1);
        Hk.setElement(0, 1, 0);
        Hk.setElement(0, 2, 0);
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
            double px = Pt * Math.cos(phiv);
            double py = Pt * Math.sin(phiv);
            double pz = Pt * 1 / Math.tan(theta);
            //derivities wrt theta
            Hk.setElement(3 * (i + 1), 0, 0);
            Hk.setElement(3 * (i + 1), 1, 0);
            Hk.setElement(3 * (i + 1), 2, 0);
            //derivities wrt phi
            Hk.setElement(3 * (i + 1) + 1, 0, 0);
            Hk.setElement(3 * (i + 1) + 1, 1,
                    0);
            Hk.setElement(3 * (i + 1) + 1, 2, 0);
            //derivities wrt rho
            Hk.setElement(3 * (i + 1) + 2, 0, 0);
//            Hk.setElement(3 * (i + 1) + 2, 1,
//                    (pytot / pxtot - 1) * (Pt / rho) * (1 / pxtot) * Vx);
//            Hk.setElement(3 * (i + 1) + 2, 2,
//                    (pztot / pxtot - 1) * (Pt / rho) * (1 / pxtot) * Vx);
            Hk.setElement(3 * (i + 1) + 2, 1,
                    0);
            Hk.setElement(3 * (i + 1) + 2, 2,
                    0);
            //                   if(_debug)System.out.println("pxtot = "+pxtot+"; rho = "+rho+"; Pt = "+Pt);
            //                   if(_debug)System.out.println("cos(phiv)*pytot / pxtot - sin(phiv) = "+(cos(phiv)*pytot / pxtot - sin(phiv)));
            //                   if(_debug)System.out.println("Pt/(rho*pxtot) = "+(Pt / rho) * (1 / pxtot));
        }
        // the beam covariance
        BasicMatrix Vk = new BasicMatrix(3, 3);
        Vk.setElement(0, 0, _beamSize[0] * _beamSize[0]);
        Vk.setElement(1, 1, _beamSize[1] * _beamSize[1]);
        Vk.setElement(2, 2, _beamSize[2] * _beamSize[1]);

        //now do the matrix operations to get the constrained parameters
        BasicMatrix Hkt = (BasicMatrix) MatrixOp.transposed(Hk);
//         if(_debug)System.out.println("Ckm1Hk = " + MatrixOp.mult(Ckm1, Hk));

        BasicMatrix Rk = (BasicMatrix) MatrixOp.mult(Hkt, MatrixOp.mult(Ckm1, Hk));
//        if(_debug)System.out.println("Pre Vk:  Rk = " + Rk.toString());
        Rk = (BasicMatrix) MatrixOp.add(Rk, Vk);
        BasicMatrix Rkinv = (BasicMatrix) MatrixOp.inverse(Rk);
        BasicMatrix Kk = (BasicMatrix) MatrixOp.mult(Ckm1, MatrixOp.mult(Hk, Rkinv));

//        if(_debug)System.out.println("Ckm1 = " + Ckm1.toString());
//        if(_debug)System.out.println("Hk = " + Hk.toString());
//        if(_debug)System.out.println("Rk = " + Rk.toString());
//        if(_debug)System.out.println("Vk = " + Vk.toString());
//        if(_debug)System.out.println("rk = " + rk.toString());
//        if(_debug)System.out.println("Kk = " + Kk.toString());
        _constrainedFit = MatrixOp.mult(Kk, rk);
        _constrainedFit = MatrixOp.add(_constrainedFit, Xkm1);//Xk

        //ok, get the new covariance
        BasicMatrix RkKkt = (BasicMatrix) MatrixOp.mult(Rk, MatrixOp.transposed(Kk));
        BasicMatrix HkCkm1 = (BasicMatrix) MatrixOp.mult(Hkt, Ckm1);
        RkKkt = (BasicMatrix) MatrixOp.mult(1, RkKkt);
        HkCkm1 = (BasicMatrix) MatrixOp.mult(-2, HkCkm1);
        BasicMatrix sumMatrix = (BasicMatrix) MatrixOp.mult(Kk, MatrixOp.add(HkCkm1, RkKkt));
        _constrainedCov = (BasicMatrix) MatrixOp.add(Ckm1, sumMatrix);

        //update the regular parameter names to the constrained result
//        if(_debug)System.out.println("Without Constraint : " + _vertexPosition.toString());
//        if(_debug)System.out.println("Without Constraint:  x= "+_vertexPosition.e(0,0));
        //        if(_debug)System.out.println(_constrainedFit.toString());
//         if(_debug)System.out.println("Without Constraint : " + _covVtx.toString());
        _vertexPosition = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedFit, 0, 0, 3, 1);
        _covVtx = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedCov, 0, 0, 3, 3);
//        if(_debug)System.out.println("With Constraint : " + _vertexPosition.toString());
//        if(_debug)System.out.println("With Constraint : " + _covVtx.toString());

        for (int i = 0; i < _ntracks; i++) {
            BasicMatrix ptmp = (BasicMatrix) MatrixOp.getSubMatrix(_constrainedFit, 3 * (i + 1), 0, 3, 1);
            _pFit.set(i, ptmp);
        }

//        if(_debug)System.out.println("Unconstrained chi^2 = "+_chiSq);
        //ok...add to the chi^2
        if (_debug)
            System.out.println(MatrixOp.mult(MatrixOp.transposed(rk), MatrixOp.mult(Rkinv, rk)));
        _chiSq += MatrixOp.mult(MatrixOp.transposed(rk), MatrixOp.mult(Rkinv, rk)).e(0, 0);
//        if(_debug)System.out.println("Constrained chi^2 = "+_chiSq);
    }

    public void setV0(double[] v0) {
        _v0 = v0;
    }

    public void setBeamSize(double[] bs) {
        _beamSize[0] = bs[0];
        _beamSize[1] = bs[1];
        _beamSize[2] = bs[2];
    }

    public void doBeamSpotConstraint(boolean bsconst) {
        _beamspotConstraint = bsconst;
         _constraintType="BeamspotConstrained";
        
    }

    public void doTargetConstraint(boolean bsconst) {
        _targetConstraint = bsconst;
         _constraintType="TargetConstrained";
    }

    public double getChiSq() {
        return _chiSq;
    }
    //in theta/phi/rho for now...should fix this to return Hep3Vector

    private double[] getFittedMomentum(int index) {
        BasicMatrix pi = (BasicMatrix) _pFit.get(index);
        double[] mom = {0, 0, 0};
        double theta = pi.e(0, 0);
        double phiv = pi.e(1, 0);
        double rho = pi.e(2, 0);
        double Pt = Math.abs((1. / rho) * _bField * Constants.fieldConversion);
        mom[0] = Pt * Math.cos(phiv);
        mom[1] = Pt * Math.sin(phiv);
        mom[2] = Pt * 1 / Math.tan(theta);
        if (_debug){
            System.out.println("getFittedMomentum::  "+mom[0] + "; " + mom[1] + "; " + mom[2]);

            System.out.println("pT= "+Pt+"; phi = "+phiv+"; B = "+ _bField);
        }
      return mom;
    }

    private double getInvMass() {
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

    public String toString() {
        StringBuffer sb = new StringBuffer("Vertex at : \nx= " + _vertexPosition.e(0, 0) + " +/- " + Math.sqrt(_covVtx.e(0, 0)) + "\ny= " + _vertexPosition.e(1, 0) + " +/- " + Math.sqrt(_covVtx.e(1, 1)) + "\nz= " + _vertexPosition.e(2, 0) + " +/- " + Math.sqrt(_covVtx.e(2, 2)));
        return sb.toString();
    }

    private void follow1985Paper(List<BilliorTrack> tracks) {

        BasicMatrix v0 = new BasicMatrix(3, 1);
        v0.setElement(0, 0, _v0[0]);
        v0.setElement(1, 0, _v0[1]);
        v0.setElement(2, 0, _v0[2]);
        List<Matrix> params = new ArrayList<Matrix>();
        List<Matrix> q0s = new ArrayList<Matrix>();
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
            BasicMatrix tmpPar = new BasicMatrix(5, 1);
            tmpPar.setElement(0, 0, par[0]);
            tmpPar.setElement(1, 0, par[1]);
            tmpPar.setElement(2, 0, par[2]);
            tmpPar.setElement(3, 0, par[3]);
            tmpPar.setElement(4, 0, par[4]);
            params.add(tmpPar);
            double theta = par[2];
            double phiv = par[3];
            double rho = par[4];
            double Pt = Math.abs((1. / rho) * _bField * Constants.fieldConversion);



            double cotth = 1. / tan(par[2]);
            double uu = v0.e(0, 0) * cos(par[3]) + v0.e(1, 0) * sin(par[3]);//Q
            double vv = v0.e(1, 0) * cos(par[3]) - v0.e(0, 0) * sin(par[3]);//R
            double eps = -vv - .5 * uu * uu * par[4];
            double zp = v0.e(2, 0) - uu * (1 - vv * par[4]) * cotth;
            // * phi at vertex with these parameters
            double phiVert = par[3] + uu * par[4];
            BasicMatrix p0 = new BasicMatrix(5, 1);
            p0.setElement(0, 0, eps);
            p0.setElement(1, 0, zp);
            p0.setElement(2, 0, theta);
            p0.setElement(3, 0, phiVert);
            p0.setElement(4, 0, rho);

            BasicMatrix q0 = new BasicMatrix(3, 1);
            /*   this looks just wrong...
            q0.setElement(0, 0, Pt * Math.cos(phiv));
            q0.setElement(1, 0, Pt * Math.sin(phiv));
            q0.setElement(2, 0, Pt * 1 / Math.tan(theta));
            q0s.add(q0);
             */
            q0.setElement(0, 0, theta);
            q0.setElement(1, 0, phiVert);
            q0.setElement(2, 0, rho);
            q0s.add(q0);

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
            tmpB.setElement(0, 1, uu);
            tmpB.setElement(0, 2, -uu * uu / 2);
            tmpB.setElement(1, 0, uu * (1 + cotth * cotth));
            tmpB.setElement(1, 1, -vv * cotth);
            tmpB.setElement(1, 2, uu * vv * cotth);
            tmpB.setElement(3, 1, 1);
            tmpB.setElement(3, 2, -uu);
            tmpB.setElement(2, 0, 1);  //partial(theta)/dtheta
            tmpB.setElement(4, 2, 1); //partial (rho)/drho
            As.add(tmpA);
            Bs.add(tmpB);

            BasicMatrix ci = (BasicMatrix) MatrixOp.add(p0, MatrixOp.mult(-1, MatrixOp.mult(tmpA, v0)));
            ci = (BasicMatrix) MatrixOp.add(ci, MatrixOp.mult(-1, MatrixOp.mult(tmpB, q0)));
            cis.add(ci);
            pis.add(MatrixOp.add(tmpPar, MatrixOp.mult(-1, ci)));

            BasicMatrix tmpG = (BasicMatrix) MatrixOp.inverse(bt.covariance());
            Gs.add(tmpG);

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
            tmpCovVtx = (BasicMatrix) MatrixOp.add(tmpCovVtx, MatrixOp.mult(-1, sub));

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
            System.out.println("follow1985Paper::Vertex at : \nx= " + xtilde.e(0, 0) + " +/- " + Math.sqrt(covVtx.e(0, 0)) + "\ny= " + xtilde.e(1, 0) + " +/- " + Math.sqrt(covVtx.e(1, 1)) + "\nz= " + xtilde.e(2, 0) + " +/- " + Math.sqrt(covVtx.e(2, 2)));

        //ok, now the momentum
        List<Matrix> qtildes = new ArrayList<Matrix>();
        List<Matrix> ptildes = new ArrayList<Matrix>();
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
            BasicMatrix first = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.transposed(d)));
            first = (BasicMatrix) MatrixOp.mult(first, xtilde);
            BasicMatrix second = (BasicMatrix) MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.mult(MatrixOp.transposed(b), g));
            second = (BasicMatrix) MatrixOp.mult(second, p);
            BasicMatrix qtilde = (BasicMatrix) MatrixOp.add(first, second);
            qtildes.add(qtilde);
            BasicMatrix ptilde = (BasicMatrix) MatrixOp.add(MatrixOp.mult(a, xtilde), MatrixOp.mult(b, qtilde));
            ptildes.add(ptilde);
            chisq += MatrixOp.mult(MatrixOp.transposed(MatrixOp.add(p, MatrixOp.mult(-1, ptilde))), MatrixOp.mult(g, MatrixOp.add(p, MatrixOp.mult(-1, ptilde)))).e(0, 0);
            if (_debug)
                System.out.println("\n\nfollow1985Paper::Track #" + j);
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
                BasicMatrix ai = (BasicMatrix) As.get(i);
                BasicMatrix bi = (BasicMatrix) Bs.get(i);
                BasicMatrix di = (BasicMatrix) Ds.get(i);
                BasicMatrix ei = (BasicMatrix) Es.get(i);
                BasicMatrix gi = (BasicMatrix) Gs.get(i);
                BasicMatrix pi = (BasicMatrix) pis.get(i);
                BasicMatrix tmpCij = (BasicMatrix) MatrixOp.mult(-1, MatrixOp.mult(MatrixOp.inverse(e), MatrixOp.mult(MatrixOp.transposed(d), tmpC0j)));
                Cij[i][j] = tmpCij;
            }
            BasicMatrix tmppfit = new BasicMatrix(3, 1);
            tmppfit.setElement(0, 0, qtilde.e(0, 0) + c.e(2, 0));
            tmppfit.setElement(1, 0, qtilde.e(1, 0) + c.e(3, 0));
            tmppfit.setElement(2, 0, qtilde.e(2, 0) + c.e(4, 0));
            pfit.add(tmppfit);
        }

        if (_debug)
            System.out.println("follow1985Paper::chi^2 = " + chisq);

        _chiSq = chisq;
        _covVtx = covVtx;
        _vertexPosition = xtilde;
        _pFit = pfit;
        covMomList = Cij;
        covVtxMomList = C0j;

    }
}
