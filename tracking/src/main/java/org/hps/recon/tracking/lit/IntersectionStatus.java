package org.hps.recon.tracking.lit;

/**
 * Contains the status of an intersection calculation
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class IntersectionStatus {

    private boolean _success;
    private PhysicalTrack _trackAtIntersection;
    private double _stepLength;
    private int _iterations;

    public IntersectionStatus(boolean success, PhysicalTrack t, double s, int n) {
        _success = success;
        if (success) {
            _trackAtIntersection = new PhysicalTrack(t);
            _stepLength = s;
            _iterations = n;
        }
    }

    public boolean success() {
        return _success;
    }

    public PhysicalTrack track() {
        return new PhysicalTrack(_trackAtIntersection);
    }

    public CartesianThreeVector position() {
        return _trackAtIntersection.position();
    }

    public int iterations() {
        return _iterations;
    }
}
