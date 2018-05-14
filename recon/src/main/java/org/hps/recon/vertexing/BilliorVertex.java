package org.hps.recon.vertexing;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;

/**
 *
 * @author Mathew Thomas Graham <mgraham@slac.stanford.edu>
 * @version $Id:$
 *
 */
public class BilliorVertex implements Vertex {
    // the value of the magnetic field in the vicinity of the vertex
    // default is a constant field along the z axis

    private Hep3Vector _vertexPosition;
    private Hep3Vector _vertexPositionError;

    private Matrix _covVtx = new BasicMatrix(3, 3);
    private Map<Integer, Hep3Vector> _fittedMomentum = new HashMap<Integer, Hep3Vector>();
    private ReconstructedParticle _particle = null;
    private String _constraintType;

    private boolean _isPrimary = true;
    private double _chiSq;
    private double _invMass;
    private double _probability;

    private List<Matrix> _covTrkMomList = null;
    private double _invMassError;
    private boolean storeCovTrkMomList = false;

    private Hep3Vector _v0Momentum;
    private Hep3Vector _v0MomentumErr;

    private double[] _v0TargetProjectionXY;
    private double[] _v0TargetProjectionXYErr;

    /**
     * Dflt Ctor
     */
    public BilliorVertex() {
    }

    BilliorVertex(Hep3Vector vtxPos, Matrix covVtx, double chiSq, double invMass, Map<Integer, Hep3Vector> pFitMap, String constraintType) {
        _chiSq = chiSq;
        _covVtx = covVtx;
        _vertexPosition = vtxPos;
        _invMass = invMass;
        _fittedMomentum = pFitMap;
        _constraintType = constraintType;
    }

    BilliorVertex(Hep3Vector vtxPos, Matrix covVtx, double chiSq, double invMass) {
        _chiSq = chiSq;
        _covVtx = covVtx;
        _vertexPosition = vtxPos;
        _invMass = invMass;

    }

    BilliorVertex(Vertex lcioVtx) {
        _chiSq = lcioVtx.getChi2();
        _vertexPosition = lcioVtx.getPosition();
        _covVtx = lcioVtx.getCovMatrix();
        _constraintType = lcioVtx.getAlgorithmType();
        _particle = lcioVtx.getAssociatedParticle();
        _probability = lcioVtx.getProbability();
        Map<String, Double> paramMap = lcioVtx.getParameters();
        if (paramMap.containsKey("p1X")) {
            Hep3Vector v1 = new BasicHep3Vector(paramMap.get("p1X"), paramMap.get("p1Y"), paramMap.get("p1Z"));
            _fittedMomentum.put(0, v1);
        }
        if (paramMap.containsKey("p2X")) {
            Hep3Vector v2 = new BasicHep3Vector(paramMap.get("p2X"), paramMap.get("p2Y"), paramMap.get("p2Z"));
            _fittedMomentum.put(1, v2);
        }
        if (paramMap.containsKey("invMass"))
            _invMass = paramMap.get("invMass");
        if (paramMap.containsKey("invMassError"))
            _invMassError = paramMap.get("invMassError");

        List<Matrix> covList = new ArrayList<Matrix>();
        if (paramMap.containsKey("c1-0")) {
            double[] temp = new double[6];
            temp[0] = paramMap.get("c1-0");
            temp[1] = paramMap.get("c1-1");
            temp[2] = paramMap.get("c1-2");
            temp[3] = paramMap.get("c1-3");
            temp[4] = paramMap.get("c1-4");
            temp[5] = paramMap.get("c1-5");
            SymmetricMatrix cov1 = new SymmetricMatrix(3, temp, true);
            covList.add(cov1);
        }
        if (paramMap.containsKey("c2-0")) {
            double[] temp = new double[6];
            temp[0] = paramMap.get("c2-0");
            temp[1] = paramMap.get("c2-1");
            temp[2] = paramMap.get("c2-2");
            temp[3] = paramMap.get("c2-3");
            temp[4] = paramMap.get("c2-4");
            temp[5] = paramMap.get("c2-5");
            SymmetricMatrix cov2 = new SymmetricMatrix(3, temp, true);
            covList.add(cov2);
        }
        if (covList.size() > 0)
            _covTrkMomList = covList;

    }

    public void setStoreCovTrkMomList(boolean input) {
        storeCovTrkMomList = input;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Vertex at : \nx= " + _vertexPosition.x() + " +/- " + Math.sqrt(_covVtx.e(0, 0)) + "\ny= " + _vertexPosition.y() + " +/- " + Math.sqrt(_covVtx.e(1, 1)) + "\nz= " + _vertexPosition.z() + " +/- " + Math.sqrt(_covVtx.e(2, 2)));
        return sb.toString();
    }

    /**
     * Set the ReconstructedParticle associated with this Vertex
     *
     * @param particle : The ReconstructedParticle Associated with this Vertex
     */
    public void setAssociatedParticle(ReconstructedParticle particle) {
        this._particle = particle;
    }

    public void setVertexTrackParameters(Hep3Vector p1, Hep3Vector p2, double mass) {
        _invMass = mass;
        _fittedMomentum.put(0, p1);
        _fittedMomentum.put(1, p2);
    }

