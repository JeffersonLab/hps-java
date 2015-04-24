package org.hps.conditions.dummy;

import java.sql.Connection;
import java.sql.SQLException;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * A dummy conditions object type.
 */
@Table(names = {"dummy"})
public final class DummyConditionsObject extends BaseConditionsObject {

    public static class DummyConditionsObjectCollection extends BaseConditionsObjectCollection<DummyConditionsObject> {

        public DummyConditionsObjectCollection() {
        }

        public DummyConditionsObjectCollection(final Connection connection, final TableMetaData tableMetaData)
                throws SQLException, DatabaseObjectException {
            super(connection, tableMetaData, -1);
        }
    }

    public DummyConditionsObject() {
    }

    public DummyConditionsObject(final Connection connection, final TableMetaData tableMetaData) {
        super(connection, tableMetaData);
    }

    @Field(names = {"dummy"})
    public Double getDummy() {
        return this.getFieldValue(Double.class, "dummy");
    }
}
