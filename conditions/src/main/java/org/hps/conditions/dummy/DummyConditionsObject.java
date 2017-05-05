package org.hps.conditions.dummy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * A dummy conditions object type for testing purposes.
 */
@Table(names = {"dummy"})
public final class DummyConditionsObject extends BaseConditionsObject {

    /**
     * Collection implementation.
     */
    public static class DummyConditionsObjectCollection extends BaseConditionsObjectCollection<DummyConditionsObject> {

        public DummyConditionsObjectCollection() {
        }

        public DummyConditionsObjectCollection(final Connection connection, final TableMetaData tableMetaData)
                throws SQLException, DatabaseObjectException {
            super(connection, tableMetaData, -1);
        }
    }

    /**
     * Class constructor.
     */
    public DummyConditionsObject() {
    }

    /**
     * Class constructor.
     *
     * @param connection the database connection
     * @param tableMetaData the table meta data
     */
    public DummyConditionsObject(final Connection connection, final TableMetaData tableMetaData) {
        super(connection, tableMetaData);
    }

    /**
     * Dummy double value.
     * 
     * @return the dummy double value
     */
    @Field(names = {"dummy"})
    public Double getDummy() {
        return this.getFieldValue(Double.class, "dummy");
    }

    /**
     * Dummy date.
     * 
     * @return the dummy date value
     */
    @Field(names = {"dummy_dt"})
    public Date getDummyDt() {
        return this.getFieldValue(Date.class, "dummy_dt");
    }

    /**
     * Dummy timestamp.
     * 
     * @return the dummy timestamp value
     */
    @Field(names = {"dummy_ts"})
    public Date getDummyTs() {
        return this.getFieldValue(Date.class, "dummy_ts");
    }
}
