package org.hps.conditions.dummy;

import org.hps.conditions.database.AbstractConditionsObjectConverter;
import org.hps.conditions.dummy.DummyConditionsObject.DummyConditionsObjectCollection;

/**
 * Converter for dummy conditions object.
 */
public final class DummyConditionsObjectConverter extends
        AbstractConditionsObjectConverter<DummyConditionsObjectCollection> {

    /**
     * Get the object's type.
     *
     * @return the object's type
     */
    @Override
    public Class<DummyConditionsObjectCollection> getType() {
        return DummyConditionsObjectCollection.class;
    }
}
