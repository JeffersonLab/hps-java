package org.hps.conditions.api;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This is a simple ORM interface for mapping Java objects to a database.
 *
 * @author Jeremy McCormick, SLAC
 */
public interface DatabaseObject {

    /**
     * Delete the object from the database, using its table name from the meta data and row ID.
     *
     * @throws ConditionsObjectException if there was an error deleting the object
     * @throws SQLException if there was a SQL query error
     */
    void delete() throws DatabaseObjectException, SQLException;

    /**
     * Get the {@link TableMetaData} for this object.
     *
     * @return the {@link TableMetaData} for this object.
     */
    TableMetaData getTableMetaData();

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
     * Return <code>true</code> if the record
     *
     * @return <code>true</code> if the object is new
     */
    boolean isNew();

    /**
     * Select information from the database into this object using the row ID.
     *
     * @param id the row ID
     * @return <code>true</code> if the select operation worked
     * @throws DatabaseObjectException if there was an error selecting information into this object
     * @throws SQLException if there was a query error
     */
    boolean select(final int id) throws DatabaseObjectException, SQLException;

    /**
     * Set the database <code>Connection</code> for the object.
     *
     * @param connection the database <code>Connection</code> for the object
     */
    void setConnection(Connection connection);

    /**
     * Set the table meta data of the object.
     *
     * @param tableMetaData the table meta data of the object
     * @see TableMetaData
     */
    void setTableMetaData(TableMetaData tableMetaData);

    /**
     * Update the information in the database from this object.
     *
     * @return <code>true</code> if an update occurred
     * @throws ConditionsObjectException if there is an error performing the update from the object
     * @throws SQLException if there is a SQL query error
     */
    boolean update() throws DatabaseObjectException, SQLException;
}
