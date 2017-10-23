package org.hps.recon.vertexing;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;

/**
 * 
 *  @author Mathew Thomas Graham <mgraham@slac.stanford.edu>
 *  @version $Id:$
 *
 */
public class BilliorVertex implements Vertex {
    // the value of the magnetic field in the vicinity of the vertex
    // default is a constant field along the z axis

    private Hep3Vector _vertexPosition;
    private Matrix _covVtx = new BasicMatrix(3, 3);
//    private List<Matrix> _pFit = new ArrayList<Matrix>();
//    private List<Matrix> covVtxMomList = new ArrayList<Matrix>();
    private List<BilliorTrack> _tracks;
    private Map<Integer, Hep3Vector> _fittedMomentum = new HashMap<Integer, Hep3Vector>();
    private ReconstructedParticle _particle = null;
    private String _constraintType;
    
    private boolean _debug = false;
    private boolean _isPrimary = true; 
    
    private int _ntracks;

    private double[] _v0 = {0.0, 0.0, 0.0};
    private double _chiSq;
    private double _invMass;
    private double _probability; 
    
    /**
     * Dflt Ctor
     */
    public BilliorVertex() {}

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
    
    /**
     * Set the ReconstructedParticle associated with this Vertex
     * 
     * @param particle : The ReconstructedParticle Associated with this Vertex
     */
    public void setAssociatedParticle(ReconstructedParticle particle){
        this._particle = particle;
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
    
    public void setVertexTrackParameters(Hep3Vector p1, Hep3Vector p2, double mass)
    {
        _invMass = mass;
        _fittedMomentum.put(0, p1);
        _fittedMomentum.put(1,p2);
    }

    @Override
    public ReconstructedParticle getAssociatedParticle() {
        return _particle; 
    }
}
