package org.hps.recon.tracking.gbl;

import java.util.List;
import java.util.ArrayList;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.lcsim.geometry.compact.converter.MilleParameter;


public class GlobalDers {
    
    private final int _layer;
    private final Hep3Vector _t; // track direction
    private final Hep3Vector _p; // track prediction
    private final Hep3Vector _n; // normal to plane
    private final Matrix _dm_dg; // Global derivaties of the local measurements
    private final Matrix _dr_dm; // Derivatives of residuals w.r.t. measurement
    private final Matrix _dr_dg; // Derivatives of residuals w.r.t. global parameters
    
    public GlobalDers(int layer, double umeas, double vmeas, double wmeas, Hep3Vector tDir, Hep3Vector tPred, Hep3Vector normal) {
        _layer = layer;
        _t = tDir;
        _p = tPred;
        _n = normal;
        // Derivatives of residuals w.r.t. perturbed measurement
        _dr_dm = getResDers();
        // Derivatives of perturbed measurement w.r.t. global parameters
        _dm_dg = getMeasDers();
        // Calculate, by chain rule, derivatives of residuals w.r.t. global parameters
        _dr_dg = _dr_dm.times(_dm_dg);

    }

    /**
     * Derivative of mt, the perturbed measured coordinate vector m w.r.t. to global parameters:
     * u,v,w,alpha,beta,gamma
     */
    private Matrix getMeasDers() {

        // Derivative of the local measurement for a translation in u
        double dmu_du = 1.;
        double dmv_du = 0.;
        double dmw_du = 0.;
        // Derivative of the local measurement for a translation in v
        double dmu_dv = 0.;
        double dmv_dv = 1.;
        double dmw_dv = 0.;
        // Derivative of the local measurement for a translation in w
        double dmu_dw = 0.;
        double dmv_dw = 0.;
        double dmw_dw = 1.;
        // Derivative of the local measurement for a rotation around u-axis (alpha)
        double dmu_dalpha = 0.;
        double dmv_dalpha = _p.z(); // self.wmeas
        double dmw_dalpha = -1.0 * _p.y(); // -1.0 * self.vmeas
        // Derivative of the local measurement for a rotation around v-axis (beta)
        double dmu_dbeta = -1.0 * _p.z(); // -1.0 * self.wmeas
        double dmv_dbeta = 0.;
        double dmw_dbeta = _p.x(); // self.umeas
        // Derivative of the local measurement for a rotation around w-axis (gamma)
        double dmu_dgamma = _p.y(); // self.vmeas
        double dmv_dgamma = -1.0 * _p.x(); // -1.0 * self.umeas 
        double dmw_dgamma = 0.;
        // put into matrix
        Matrix dm_dg = new Matrix(3, 6);
        dm_dg.set(0, 0, dmu_du);
        dm_dg.set(0, 1, dmu_dv);
        dm_dg.set(0, 2, dmu_dw);
        dm_dg.set(0, 3, dmu_dalpha);
        dm_dg.set(0, 4, dmu_dbeta);
        dm_dg.set(0, 5, dmu_dgamma);
        dm_dg.set(1, 0, dmv_du);
        dm_dg.set(1, 1, dmv_dv);
        dm_dg.set(1, 2, dmv_dw);
        dm_dg.set(1, 3, dmv_dalpha);
        dm_dg.set(1, 4, dmv_dbeta);
        dm_dg.set(1, 5, dmv_dgamma);
        dm_dg.set(2, 0, dmw_du);
        dm_dg.set(2, 1, dmw_dv);
        dm_dg.set(2, 2, dmw_dw);
        dm_dg.set(2, 3, dmw_dalpha);
        dm_dg.set(2, 4, dmw_dbeta);
        dm_dg.set(2, 5, dmw_dgamma);

        return dm_dg;
    }

    /**
     * Derivatives of the local perturbed residual w.r.t. the measurements m (u,v,w)'
     */
    private Matrix getResDers() {
        double tdotn = VecOp.dot(_t, _n);
        Matrix dr_dm = Matrix.identity(3, 3);

        double delta, val;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                delta = i == j ? 1. : 0.;
                val = delta - _t.v()[i] * _n.v()[j] / tdotn;
                dr_dm.set(i, j, val);
            }
        }
        return dr_dm;
    }

    /**
     * Turn derivative matrix into @Milleparameter
     *
     * @param isTop - top or bottom track
     * @return list of @Milleparameters
     */
    public List<MilleParameter> getDers(boolean isTop) {
        int transRot;
        int direction;
        int label;
        double value;
        List<MilleParameter> milleParameters = new ArrayList<MilleParameter>();
        int topBot = isTop == true ? 1 : 2;
        for (int ip = 1; ip < 7; ++ip) {
            if (ip > 3) {
                transRot = 2;
                direction = ((ip - 1) % 3) + 1;
            } else {
                transRot = 1;
                direction = ip;
            }
            label = topBot * MilleParameter.half_offset + transRot * MilleParameter.type_offset + direction * MilleParameter.dimension_offset + _layer;
            value = _dr_dg.get(0, ip - 1);
            MilleParameter milleParameter = new MilleParameter(label, value, 0.0);
            milleParameters.add(milleParameter);
        }
        return milleParameters;
    }

}

