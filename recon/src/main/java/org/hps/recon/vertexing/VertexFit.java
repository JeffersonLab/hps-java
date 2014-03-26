
package org.hps.recon.vertexing;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import java.util.Map;

import org.hps.recon.tracking.StraightLineTrack;

/**
 *  Encapsulate the results of a vertex fit
 *
 * @author Richard Partridge
 */
public class VertexFit {

    private Hep3Vector _vtx;
    private SymmetricMatrix _cov;
    private double _chisq;
    private Map<StraightLineTrack, Hep3Vector> _dirmap;

    /**
     * Fully qualified constructor for a VertexFit
     *
     * @param vtx vertex position
     * @param cov covariance matrix of vertex
     * @param chisq chisq of vertex fit
     * @param dirmap map relating the input StraightLineTracks to the fitted track directions
     */
    public VertexFit(Hep3Vector vtx, SymmetricMatrix cov, double chisq,
            Map<StraightLineTrack, Hep3Vector> dirmap) {

        _vtx = vtx;
        _cov = cov;
        _chisq = chisq;
        _dirmap = dirmap;
    }

    /**
     * Return the vertex position.
     *
     * @return vertex position
     */
    public Hep3Vector vtx() {
        return _vtx;
    }

    /**
     * Return the covariance matrix from the vertex fit.
     *
     * @return covariance matrix
     */
    public SymmetricMatrix covariance() {
        return _cov;
    }

    /**
     * Return the chisq of the vertex fit.
     *
     * @return chisq
     */
    public double chisq() {
        return _chisq;
    }

    /**
     * Return the fitted direction for a given StraightLineTrack that was input to the fit
     *
     * @param slt input StraightLineTrack
     * @return fitted direction
     */
    public Hep3Vector TrackDirection(StraightLineTrack slt) {
        return _dirmap.get(slt);
    }
}