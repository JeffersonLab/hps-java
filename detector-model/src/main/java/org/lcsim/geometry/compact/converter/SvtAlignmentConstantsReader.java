package org.lcsim.geometry.compact.converter;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtAlignmentConstant.SvtAlignmentConstantCollection;

/**
 * Reads in SVT alignment constants from the database and converts them to the {@link MilleParameter} class expected by
 * the detector model.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class SvtAlignmentConstantsReader {
	
	private SvtAlignmentConstantsReader() {
	}

	/**
	 * Read SVT alignment constants from the conditions database table <i>svt_alignments</i> and create a list of 
	 * <code>MilleParameter</code> objects from it.
	 * 
	 * @return the Millepede parameter list
	 */
    static List<MilleParameter> readMilleParameters() {

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();

        final List<MilleParameter> milleParameters = new ArrayList<MilleParameter>();

        final SvtAlignmentConstantCollection alignmentConstants = manager.getCachedConditions(
                SvtAlignmentConstantCollection.class, "svt_alignments").getCachedData();

        for (final SvtAlignmentConstant constant : alignmentConstants) {
            final MilleParameter p = new MilleParameter(constant.getParameter(), constant.getValue(), 0.0);
            milleParameters.add(p);
        }

        return milleParameters;
    }
}
