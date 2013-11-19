package org.lcsim.hps.recon.vertexing;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import java.util.List;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import org.lcsim.constants.Constants;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.tan;
import java.util.HashMap;
import java.util.Map;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;

/**
 @version $Id: BilliorVertex.java,v 1.6 2013/04/09 22:02:39 mgraham Exp $
 @version Vertex tracks using least-squares method laid out by billior etal used
 in the HPS Java package.
 */
public class BilliorVertex implements Vertex {
    // the value of the magnetic field in the vicinity of the vertex
    // default is a constant field along the z axis

    private boolean _debug = false;
    private int _ntracks;
    private double[] _v0 = {0.0, 0.0, 0.0};
    private Hep3Vector _vertexPosition;
    private Matrix _covVtx = new BasicMatrix(3, 3);
//    private List<Matrix> _pFit = new ArrayList<Matrix>();
//    private List<Matrix> covVtxMomList = new ArrayList<Matrix>();
    private double _chiSq;
    private double _invMass;
    private List<BilliorTrack> _tracks;
    private Map<Integer, Hep3Vector> _fittedMomentum = new HashMap<Integer, Hep3Vector>();
    private String _constraintType;
    // constructor
    public BilliorVertex() {
    }

    BilliorVertex(Hep3Vector vtxPos, Matrix covVtx, double chiSq, double invMass, Map<Integer, Hep3Vector> pFitMap,String constraintType) {
        _chiSq = chiSq;
        _covVtx = covVtx;
        _vertexPosition = vtxPos;
        _invMass = invMass;
        _fittedMomentum = pFitMap;
        _constraintType=constraintType;
    }

    BilliorVertex(Hep3Vector vtxPos, Matrix covVtx, double chiSq, double invMass) {
        _chiSq = chiSq;
        _covVtx = covVtx;
        _vertexPosition = vtxPos;
        _invMass = invMass;

    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Vertex at : \nx= " + _vertexPosition.x() + " +/- " + Math.sqrt(_covVtx.e(0, 0)) + "\ny= " + _vertexPosition.y() + " +/- " + Math.sqrt(_covVtx.e(1, 1)) + "\nz= " + _vertexPosition.z() + " +/- " + Math.sqrt(_covVtx.e(2, 2)));
        return sb.toString();
    }

    @Override
    public boolean isPrimary() {
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Hep3Vector getPosition() {
        return (Hep3Vector) _vertexPosition;
    }

    @Override
    public SymmetricMatrix getCovMatrix() {
        
        return new SymmetricMatrix(_covVtx);
    }

    @Override
    public Map<String, Double> getParameters() {
        Map<String, Double> pars = new HashMap<String, Double>();
        pars.put("invMass", _invMass);
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
        return pars;
    }

    @Override
    public ReconstructedParticle getAssociatedParticle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
