package org.lcsim.geometry.compact.converter;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtAlignmentConstant.SvtAlignmentConstantCollection;
import org.lcsim.conditions.ConditionsManager;

/**
 * Reads in SVT alignment constants from the database and converts them to the {@link MilleParameter} class expected by
 * the detector model.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class SvtAlignmentConstantsReader {
	
	private SvtAlignmentConstantsReader() {		
	}

    public static List<MilleParameter> readMilleParameters() {

        if (!ConditionsManager.isSetup()) {
            throw new RuntimeException("Conditions system is not initialized.");
        }

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();

        System.out.printf("loading alignment parameters with detector: %s; run: %d", manager.getDetector(),
                manager.getRun());

        final List<MilleParameter> milleParameters = new ArrayList<MilleParameter>();

        final SvtAlignmentConstantCollection alignmentConstants = manager.getCachedConditions(
                SvtAlignmentConstantCollection.class, "svt_alignments").getCachedData();

        for (final SvtAlignmentConstant constant : alignmentConstants) {
            final MilleParameter p = new MilleParameter(Integer.parseInt(constant.getParameter()), constant.getValue(),
                    0.0);
            milleParameters.add(p);
            System.out.println("added " + p.toString());
        }

        return milleParameters;
    }
}
