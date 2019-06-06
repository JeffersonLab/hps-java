package org.hps.recon.tracking.lit;

import static java.lang.Math.sqrt;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 * A simple class to provide two-body decays for tests of vertexing code. TODO
 * add lifetime
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class TwoBodyDecayProvider {

    private double _mass;
    private double _energy;
    private double _emass = 0.000511;
    Momentum4Vector _vec;

    public TwoBodyDecayProvider(double mass, double energy) {
        _mass = mass;
        _energy = energy;
        double p = sqrt(_energy * _energy - _mass * _mass);
        _vec = new Momentum4Vector(0, 0, p, _energy);
//        System.out.println(_vec);
    }

    public Lorentz4Vector[] decayIt() {
        return _vec.twobodyDecay(_emass, _emass);
    }

    public Momentum4Vector vec() {
        return _vec;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("TwoBodyDecayProvider with mass " + _mass + " GeV and energy " + _energy + " GeV");
        return sb.toString();
    }

}
