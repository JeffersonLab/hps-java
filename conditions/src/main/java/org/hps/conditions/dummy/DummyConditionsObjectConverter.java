package org.hps.conditions.dummy;

import org.hps.conditions.api.AbstractConditionsObjectConverter;
import org.hps.conditions.dummy.DummyConditionsObject.DummyConditionsObjectCollection;

public final class DummyConditionsObjectConverter extends
        AbstractConditionsObjectConverter<DummyConditionsObjectCollection> {
    @Override
    public Class<DummyConditionsObjectCollection> getType() {
        return DummyConditionsObjectCollection.class;
    }
}
