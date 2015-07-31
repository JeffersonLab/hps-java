package org.hps.users.meeg;

import java.util.Date;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtBiasConstant.SvtBiasConstantCollection;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class BiasTimeDemo extends Driver {

    SvtBiasConstantCollection svtBiasConstants = null;

    @Override
    protected void detectorChanged(Detector detector) {
        svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
        System.out.println("there are " + svtBiasConstants.size() + " constants to search");
        for (SvtBiasConstant constant : svtBiasConstants) {
            System.out.format("start %d (%s), end %d (%s), value %f\n", new Date(constant.getStart()), constant.getStart().toString(), new Date(constant.getEnd()), constant.getEnd().toString(), constant.getValue());
        }
    }
}
