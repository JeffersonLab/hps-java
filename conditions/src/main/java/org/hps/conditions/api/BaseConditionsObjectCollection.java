package org.hps.conditions.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * Implementation of the {@link ConditionsObjectCollection} interface.
 *
 * @param <ObjectType> the type of the object contained in this collection
 * @author Jeremy McCormick, SLAC
 */
public class BaseConditionsObjectCollection<ObjectType extends ConditionsObject> implements
        ConditionsObjectCollection<ObjectType> {

    /**
     * The collection ID.
     */
    private int collectionId = BaseConditionsObject.UNSET_COLLECTION_ID;

    /**
     * The database connection.
     */
    private Connection connection;

    /**
     * The set of objects contained in the collection (no duplicate references allowed to the same object).
     */
    private final Set<ObjectType> objects = new LinkedHashSet<ObjectType>();

    /**
     * The collection's table meta data mapping it to the database.
     */
    private TableMetaData tableMetaData;

    /**
     * No argument constructor; usable by sub-classes.
     */
    protected BaseConditionsObjectCollection() {
    }

    /**
     * Constructor with connection and meta data without field values.
     *
     * @param connection the database connection
     * @param tableMetaData the table meta data
     * @throws SQLException if there
     * @throws DatabaseObjectException
     */
    public BaseConditionsObjectCollection(final Connection connection, final TableMetaData tableMetaData) {
        this.connection = connection;
        this.tableMetaData = tableMetaData;
    }

    /**
     * Constructor which selects data into the collection using a collection ID.
     *
     * @param connection the database connection
     * @param tableMetaData the table meta data
     * @param collectionId the collection ID
     * @throws SQLException if there is an error executing SQL query
     * @throws DatabaseObjectException if there is an error relating to the conditions object API
     */
    public BaseConditionsObjectCollection(final Connection connection, final TableMetaData tableMetaData,
            final int collectionId) throws SQLException, DatabaseObjectException {
        this.connection = connection;
        this.tableMetaData = tableMetaData;
        this.collectionId = collectionId;
        if (collectionId != -1) {
            this.select(collectionId);
        }
    }

    /**
     * Add an object to the collection.
     *
     * @param object the object to add to the collection
     */
    @Override
    public boolean add(final ObjectType object) throws ConditionsObjectException {
        if (object == null) {
            throw new IllegalArgumentException("The object argument is null.");
        }
        //checkCollectionId(object);
        final boolean added = this.objects.add(object);
        if (!added) {
            throw new RuntimeException("Failed to add object.");
        }
        return added;
    }

    private void checkCollectionId(final ObjectType object) {
        // Does this collection have a valid ID yet?
        if (this.getCollectionId() != BaseConditionsObject.UNSET_COLLECTION_ID) {
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
    }

    /**
     * Add all objects from a collection.
     *
     * @param collection the collection with objects to add
     */
    @Override
    public void addAll(final ConditionsObjectCollection<ObjectType> collection) {
        for (final ObjectType object : collection) {
            this.objects.add(object);
        }
    }

    /**
     * Build a SQL insert statement.
     *
     * @return the SQL insert statement
     */
    private String buildInsertStatement() {
        final StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO " + this.getTableMetaData().getTableName() + " (");
        for (final String field : this.getTableMetaData().getFieldNames()) {
            sb.append(field + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(") VALUES (");
        for (final String fieldName : this.getTableMetaData().getFieldNames()) {
            sb.append("?, ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(")");
        final String updateStatement = sb.toString();
        return updateStatement;
    }

    /**
     * Clear the objects from this collection and reset its ID.
     * <p>
     * This has no effect on the underlying database values.
     */
    @Override
    public void clear() {
        this.objects.clear();
    }

    /**
     * Return <code>true</code> if object is contained in this collection.
     *
     * @return <code>true</code> if object is contained in this collection
     */
    @Override
    public final boolean contains(final Object object) {
        return this.objects.contains(object);
    }

    /**
     * Delete the objects in the collection from database.
     *
     * @throws ConditionsObjectException if there is an error deleting the object
     * @throws SQLException if there was an error executing the query
     */
    @Override
    public final void delete() throws DatabaseObjectException, SQLException {
        Statement statement = null;
        try {
            final String sql = "DELETE FROM `" + this.tableMetaData.getTableName() + "` WHERE collection_id = '"
                    + this.getCollectionId() + "'";
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

    /**
     * Return <code>true</code> if collection exists in the database.
     *
     * @return <code>true</code> if collection exists in the database
     * @throws SQLException if there is a query error
     */
    private boolean exists() throws SQLException {
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

    /**
     * Get the object at the index.
     *
     * @param index the object index
     * @return the object at the index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
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

    /**
     * Get the collection ID.
     *
     * @return the collection ID
     */
    @Override
    public final int getCollectionId() {
        return this.collectionId;
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
        final String description = System.getProperty("user.name") + " created new collection in "
                + this.tableMetaData.getTableName();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        int nextCollectionId = -1;
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
            nextCollectionId = resultSet.getInt(1);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return nextCollectionId;
    }

    /**
     * Get the set of objects in this collection.
     *
     * @return the set of objects in this collection
     */
    protected Set<ObjectType> getObjects() {
        return this.objects;
    }

    /**
     * Get the table meta data.
     *
     * @return the table meta data
     * @see TableMetaData
     */
    @Override
    public final TableMetaData getTableMetaData() {
        return this.tableMetaData;
    }

    /**
     * Insert the objects from this collection into the database.
     * <p>
     * The collection ID will be determined automatically if it is not already set.
     *
     * @throws ConditionsObjectException if there is an error inserting the objects
     * @throws SQLException if there is a query error
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
            if (this.exists()) {
                throw new DatabaseObjectException("The collection ID " + this.collectionId
                        + " cannot be inserted because it already exists in the " + this.tableMetaData.getTableName()
                        + " table.", this);
            }
        }
        // Set collection ID on objects.
        try {
            this.setConditionsObjectCollectionIds();
        } catch (final ConditionsObjectException e) {
            throw new DatabaseObjectException("Error setting collection IDs on objects.", e, this);
        }
        PreparedStatement insertStatement = null;
        try {
            insertStatement = this.connection.prepareStatement(this.buildInsertStatement(),
                    Statement.RETURN_GENERATED_KEYS);
            for (final ObjectType object : this) {
                object.setConnection(this.connection);
                object.setTableMetaData(this.tableMetaData);
                ConditionsObjectUtilities.setupPreparedStatement(insertStatement, object);
                insertStatement.executeUpdate();
                final ResultSet resultSet = insertStatement.getGeneratedKeys();
                resultSet.next();
                ((BaseConditionsObject) object).setRowId(resultSet.getInt(1));
                resultSet.close();
            }
            // Commit all the object insert statements together.
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
            if (insertStatement != null) {
                insertStatement.close();
            }
            this.connection.setAutoCommit(true);
        }
    }

    /**
     * Return <code>true</code> if the collection is new, e.g. it is not in the database.
     *
     * @return <code>true</code> if the collection is new
     */
    @Override
    public final boolean isNew() {
        if (this.collectionId == BaseConditionsObject.UNSET_COLLECTION_ID) {
            return true;
        }
        try {
            // Valid collection ID exists but need to check if inserted into database yet.
            return this.exists() == false;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an iterator for the collection.
     *
     * @return an iterator for the collection
     */
    @Override
    public final Iterator<ObjectType> iterator() {
        return this.objects.iterator();
    }

    /**
     * Load data from a CSV file.
     *
     * @param file the CSV file
     */
    @Override
    public void loadCsv(final File file) throws IOException, FileNotFoundException,
            ConditionsObjectException {

        // Clear the objects from the collection.
        this.objects.clear();

        final TableMetaData tableMetaData = this.getTableMetaData();

        // Check if the table info exists.
        if (tableMetaData == null) {
            // Table name is invalid.
            throw new RuntimeException("The table meta data is not set.");
        }

        FileReader reader = null;
        CSVParser parser = null;

        try {

            // Read in the CSV records.
            reader = new FileReader(file);

            parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
            final List<CSVRecord> records = parser.getRecords();

            // Get the database field names from the table info.
            final Set<String> fields = tableMetaData.getFieldNames();

            // Get the text file column headers from the parser.
            final Map<String, Integer> headerMap = parser.getHeaderMap();

            // Get the headers that were read in from CSV.
            final Set<String> headers = headerMap.keySet();

            // Make sure the headers are actually valid column names in the database.
            for (final String header : headerMap.keySet()) {
                if (!fields.contains(header)) {
                    // The field name does not match a table column.
                    throw new RuntimeException("Header " + header + " from CSV is not a column in the "
                            + tableMetaData.getTableName() + " table.");
                }
            }

            // Get the class of the objects contained in this collection.
            final Class<? extends ConditionsObject> objectClass = this.getTableMetaData().getObjectClass();

            // Iterate over the CSV records.
            for (final CSVRecord record : records) {

                // Create a new conditions object.
                final ObjectType object;
                try {
                    // Create a new conditions object and cast to correct type for adding to collection.
                    object = (ObjectType) objectClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Error creating conditions object.", e);
                }

                // Set the field values on the object.
                for (final String header : headers) {
                    // Set the value of a field in the object based on the header name, converting to the correct type.
                    object.setFieldValue(
                            header,
                            ConditionsObjectUtilities.convertValue(this.getTableMetaData().getFieldType(header),
                                    record.get(header)));
                }

                // Add the object to the collection.
                this.add(object);
            }
        } finally {
            // Close the CSV parser and reader.
            parser.close();
            reader.close();
        }
    }

    /**
     * Select objects into this collection by their collection ID in the database.
     *
     * @return <code>true</code> if at least one object was selected
     */
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
            sb.append(" FROM " + this.tableMetaData.getTableName() + " WHERE collection_id = " + collectionId);
            final String sql = sb.toString();
            final ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                try {
                    final ObjectType newObject = (ObjectType) this.tableMetaData.getObjectClass().newInstance();
                    newObject.setConnection(this.connection);
                    newObject.setTableMetaData(this.tableMetaData);
                    final int id = resultSet.getInt(1);
                    ((BaseConditionsObject) newObject).setRowId(id);
                    int column = 2;
                    for (final String fieldName : this.tableMetaData.getFieldNames()) {
                        newObject.setFieldValue(fieldName, resultSet.getObject(column));
                        ++column;
                    }
                    try {
                        this.add(newObject);
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

    /**
     * Set the collection ID of this collection.
     *
     * @param collectionId the collection ID
     */
    @Override
    public final void setCollectionId(final int collectionId) {
        this.collectionId = collectionId;
        try {
            // Set collection ID on all objects.
            this.setConditionsObjectCollectionIds();
        } catch (final ConditionsObjectException e) {
            throw new RuntimeException("Error setting collection ID on object.", e);
        }
    }

    /**
     * Iterate over the objects in the collection and set their collection IDs.
     *
     * @throws ConditionsObjectException if there is an error setting one of the collection IDs
     */
    private final void setConditionsObjectCollectionIds() throws ConditionsObjectException {
        if (this.collectionId != BaseConditionsObject.UNSET_COLLECTION_ID) {
            for (final ConditionsObject object : this) {
                ((BaseConditionsObject) object).setCollectionId(this.collectionId);
            }
        }
    }

    /**
     * Iterate over the objects in the collection and set their database connection reference.
     */
    private final void setConditionsObjectConnections() {
        for (final ConditionsObject object : this) {
            object.setConnection(this.connection);
        }
    }

    /**
     * Set the database connection of the collection.
     *
     * @param connection the database connection of the collection
     */
    @Override
    public final void setConnection(final Connection connection) {
        this.connection = connection;

        // Set connection on all objects.
        this.setConditionsObjectConnections();
    }

    /**
     * Set the table meta data of the collection.
     *
     * @param tableMetaData the table meta data of the collection
     * @see TableMetaData
     */
    @Override
    public void setTableMetaData(final TableMetaData tableMetaData) {
        this.tableMetaData = tableMetaData;
    }

    /**
     * Get the size of the collection.
     *
     * @return the size of the collection
     */
    @Override
    public int size() {
        return this.objects.size();
    }

    /**
     * Sort the collection in place.
     *
     * @param comparator the comparison operator to use for sorting
     */
    @Override
    public final void sort(final Comparator<ObjectType> comparator) {
        final List<ObjectType> list = new ArrayList<ObjectType>(this.objects);
        Collections.sort(list, comparator);
        this.objects.clear();
        this.objects.addAll(list);
    }

    /**
     * Return a sorted copy of this collection, leaving the original unchanged.
     *
     * @param comparator the comparison class to use for sorting
     * @return a sorted copy of this collection
     */
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

    /**
     * Convert object to string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        final StringBuffer buff = new StringBuffer();
        for (final ConditionsObject object : this.getObjects()) {
            buff.append(object);
            buff.append('\n');
        }
        return buff.toString();
    }

    /**
     * Perform database updates on the objects in the collection.
     *
     * @return <code>true</code> if at least one object was updated
     */
    // FIXME: Rewrite to use PreparedStatement.
    @Override
    public boolean update() throws DatabaseObjectException, SQLException {
        boolean updated = false;
        for (final ObjectType object : this.objects) {
            if (object.update() && updated == false) {
                updated = true;
            }
        }
        return updated;
    }

    /**
     * Write contents of this collection to a CSV file.
     *
     * @param file the output CSV file
     */
    @Override
    public void writeCsv(final File file) throws IOException {
        FileWriter fileWriter = null;
        CSVPrinter csvFilePrinter = null;
        try {
            fileWriter = new FileWriter(file);
            csvFilePrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);
            final List<String> fieldNameList = new ArrayList<String>(this.getTableMetaData().getFieldNames());
            fieldNameList.remove(BaseConditionsObject.COLLECTION_ID_FIELD);
            csvFilePrinter.printRecord(fieldNameList);
            for (final ConditionsObject conditionsObject : this.getObjects()) {
                final List<Object> record = new ArrayList<Object>();
                for (final String fieldName : fieldNameList) {
                    Object value = conditionsObject.getFieldValue(fieldName);
                    if (value instanceof Date) {
                        value = BaseConditionsObject.DEFAULT_DATE_FORMAT.format(value);
                    }
                    record.add(value);
                }
                csvFilePrinter.printRecord(record);
            }
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
                csvFilePrinter.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}