    public void setPositionError(Hep3Vector err) {
        _vertexPositionError = err;
    }

    public void setPosition(Hep3Vector position) {
        _vertexPosition = position;
    }

    public void setTrackMomentumCovariances(List<Matrix> pErrs) {
        _covTrkMomList = pErrs;
    }

    public void setMassError(double invMassErr) {
        _invMassError = invMassErr;

    }

    public void setV0Momentum(Hep3Vector mom, Hep3Vector momErr) {
        _v0Momentum = mom;
        _v0MomentumErr = momErr;
    }

    public void setV0TargetXY(double[] xy, double[] xyerr) {
        _v0TargetProjectionXY = xy;
        _v0TargetProjectionXYErr = xyerr;
    }

    @Override
    public boolean isPrimary() {
        return _isPrimary;
    }

    @Override
    public String getAlgorithmType() {
        return (_constraintType);
    }

    @Override
    public double getChi2() {
        return _chiSq;
    }

    @Override
    public double getProbability() {
        return _probability;
    }

    @Override
    public Hep3Vector getPosition() {
        return (Hep3Vector) _vertexPosition;
    }

    public Hep3Vector getPositionError() {
        return (Hep3Vector) _vertexPositionError;
    }

    @Override
    public SymmetricMatrix getCovMatrix() {
        return new SymmetricMatrix(_covVtx);
    }

    // TODO: These should be pulled out and accessed by their own 
    //       getter methods.  
    @Override
    public Map<String, Double> getParameters() {
        Map<String, Double> pars = new HashMap<String, Double>();
        pars.put("invMass", _invMass);
        pars.put("invMassError", _invMassError);
        if (!_fittedMomentum.isEmpty()) {
            Hep3Vector p1Fit = _fittedMomentum.get(0);
            Hep3Vector p2Fit = _fittedMomentum.get(1);
            pars.put("p1X", p1Fit.x());
            pars.put("p1Y", p1Fit.y());
            pars.put("p1Z", p1Fit.z());
            pars.put("p2X", p2Fit.x());
            pars.put("p2Y", p2Fit.y());
            pars.put("p2Z", p2Fit.z());
        }

        if (_covTrkMomList != null && storeCovTrkMomList == true)
            if (_covTrkMomList.size() >= 2) {
                SymmetricMatrix covMat = new SymmetricMatrix(_covTrkMomList.get(0));
                double[] cov = covMat.asPackedArray(true);
                pars.put("c1-0", cov[0]);
                pars.put("c1-1", cov[1]);
                pars.put("c1-2", cov[2]);
                pars.put("c1-3", cov[3]);
                pars.put("c1-4", cov[4]);
                pars.put("c1-5", cov[5]);
                covMat = new SymmetricMatrix(_covTrkMomList.get(1));
                cov = covMat.asPackedArray(true);
                pars.put("c2-0", cov[0]);
                pars.put("c2-1", cov[1]);
                pars.put("c2-2", cov[2]);
                pars.put("c2-3", cov[3]);
                pars.put("c2-4", cov[4]);
                pars.put("c2-5", cov[5]);
                /* mg 3/5/2018:  added the track1-track2 momentum covariance...added as the third maxtrix
                                 what if we ever vertex >2 tracks???  Need to have a better way to store these
                 */
                if (_covTrkMomList.size() >= 3) {
                    covMat = new SymmetricMatrix(_covTrkMomList.get(2));
                    cov = covMat.asPackedArray(true);
                    pars.put("c12-0", cov[0]);
                    pars.put("c12-1", cov[1]);
                    pars.put("c12-2", cov[2]);
                    pars.put("c12-3", cov[3]);
                    pars.put("c12-4", cov[4]);
                    pars.put("c12-5", cov[5]);
                }
            }
        return pars;
    }

    public double getInvMass() {
        return _invMass;
    }

    public double getInvMassError() {
        return _invMassError;
    }

    public Hep3Vector getFittedMomentum(int index) {
        return _fittedMomentum.get(index);
    }

    /* 
    *  Return the  track momentum  list for all tracks 
     */
    public Map<Integer, Hep3Vector> getFittedMomentum() {
        return _fittedMomentum;
    }

    /* 
    *  Return the track momentum error for track i
    *  note:  only the diagional terms of covariance
     */
    public Hep3Vector getFittedMomentumError(int index) {
        return new BasicHep3Vector(Math.sqrt(_covTrkMomList.get(index).e(0, 0)), Math.sqrt(_covTrkMomList.get(index).e(1, 1)), Math.sqrt(_covTrkMomList.get(index).e(2, 2)));
    }

    /* 
    *  Return the entire track momentum covariance list for all tracks  
     */
    public List<Matrix> getFittedMomentumCovariance() {
        return _covTrkMomList;
    }

    /* 
    *  Return the recon particle associated with this vertex 
     */
    @Override
    public ReconstructedParticle getAssociatedParticle() {
        return _particle;
    }

    public Hep3Vector getV0Momentum() {
        return _v0Momentum;
    }

    public Hep3Vector getV0MomentumError() {
        return _v0MomentumErr;
    }

    public double[] getV0TargetXY() {
        return _v0TargetProjectionXY;
    }

    public double[] getV0TargetXYError() {
        return _v0TargetProjectionXYErr;
    }

}
