package org.hps.conditions.beam;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * This class is a conditions object for integrated beam current values.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@Table(names = {"beam_current"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class BeamCurrent extends AbstractConditionsObject {

    public static class BeamCurrentCollection extends AbstractConditionsObjectCollection<BeamCurrent> {
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
