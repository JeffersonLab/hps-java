package org.hps.conditions.api;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public interface DatabaseObject {

    /**
     * Get the {@link TableMetaData} for this object.
     *
     * @return the {@link TableMetaData} for this object.
     */
    TableMetaData getTableMetaData();

    /**
     * @param tableMetaData
     */
    void setTableMetaData(TableMetaData tableMetaData);

    /**
     * Insert the data of this object into the database.
     * <p>
     * This could be a single object or a collection of objects depending on the implementation.
     *
     * @throws ConditionsObjectException
     * @throws SQLException
     */
    public void insert() throws DatabaseObjectException, SQLException;

    /**
     * Set the database <code>Connection</code> for the object.
     *
     * @param connection the database <code>Connection</code> for the object
     */
    void setConnection(Connection connection);

    /**
     * Return <code>true</code> if there are local data modifications that have not been persisted to the database.
     *
     * @return <code>true</code> if there un-persisted local data modifications
     */
    boolean isDirty();

    /**
     * Return <code>true</code> if the record
     *
     * @return
     */
    boolean isNew();

    /**
     * @return <code>true</code> if an update occurred
     * @throws ConditionsObjectException
     */
    boolean update() throws DatabaseObjectException, SQLException;

    /**
     * @return
     * @throws ConditionsObjectException
     * @throws SQLException
     */
    void delete() throws DatabaseObjectException, SQLException;

    /**
     * @param id
     * @return
     * @throws DatabaseObjectException
     * @throws SQLException
     */
    boolean select(final int id) throws DatabaseObjectException, SQLException;
}
