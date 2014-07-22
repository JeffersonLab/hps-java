package org.hps.recon.tracking.nobfield;

import hep.physics.matrix.SymmetricMatrix;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import java.util.ArrayList;
import java.util.List;
import org.lcsim.constants.Constants;
import org.lcsim.event.LCIOParameters;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import static org.lcsim.event.base.BaseTrack.D0;
import static org.lcsim.event.base.BaseTrack.OMEGA;
import static org.lcsim.event.base.BaseTrack.PHI;
import static org.lcsim.event.base.BaseTrack.TANLAMBDA;
import static org.lcsim.event.base.BaseTrack.Z0;
import org.lcsim.event.base.BaseTrackState;

/**
 *
 * @author mgraham
 */
public class StraightTrack implements Track {

    protected int _type;
    protected int[] _subdetId = new int[1];
    // References to other objects.
    protected List<Track> _tracks;
    protected List<TrackerHit> _hits;
    protected List<TrackState> _trackStates;
    protected double[] _chi2 = new double[2];
    protected double[] _parameters = new double[5];
    // Parameter ordering.
    public static final int x0 = LCIOParameters.ParameterName.d0.ordinal();
    public static final int slopeXZ = LCIOParameters.ParameterName.phi0.ordinal();
    public static final int y0 = LCIOParameters.ParameterName.omega.ordinal();
    public static final int slopeYZ = LCIOParameters.ParameterName.tanLambda.ordinal();

    /**
     * Creates a new instance of StraightTrack
     */
    public StraightTrack() {
        _tracks = new ArrayList<Track>();
        _hits = new ArrayList<TrackerHit>();
        _trackStates = new ArrayList<TrackState>();
    }

    /**
     * This gets the first TrackState as a BaseTrackState, so it can be
     * modified. It will create
     * this TrackState, if it doesn't exist already.
     *
     * @return The first TrackState.
     */
    private BaseTrackState getFirstTrackState() {
        if (_trackStates.size() == 0)
            _trackStates.add(new BaseTrackState());
        return (BaseTrackState) _trackStates.get(0);
    }
    // add following setters for subclasses.

    public void setTrackParameters(double[] params) {
        // Copy to this object's parameters array.
        System.arraycopy(params, 0, _parameters, 0, 5);

        // LCIO v2 ... setup a TrackState with full parameter list.
        getFirstTrackState().setParameters(params, 0);
    }

    /**
     * Add a hit to this track.
     *
     * @param hit The TrackerHit to add to this track.
     */
    public void addHit(TrackerHit hit) {
        _hits.add(hit);
    }

    /**
     * Add a list of hits to this track.
     *
     * @param hits The list of TrackerHits to add to this track.
     */
    public void addHits(List<TrackerHit> hits) {
        _hits.addAll(hits);
    }

    /**
     * The ids of the subdetectors hit by this track. Not yet defined.
     *
     * @return a list of integers representing the subdetector ids hit by this
     * track.
     */
    // TODO establish what this means.
    public int[] getSubdetectorHitNumbers() {
        return _subdetId;
    }

    public void setSubdetectorHitNumbers(int[] subdetId) {
        this._subdetId = subdetId;
    }

    /**
     * If this is a composite track, return a list of constituent tracks.
     *
     * @return the list of individual tracks of which this track is composed.
     */
    public List<Track> getTracks() {
        return _tracks;
    }

    /**
     * Return the list of tracker hits of which this track is composed.
     *
     * @return the list of hits on this track.
     */
    public List<TrackerHit> getTrackerHits() {
        return _hits;
    }

    /**
     * Return the type of this track. Not yet defined.
     *
     * @return an integer representation of the type of this track.
     */
    public int getType() {
        return _type;
    }

    /**
     * Get the list of associated <code>TrackState</code> objects.
     *
     * @return The list of TrackStates.
     */
    public List<TrackState> getTrackStates() {
        return this._trackStates;
    }

    @Override
    public int getCharge() {
        throw new UnsupportedOperationException("StraightTrack...no momentum measured."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] getReferencePoint() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getReferencePointX() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getReferencePointY() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getReferencePointZ() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isReferencePointPCA() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] getMomentum() {
        throw new UnsupportedOperationException("StraightTrack...no momentum measured."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getPX() {
        throw new UnsupportedOperationException("StraightTrack...no momentum measured."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getPY() {
        throw new UnsupportedOperationException("StraightTrack...no momentum measured."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getPZ() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean fitSuccess() {
        throw new UnsupportedOperationException("StraightTrack...no momentum measured."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTrackParameter(int i) {
        return _parameters[i];
    }

    @Override
    public double[] getTrackParameters() {
        return _parameters;
    }

    @Override
    public SymmetricMatrix getErrorMatrix() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getChi2() {
        return _chi2[0] + _chi2[1];
    }

    public double getChi2X() {
        return _chi2[0];
    }

    public double getChi2Y() {
        return _chi2[1];
    }

    public void setChi2(double chiX, double chiY) {
        _chi2[0] = chiX;
        _chi2[1] = chiY;
    }

    @Override
    public int getNDF() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getdEdx() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getdEdxError() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getRadiusOfInnermostHit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String toString() {
        String className = getClass().getName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot != -1)
            className = className.substring(lastDot + 1);
        StringBuffer sb = new StringBuffer(className + ": Type: " + _type + "\n");
        sb.append("x0= " + _parameters[x0] + "\n");
        sb.append("slopeXZ= " + _parameters[slopeXZ] + "\n");
        sb.append("y0: " + _parameters[y0] + "\n");
        sb.append("slopeYZ= " + _parameters[slopeYZ] + "\n");
        return sb.toString();
    }

}
