package org.hps.conditions.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class BaseConditionsObjectCollection<ObjectType extends ConditionsObject> implements
        ConditionsObjectCollection<ObjectType> {

    private int collectionId = BaseConditionsObject.UNSET_COLLECTION_ID;
    private Connection connection;
    private final Set<ObjectType> objects = new LinkedHashSet<ObjectType>();
    private TableMetaData tableMetaData;
    private boolean isDirty;
    private Class<ObjectType> type;

    protected BaseConditionsObjectCollection() {
    }

    public BaseConditionsObjectCollection(final Connection connection, final TableMetaData tableMetaData)
            throws SQLException, DatabaseObjectException {
        this.connection = connection;
        this.tableMetaData = tableMetaData;
        this.type = (Class<ObjectType>) tableMetaData.getObjectClass();
    }

    public BaseConditionsObjectCollection(final Connection connection, final TableMetaData tableMetaData,
            final int collectionId) throws SQLException, DatabaseObjectException {
        this.connection = connection;
        this.tableMetaData = tableMetaData;
        this.collectionId = collectionId;
        if (collectionId != -1) {
            select(collectionId);
        }
        this.type = (Class<ObjectType>) tableMetaData.getObjectClass();
    }

    @Override
    public boolean add(final ObjectType object) throws ConditionsObjectException {
        if (object == null) {
            throw new IllegalArgumentException("The object argument is null.");
        }
        // Does this collection have a valid ID yet?
        if (getCollectionId() != BaseConditionsObject.UNSET_COLLECTION_ID) {
            // Does the object that is being added have a collection ID?
            if (object.getCollectionId() != BaseConditionsObject.UNSET_COLLECTION_ID) {
                // Does the object's collection ID not match?
                if (object.getCollectionId() != this.collectionId) {
                    // Cannot add an object from a different collection.
                    throw new IllegalArgumentException("Cannot add object with different collection ID: "
                            + object.getCollectionId());
                }
            } else {
                try {
                    // Set the collection ID on the object.
                    // FIXME: Uses concrete type instead of interface.
                    ((BaseConditionsObject) object).setCollectionId(this.collectionId);
                } catch (final ConditionsObjectException e) {
                    throw new RuntimeException("Error assigning collection ID " + this.collectionId + " to object.", e);
                }
            }
        }
        final boolean added = this.objects.add(object);
        if (!added) {
            throw new RuntimeException("Failed to add object.");
        }
        this.isDirty = true;
        return added;
    }

    /**
     * @throws ConditionsObjectException
     * @throws SQLException
     */
    @Override
    public final void delete() throws DatabaseObjectException, SQLException {
        Statement statement = null;
        try {
            final String sql = "DELETE FROM `" + this.tableMetaData.getTableName() + "` WHERE collection_id = '"
                    + getCollectionId() + "'";
            statement = this.connection.createStatement();
            statement.executeUpdate(sql);
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public final ObjectType get(final int index) {
        if (index + 1 > this.size() || index < 0) {
            throw new IndexOutOfBoundsException("index out of bounds: " + index);
        }
        int current = 0;
        final Iterator<ObjectType> iterator = this.objects.iterator();
        ObjectType object = iterator.next();
        while (current != index && iterator.hasNext()) {
            object = iterator.next();
            current++;
        }
        return object;
    }

    @Override
    public final int getCollectionId() {
        return this.collectionId;
    }

    @Override
    public final TableMetaData getTableMetaData() {
        return this.tableMetaData;
    }

    /**
     * @param collectionId
     * @throws ConditionsObjectException
     * @throws SQLException
     */
    @Override
    public final void insert() throws DatabaseObjectException, SQLException {

        // Turn off auto-commit to perform a transaction.
        this.connection.setAutoCommit(false);

        if (this.collectionId == BaseConditionsObject.UNSET_COLLECTION_ID) {
            // Automatically get the next global collection ID from the conditions database.
            this.collectionId = this.getNextCollectionId();
        } else {
            // If the collection already exists in the database with this ID then it cannot be inserted.
            if (checkExists()) {
                throw new DatabaseObjectException("The collection " + this.collectionId
                        + " cannot be inserted because it already exists in the " + this.tableMetaData.getTableName()
                        + " table.", this);
            }
        }
        
        // Set collection ID on objects.
        try {
            setConditionsObjectCollectionIds();
        } catch (final ConditionsObjectException e) {
            throw new DatabaseObjectException("Error setting collection IDs on objects.", e, this);
        }

        PreparedStatement insertObjects = null;
        final StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO " + this.getTableMetaData().getTableName() + " (");
        for (final String field : this.getTableMetaData().getFieldNames()) {
            sb.append(field + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(") VALUES (");
        for (int fieldIndex = 0; fieldIndex < this.getTableMetaData().getFieldNames().length; fieldIndex++) {
            sb.append("?, ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(")");
        final String updateStatement = sb.toString();
        try {
            insertObjects = this.connection.prepareStatement(updateStatement, Statement.RETURN_GENERATED_KEYS);
            for (final ObjectType object : this) {
                for (int fieldIndex = 0; fieldIndex < this.getTableMetaData().getFieldNames().length; fieldIndex++) {
                    final String fieldName = this.getTableMetaData().getFieldNames()[fieldIndex];
                    insertObjects.setObject(fieldIndex + 1,
                            object.getFieldValue(getTableMetaData().getFieldType(fieldName), fieldName));
                }
                insertObjects.executeUpdate();
                final ResultSet resultSet = insertObjects.getGeneratedKeys();
                resultSet.next();
                ((BaseConditionsObject) object).setRowId(resultSet.getInt(1));
                resultSet.close();
            }
            // Commit the object insert statements together.
            this.connection.commit();

        } catch (final SQLException e1) {
            e1.printStackTrace();
            if (this.connection != null) {
                try {
                    System.err.println("Transaction is being rolled back ...");
                    this.connection.rollback();
                } catch (final SQLException e2) {
                    e2.printStackTrace();
                }
            }
        } finally {
            if (insertObjects != null) {
                insertObjects.close();
            }
            this.connection.setAutoCommit(true);
        }
    }

    /**
     * Add a row for a new collection in the <i>collections</i> table and return the new collection ID assigned to it.
     *
     * @param tableName the name of the table
     * @param comment an optional comment about this new collection
     * @return the collection's ID
     * @throws SQLException
     */
    private synchronized int getNextCollectionId() throws SQLException, DatabaseObjectException {
        final String log = "BaseConditionsObject generated new collection ID";
        final String description = "inserted " + this.size() + " records into " + this.tableMetaData.getTableName();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        int collectionId = -1;
        try {
            statement = this.connection.prepareStatement(
                    "INSERT INTO collections (table_name, log, description, created) VALUES (?, ?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, this.tableMetaData.getTableName());
            statement.setString(2, log);
            statement.setString(3, description);
            statement.execute();
            resultSet = statement.getGeneratedKeys();
            if (!resultSet.next()) {
                throw new DatabaseObjectException("Failed to create new collection record.", this);
            }
            collectionId = resultSet.getInt(1);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return collectionId;
    }

    @Override
    public final boolean select(final int collectionId) throws SQLException, DatabaseObjectException {
        this.collectionId = collectionId;
        Statement statement = null;
        boolean selected = false;
        try {
            statement = this.connection.createStatement();
            final StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, ");
            for (final String fieldName : this.tableMetaData.getFieldNames()) {
                sb.append(fieldName + ", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append(" FROM " + this.tableMetaData.getTableName() + " WHERE collection_id=" + collectionId);
            final String sql = sb.toString();
            final ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                try {
                    final ObjectType newObject = (ObjectType) this.tableMetaData.getObjectClass().newInstance();
                    newObject.setConnection(this.connection);
                    newObject.setTableMetaData(this.tableMetaData);
                    final int id = resultSet.getInt(1);
                    ((BaseConditionsObject) newObject).setRowId(id);
                    for (int fieldIndex = 0; fieldIndex < this.tableMetaData.getFieldNames().length; fieldIndex++) {
                        final String fieldName = this.tableMetaData.getFieldNames()[fieldIndex];
                        newObject.setFieldValue(fieldName, resultSet.getObject(fieldIndex + 2));
                    }
                    try {
                        add(newObject);
                    } catch (final ConditionsObjectException e) {
                        throw new DatabaseObjectException("Error adding object to collection.", e, newObject);
                    }
                    selected = true;
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return selected;
    }

    @Override
    public final void setCollectionId(final int collectionId) {
        this.collectionId = collectionId;
        try {
            // Set collection ID on all objects.
            setConditionsObjectCollectionIds();
        } catch (final ConditionsObjectException e) {
            throw new RuntimeException("Error setting collection ID on object.", e);
        }
    }

    private final void setConditionsObjectCollectionIds() throws ConditionsObjectException {
        if (this.collectionId != BaseConditionsObject.UNSET_COLLECTION_ID) {
            for (final ConditionsObject object : this) {
                ((BaseConditionsObject) object).setCollectionId(this.collectionId);
            }
        }
    }

    private final void setConditionsObjectConnections() {
        for (final ConditionsObject object : this) {
            object.setConnection(this.connection);
        }
    }

    @Override
    public void setConnection(final Connection connection) {
        this.connection = connection;

        // Set connection on all objects.
        setConditionsObjectConnections();
    }

    @Override
    public void setTableMetaData(final TableMetaData tableMetaData) {
        this.tableMetaData = tableMetaData;
    }

    @Override
    public int size() {
        return this.objects.size();
    }

    @Override
    // FIXME: This method should execute a prepared statement in a transaction instead of performing individual updates.
    public boolean update() throws DatabaseObjectException, SQLException {
        boolean updated = false;
        for (final ObjectType object : this.objects) {
            if (object.isDirty()) {
                if (object.update() && updated == false) {
                    updated = true;
                }
            }
        }
        return updated;
    }

    /**
     * Sort the collection in place.
     *
     * @param comparator the comparison to use for sorting
     */
    @Override
    public final void sort(final Comparator<ObjectType> comparator) {
        final List<ObjectType> list = new ArrayList<ObjectType>(this.objects);
        Collections.sort(list, comparator);
        this.objects.clear();
        this.objects.addAll(list);
    }

    @Override
    public ConditionsObjectCollection<ObjectType> sorted(final Comparator<ObjectType> comparator) {
        final List<ObjectType> list = new ArrayList<ObjectType>(this.objects);
        Collections.sort(list, comparator);
        BaseConditionsObjectCollection<ObjectType> collection;
        try {
            collection = this.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating new collection instance.", e);
        }
        for (final ObjectType object : list) {
            try {
                collection.add(object);
            } catch (final ConditionsObjectException e) {
                throw new RuntimeException("Error adding to new collection in sorted method.", e);
            }
        }
        return collection;
    }

    @Override
    public boolean contains(final Object object) {
        return this.objects.contains(object);
    }

    @Override
    public void addAll(final ConditionsObjectCollection<ObjectType> collection) {
        for (final ObjectType object : collection) {
            this.objects.add(object);
        }
    }

    @Override
    public Iterator<ObjectType> iterator() {
        return this.objects.iterator();
    }

    @Override
    public boolean isDirty() {
        return this.isDirty;
    }

    @Override
    public boolean isNew() {
        if (this.collectionId == BaseConditionsObject.UNSET_COLLECTION_ID) {           
            return true;
        }
        try {
            // Valid collection ID exists but need to check if inserted into database yet.
            return checkExists() == false;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // FIXME: Use PreparedStatement instead of Statement.
    private boolean checkExists() throws SQLException {                     
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        boolean exists = false;
        try {
            statement = this.connection.prepareStatement("SELECT id FROM " + this.tableMetaData.getTableName()
                    + " where collection_id = ?");
            statement.setInt(1, this.collectionId);
            resultSet = statement.executeQuery();
            exists = resultSet.next();
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return exists;
    }

    protected Set<ObjectType> getObjects() {
        return this.objects;
    }

    protected void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }
}
