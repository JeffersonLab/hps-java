package org.hps.recon.vertexing;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import hep.physics.matrix.Matrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.math.chisq.ChisqProb;

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

    private SymmetricMatrix _covVtx = null;
    private Map<Integer, Hep3Vector> _fittedMomentum = new HashMap<Integer, Hep3Vector>();
    private ReconstructedParticle _particle = null;
    private String _constraintType;

    private boolean _isPrimary = true;
    private double _chiSq;
    private double _invMass;
    private double _probability = -1.0;

    // L1L1 = 2, L1L2 = 3, L2L2 = 4
    private double layerCode = -1;
    
    private List<Matrix> _covTrkMomList = null;
    private double _invMassError;
    private boolean storeCovTrkMomList = true;

    private Hep3Vector _v0Momentum;
    private Hep3Vector _v0MomentumErr;

    private double[] _v0TargetProjectionXY;
    private double[] _v0TargetProjectionXYErr;
    
    private List<double[]> _fitTrkParsList=null;//fitted track parameters (theta,phiv,rho)   
    private List<Matrix> _fitTrkCovList=null;  //list of trk covariances (theta,phiv,rho)
    /**
     * Dflt Ctor
     */
    public BilliorVertex() {
    }

    BilliorVertex(Hep3Vector vtxPos, SymmetricMatrix covVtx, double chiSq, double invMass, Map<Integer, Hep3Vector> pFitMap, String constraintType) {
        _chiSq = chiSq;
        _covVtx = covVtx;
        _vertexPosition = vtxPos;
        _invMass = invMass;
        _fittedMomentum = pFitMap;
        _constraintType = constraintType;
        
    }

    BilliorVertex(Hep3Vector vtxPos, SymmetricMatrix covVtx, double chiSq, double invMass) {
        _chiSq = chiSq;
        _covVtx = covVtx;
        _vertexPosition = vtxPos;
        _invMass = invMass;
    }

    public BilliorVertex(Vertex lcioVtx) {
        _chiSq = lcioVtx.getChi2();
        _vertexPosition = lcioVtx.getPosition();
        _covVtx = lcioVtx.getCovMatrix();
        _constraintType = lcioVtx.getAlgorithmType();
        _particle = lcioVtx.getAssociatedParticle();
        _probability = lcioVtx.getProbability();
        Map<String, Double> paramMap = lcioVtx.getParameters();
        if (paramMap.containsKey("layerCode")) 
            layerCode = paramMap.get("layerCode");
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
        
        if (paramMap.containsKey("c12-0")) {
            double[] temp = new double[6];
            temp[0] = paramMap.get("c12-0");
            temp[1] = paramMap.get("c12-1");
            temp[2] = paramMap.get("c12-2");
            temp[3] = paramMap.get("c12-3");
            temp[4] = paramMap.get("c12-4");
            temp[5] = paramMap.get("c12-5");
            SymmetricMatrix cov12 = new SymmetricMatrix(3, temp, true);
            covList.add(cov12);
        }
        
        if (covList.size() > 0)
            _covTrkMomList = covList;

        if (paramMap.containsKey("V0Px") && paramMap.containsKey("V0Py") && paramMap.containsKey("V0Pz")) {
            _v0Momentum = new BasicHep3Vector(paramMap.get("V0Px"),paramMap.get("V0Py"),paramMap.get("V0Pz"));
        }
        
        if (paramMap.containsKey("V0PxErr") && paramMap.containsKey("V0PyErr") && paramMap.containsKey("V0PzErr")) {
            _v0MomentumErr =  new BasicHep3Vector(paramMap.get("V0PxErr"),paramMap.get("V0PyErr"),paramMap.get("V0PzErr"));
        }
        
        if (paramMap.containsKey("V0TargProjX") && paramMap.containsKey("V0TargProjY")) {
            _v0TargetProjectionXY = new double[]{paramMap.get("V0TargProjX"),paramMap.get("V0TargProjY")};
        }

        if (paramMap.containsKey("V0TargProjXErr") && paramMap.containsKey("V0TargProjYErr")) {
            _v0TargetProjectionXYErr = new double[]{paramMap.get("V0TargProjXErr"),paramMap.get("V0TargProjYErr")};
        }
    }
    
    public void setProbability(int dof) {
        _probability = ChisqProb.gammq(dof, _chiSq);
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
    
    public void setFittedTrackParameters(List<double[]> pars){
        _fitTrkParsList=pars;
    }
    
    public void setFittedTrackCovariance(List<Matrix> covs){
        _fitTrkCovList=covs;
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
        return  _covVtx;
    }

    // TODO: These should be pulled out and accessed by their own 
    //       getter methods.  
    @Override
    public Map<String, Double> getParameters() {
        Map<String, Double> pars = new HashMap<String, Double>();
        pars.put("invMass", _invMass);
        pars.put("invMassError", _invMassError);
        if (layerCode != -1)
            pars.put("layerCode", layerCode);
        
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
        if (_vertexPositionError !=null){
            pars.put("vXErr", _vertexPositionError.x());
            pars.put("vYErr", _vertexPositionError.y());
            pars.put("vZErr", _vertexPositionError.z());
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
        
        if(_v0Momentum != null){
            Hep3Vector p = _v0Momentum;
            pars.put("V0P", p.magnitude());
            pars.put("V0Px", p.x());
            pars.put("V0Py", p.y());
            pars.put("V0Pz", p.z());
        }
        
        if(_v0MomentumErr != null){
            Hep3Vector pErr = _v0MomentumErr;
            pars.put("V0PErr", pErr.magnitude());
            pars.put("V0PxErr", pErr.x());
            pars.put("V0PyErr", pErr.y());
            pars.put("V0PzErr", pErr.z());
        }
         
        if(_v0TargetProjectionXY != null){
            double[] proj = _v0TargetProjectionXY;
            pars.put("V0TargProjX", proj[0]);
            pars.put("V0TargProjY", proj[1]);
        }
        
        if(_v0TargetProjectionXYErr != null){
            double[] projErr = _v0TargetProjectionXYErr;
            pars.put("V0TargProjXErr", projErr[0]);
            pars.put("V0TargProjYErr", projErr[1]);
        }
        
        return pars;
    }


    
    public void setLayerCode(String s) {
        if (s == "L1L1")
            layerCode = 2;
        else if (s == "L1L2")
            layerCode = 3;
        else if (s == "L2L2")
            layerCode = 4;        
    }
    
    public void setLayerCode(int s) {
        if (s >= 2 && s <= 4)
            layerCode = s;
    }
    
    public int getLayerCode() {
        return (int) layerCode;
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
    
    public double[] getFittedTrackParameters(int index){
        return _fitTrkParsList.get(index);
    }
    public Matrix getFittedTrackCovariance(int index){
        return _fitTrkCovList.get(index);
    }

}
