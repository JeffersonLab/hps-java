package org.hps.conditions.beam;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class is a conditions object for integrated beam current values.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"beam_current"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class BeamCurrent extends BaseConditionsObject {

    /**
     * Collection implementation for this class.
     */
    @SuppressWarnings("serial")
    public static class BeamCurrentCollection extends BaseConditionsObjectCollection<BeamCurrent> {
    }

    /**
     * Get the integrated beam current.
     * @return The integrated beam current.
     */
    @Field(names = {"beam_current"})
    public double getIntegratedBeamCurrent() {
        return getFieldValue("beam_current");
    }
}
