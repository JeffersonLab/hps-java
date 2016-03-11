package org.hps.conditions.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import org.hps.conditions.database.Field;

/**
 * This is a basic ORM class for performing CRUD (create, read, update, delete) operations on objects in the conditions
 * system. Each object is mapped to a single row in a database table.
 *
 * @author Jeremy McCormick, SLAC
 */
public abstract class BaseConditionsObject implements ConditionsObject {

    /**
     * Field name for collection ID.
     */
    static final String COLLECTION_ID_FIELD = "collection_id";

    /**
     * Date formatting for insert statement.
     */
    static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    /**
     * Value that indicates collection ID is not set (no collection assigned).
     */
    static final int UNSET_COLLECTION_ID = -1;

    /**
     * Value that indicates row ID is not assigned (new record).
     */
    static final int UNSET_ROW_ID = -1;

    /**
     * Perform the default to string operation on a conditions object.
     *
     * @param object the conditions object
     * @return the object converted to a string
     */
    protected static String defaultToString(final BaseConditionsObject object) {
        final StringBuffer sb = new StringBuffer();
        sb.append(object.getClass().getSimpleName() + " { ");
        sb.append("id: " + object.getRowId() + ", ");
        for (final String field : object.getFieldValues().getFieldNames()) {
            sb.append(field + ": " + object.getFieldValue(Object.class, field) + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(" }");
        return sb.toString();
    }

    /**
     * The JDBC connection used for database operations.
     */
    private Connection connection;

    /**
     * The field values which is a map of key-value pairs corresponding to column values in the database.
     */
    private FieldValues fieldValues;

    /**
     * The row ID of the object in its table. This will be -1 for new objects that are not in the database.
     */
    private int rowId = UNSET_ROW_ID;

    /**
     * The information about the associated table such as the table and column names.
     */
    private TableMetaData tableMetaData;

    /**
     * No argument class constructor; usable by sub-classes only.
     */
    protected BaseConditionsObject() {
        this.fieldValues = new FieldValuesMap();
    }

    /**
     * Public class constructor.
     * <p>
     * This should be used when creating new objects without a list of field values. A new {@link FieldValues} object
     * will be automatically created from the table information.
     *
     * @param connection the database connection
     * @param tableMetaData the table meta data
     */
    public BaseConditionsObject(final Connection connection, final TableMetaData tableMetaData) {
        this.connection = connection;
        this.tableMetaData = tableMetaData;
        this.fieldValues = new FieldValuesMap(tableMetaData);
    }

    /**
     * Fully qualified class constructor.
     * <p>
     * This should be used when creating new objects from a list of field values.
     *
     * @param connection the database connection
     * @param tableMetaData the table meta data
     * @param fields the field values
     */
    public BaseConditionsObject(final Connection connection, final TableMetaData tableMetaData, final FieldValues fields) {
        this.connection = connection;
        this.tableMetaData = tableMetaData;
        this.fieldValues = fields;
    }

    /**
     * Create a SQL insert string for a prepared statement.
     *
     * @return the SQL insert string for a prepared statement
     */
    private String buildInsertStatement() {
        final StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO " + this.tableMetaData.getTableName() + " (");
        for (final String fieldName : this.getTableMetaData().getFieldNames()) {
            sb.append(fieldName + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(") VALUES (");
        for (final String fieldName : this.getTableMetaData().getFieldNames()) {
            sb.append("?, ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(")");
        final String insertSql = sb.toString();
        return insertSql;
    }

    /**
     * Build a SQL update string for a prepared statement.
     *
     * @return the SQL update string for a prepared statement
     */
    private String buildUpdateStatement() {
        final StringBuffer sb = new StringBuffer();
        sb.append("UPDATE " + this.tableMetaData.getTableName() + " SET ");
        for (final String fieldName : this.tableMetaData.getFieldNames()) {
            sb.append(fieldName + " = ?, ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(" WHERE id = ?");
        return sb.toString();
    }

    /**
     * Delete the object from the database using its row ID.
     *
     * @throws DatabaseObjectException if object is not in the database
     * @throws SQLException if there is an error performing the delete operation
     */
    @Override
    public final void delete() throws DatabaseObjectException, SQLException {
        if (this.isNew()) {
            throw new DatabaseObjectException("Missing valid row ID.", this);
        }
        this.connection.setAutoCommit(true);
        PreparedStatement statement = null;
        try {
            statement = this.connection.prepareStatement("DELETE FROM " + this.tableMetaData.getTableName()
                    + " WHERE id = ?");
            statement.setInt(1, this.getRowId());
            statement.executeUpdate();
            this.rowId = UNSET_ROW_ID;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Get the collection ID of the object.
     *
     * @return the collection ID of the object
     */
    @Override
    @Field(names = {"collection_id"})
    public final Integer getCollectionId() {
        if (this.fieldValues.isNonNull(COLLECTION_ID_FIELD)) {
            return this.getFieldValue(Integer.class, COLLECTION_ID_FIELD);
        } else {
            return UNSET_COLLECTION_ID;
        }
    }

    /**
     * Get a field value by name.
     *
     * @param type the return type
     * @param name the name of the field
     */
    @Override
    public final <T> T getFieldValue(final Class<T> type, final String name) {
        return type.cast(this.fieldValues.getValue(type, name));
    }

    /**
     * Get a field value.
     *
     * @param name the field name
     * @param <T> the implicit return type
     * @return the value of field cast to given type
     */
    @Override
    public <T> T getFieldValue(final String name) {
        return (T) this.fieldValues.getValue(name);
    }

    /**
     * Get the field values.
     *
     * @return the field values
     */
    @Override
    public FieldValues getFieldValues() {
        return this.fieldValues;
    }

    /**
     * Get the row ID.
     *
     * @return the row ID
     */
    @Override
    public final int getRowId() {
        return this.rowId;
    }

    /**
     * Get the table meta data for the object.
     *
     * @return the table meta data or <code>null</code> if not set
     */
    @Override
    public final TableMetaData getTableMetaData() {
        return this.tableMetaData;
    }

    /**
     * Return <code>true</code> if collection ID is valid.
     *
     * @return <code>true</code> if collection ID is valid
     */
    @Override
    public boolean hasValidCollectionId() {
        return this.getCollectionId() != UNSET_COLLECTION_ID;
    }

    /**
     * Insert the object into the conditions database.
     */
    @Override
    public final void insert() throws DatabaseObjectException, SQLException {
        if (!this.isNew()) {
            throw new DatabaseObjectException("Cannot insert existing record with row ID: " + this.getRowId(), this);
        }
        if (!this.hasValidCollectionId()) {
            throw new DatabaseObjectException("Cannot insert object without a valid collection ID.", this);
        }
        PreparedStatement insertStatement = null;
        ResultSet resultSet = null;
        try {
            insertStatement = this.connection.prepareStatement(this.buildInsertStatement(),
                    Statement.RETURN_GENERATED_KEYS);
            ConditionsObjectUtilities.setupPreparedStatement(insertStatement, this);
            insertStatement.executeUpdate();
            resultSet = insertStatement.getGeneratedKeys();
            resultSet.next();
            this.rowId = resultSet.getInt(1);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (insertStatement != null) {
                insertStatement.close();
            }
        }
    }

    /**
     * Return <code>true</code> if object is not in the database.
     * <p>
     * This returns <code>true</code> if the object has a valid row ID.
     *
     * @return <code>true</code> if object is not in the database
     */
    @Override
    public final boolean isNew() {
        return this.getRowId() == UNSET_ROW_ID;
    }

    /**
     * Select a conditions object by its row ID.
     *
     * @param id the row ID
     * @return <code>true</code> is selection was performed
     */
    @Override
    public final boolean select(final int id) throws DatabaseObjectException, SQLException {
        this.rowId = id;
        if (id < 1) {
            throw new IllegalArgumentException("Invalid row ID: " + id);
        }
        final StringBuffer sb = new StringBuffer();
        sb.append("SELECT");
        for (final String fieldName : this.tableMetaData.getFieldNames()) {
            sb.append(" " + fieldName + ",");
        }
        sb.setLength(sb.length() - 1);
        sb.append(" FROM " + this.tableMetaData.getTableName());
        sb.append(" WHERE id = " + this.getRowId());
        final String sql = sb.toString();
        Statement statement = null;
        ResultSet resultSet = null;
        boolean selected = false;
        try {
            statement = this.connection.createStatement();
            resultSet = statement.executeQuery(sql);
            selected = resultSet.next();
            if (selected) {
                int columnIndex = 1;
                for (final String fieldName : this.tableMetaData.getFieldNames()) {
                    this.setFieldValue(fieldName, resultSet.getObject(columnIndex));
                    ++columnIndex;
                }
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return selected;
    }

    /**
     * Set the collection ID of the object.
     *
     * @param collectionId the collection ID of the object
     */
    void setCollectionId(final int collectionId) throws ConditionsObjectException {
        this.setFieldValue(COLLECTION_ID_FIELD, collectionId);
    }

    /**
     * Set the JDBC database connection of the object.
     *
     * @param connection the database connection of the object
     */
    @Override
    public final void setConnection(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Set a field value.
     * <p>
     * Calling this method will flag the object as "dirty" meaning it needs to be updated in the database.
     *
     * @param name the name of the field
     * @param value the new value of the field
     */
    @Override
    public final void setFieldValue(final String name, final Object value) {
        this.fieldValues.setValue(name, value);
    }

    /**
     * Set the field values of the object.
     *
     * @param fieldValues the field values of the object
     */
    @Override
    public void setFieldValues(final FieldValues fieldValues) {
        this.fieldValues = fieldValues;
    }

    /**
     * Set the row ID of the object.
     *
     * @param rowId the new row ID
     */
    void setRowId(final int rowId) {
        this.rowId = rowId;
    }

    /**
     * Set the table meta data of the object.
     * <p>
     * This sets which table is associated with the object for database operations.
     *
     * @param tableMetaData the table meta data
     */
    @Override
    public final void setTableMetaData(final TableMetaData tableMetaData) {
        this.tableMetaData = tableMetaData;
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return defaultToString(this);
    }

    /**
     * Perform an update operation to insert this object's data into the database.
     *
     * @return <code>true</code> if an update occurred
     */
    @Override
    public final boolean update() throws DatabaseObjectException, SQLException {
        int rowsUpdated = 0;
        if (this.isNew()) {
            throw new DatabaseObjectException("Cannot perform an update on a new object.", this);
        }
        PreparedStatement updateStatement = null;
        try {
            updateStatement = this.connection.prepareStatement(this.buildUpdateStatement());
            updateStatement.setInt(this.fieldValues.getFieldNames().size() + 1, this.getRowId());
            ConditionsObjectUtilities.setupPreparedStatement(updateStatement, this);
            rowsUpdated = updateStatement.executeUpdate();
        } finally {
            if (updateStatement != null) {
                updateStatement.close();
            }
        }
        return rowsUpdated != 0;
    }
    
    public boolean equals(Object object) {
        // Is it the same object?
        if (object == this) {
            return true;
        }
        // Are these objects the same class?
        if (object.getClass().equals(this.getClass())) {
            BaseConditionsObject otherObject = BaseConditionsObject.class.cast(object);
            // Do the row IDs and database table name match?
            if (otherObject.getTableMetaData().getTableName().equals(this.getTableMetaData().getTableName()) &&
                    this.getRowId() == otherObject.getRowId()) {
                // These are considered the same object (same database table and row ID).
                return true;
            }
        }
        return false;
    }
}
