package org.hps.analysis.fieldOff;

/**
 *
 * @author ngraf
 */
public class TwoPointLine {

    private double _slope;
    private double _intercept;

    public TwoPointLine(double x1, double y1, double x2, double y2) {
        _slope = (y2 - y1) / (x2 - x1);
        _intercept = _slope * x1 + y1;
    }

    public double slope() {
        return _slope;
    }

    public double predict(double x) {
        return _slope * x + _intercept;
    }

    public double xAxisIntercept() {
        return -_intercept / _slope;
    }
}
