package org.hps.analysis.alignment.straighttrack.vertex;

/**
 *
 * @author ngraf
 */
public class Track {

    double[] _params = new double[6];
    double[] _cov = new double[15];

    public Track(double[] params, double[] cov) {
        System.arraycopy(params, 0, _params, 0, 6);
        System.arraycopy(cov, 0, _cov, 0, 15);
    }

    public Track(Track t) {
        System.arraycopy(t.params(), 0, _params, 0, 6);
        System.arraycopy(t.cov(), 0, _cov, 0, 15);
    }

    public double[] params() {
        return _params;
    }

    public double[] cov() {
        return _cov;
    }

    public void setParams(double[] params) {
        System.arraycopy(params, 0, _params, 0, 6);
    }

    public void setCov(double[] cov) {
        System.arraycopy(cov, 0, _cov, 0, 15);
    }

}
