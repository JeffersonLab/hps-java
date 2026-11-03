package org.hps.recon.vertexing;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import hep.physics.matrix.SymmetricMatrix;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackParams;

/**
 * Common base class for {@link NTrackVertexer} and {@link CascadeVertexer}: shared magnetic
 * field, electron-mass constant, and perigee-parameter extraction, both of which wrap
 * {@link TrackConstraintVertexFitter} but return different result shapes (a plain N-track
 * {@link BilliorVertex} vs. a two-vertex cascade {@code ReconstructedParticle}), so no common
 * {@code fit(...)} method is imposed here.
 */
public abstract class Vertexer {

    protected static final double ELECTRON_MASS = 0.000511;

    protected final double bField;

    protected Vertexer(double bField) {
        this.bField = bField;
    }

    /**
     * Extract perigee TrackParams (d0, phi0, omega, z0, tanLambda) directly from an
     * AtPerigee TrackState, with no reparametrization.
     */
    protected static TrackParams trackParamsFromTrack(TrackState ts) {
        double[] par = ts.getParameters();
        SymmetricMatrix sm = new SymmetricMatrix(5, ts.getCovMatrix(), true);
        RealMatrix cov = MatrixUtils.createRealMatrix(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setEntry(i, j, sm.e(i, j));
            }
        }
        return new TrackParams(par[0], par[1], par[2], par[3], par[4], cov);
    }

    /**
     * Extract perigee TrackParams (d0, phi0, omega, z0, tanLambda) from a Track's
     * AtPerigee TrackState.
     */
    protected static TrackParams trackParamsFromTrack(Track track) {
        return trackParamsFromTrack(TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0));
    }
}
