package org.hps.conditions;

import static org.hps.conditions.ConditionsTableConstants.CONDITIONS_RECORD;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.lcsim.conditions.CachedConditions;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class represents a single record from the primary conditions data table,
 * which defines the validity range for a specific collection of conditions objects.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsRecord {

    int id;
    int runStart;
    int runEnd;
    Date updated;
    Date created;
    Date validFrom;
    Date validTo;
    String createdBy;
    String notes;
    String name;
    String formatVersion;
    String tableName;
    String fieldName;
    int fieldValue;    
                
    protected ConditionsRecord() {        
    }
    
    /**
     * Load state into this object from a ResultSet, which must be positioned a priori
     * to the correct row number.
     * @param rs The ResultSet containing database records from the conditions table.
     */
    void load(ResultSet rs) {
        try {            
            id = rs.getInt(1);
            runStart = rs.getInt(2);
            runEnd = rs.getInt(3);
            updated = rs.getTimestamp(4);
            created = rs.getDate(5);
            validFrom = rs.getDate(6);
            validTo = rs.getDate(7);
            createdBy = rs.getString(8);
            Blob blob = rs.getBlob(9);
            if (blob != null) {
                byte[] blobData = blob.getBytes(1, (int)blob.length());
                notes = new String(blobData);
            }
            name = rs.getString(10);
            formatVersion = rs.getString(11);
            tableName = rs.getString(12);
            fieldName = rs.getString(13);
            fieldValue = rs.getInt(14);
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
     
    /**
     * Get the unique ID.
     * @return The unique ID.
     */
    public int getId() {
        return id;
    }
    
    /**
     * Get the starting run number.
     * @return The starting run number.
     */
    public int getRunStart() {
        return runStart;
    }
    
    /**
     * Get the ending run number.
     * @return The ending run number.
     */
    public int getRunEnd() {
        return runEnd;
    }
    
    /**
     * Get the date this record was last updated.
     * @return The date this record was updated.
     */
    public Date getUpdated() {
        return updated;
    }
    
    /**
     * Get the date this record was created.
     * @return The date this record was created.
     */
    public Date getCreated() {
        return created;
    }
    
    /**
     * Get the starting valid time.
     * @return The starting valid time.
     */
    public Date getValidFrom() {
        return validFrom;
    }
    
    /**
     * Get the ending valid time.
     * @return The ending valid time.
     */
    public Date getValidTo() {
        return validTo;
    }

    /**
     * Get the name of the user who created this record.
     * @return The name of the person who created the record.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Get the notes.
     * @return The notes about this condition.
     */
    public String getNotes() {
        return notes;
    }
    
    /**
     * Get the name of these conditions, which should be unique by run number.
     * @return The name of the conditions.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the version of the format these conditions are stored in.
     * @return The conditions version.
     */
    public String getFormatVersion() {
        return formatVersion;
    }
    
    /**
     * Get the name of the table containing the actual raw conditions data.
     * @return The name of the table with the conditions data. 
     */
    public String getTableName() {
        return tableName;
    }
    
    /**
     * Get the field that will define which set of conditions to fetch.
     * @return The field used as a group ID of the conditions.
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Get the value of the identifying field.
     * @return The value of identifying field for these conditions.
     */
    public int getFieldValue() {
        return fieldValue;
    }    
    
    /**
     * Convert this record to a human readable string, one field per line.
     * @return This object represented as a string.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("id: " + id + '\n');
        buff.append("runStart: " + runStart + '\n');
        buff.append("runEnd: " + runEnd + '\n');
        buff.append("updated: " + updated + '\n');
        buff.append("created: " + created + '\n');
        buff.append("validFrom: " + validFrom + '\n');
        buff.append("validTo: " + validTo + '\n');
        buff.append("createdBy: " + createdBy + '\n');
        buff.append("notes: " + notes + '\n');
        buff.append("formatVersion: " + formatVersion + '\n');
        buff.append("tableName: " + tableName + '\n');
        buff.append("fieldName: " + fieldName + '\n');
        buff.append("fieldValue: " + fieldValue + '\n');
        return buff.toString();
    }
    
    /**
     * Find a ConditionsRecord with conditions key matching <code>name</code>.
     * @param manager The current conditions manager.
     * @param name The name of the conditions key.
     * @return The matching ConditionsRecord.
     * @throws IllegalArgumentException if no records are found.
     * @throws IllegalArgumentException if more than one record is found.
     */
    public static ConditionsRecordCollection find(ConditionsManager manager, String name) {
        CachedConditions<ConditionsRecordCollection> c = manager.getCachedConditions(ConditionsRecordCollection.class, CONDITIONS_RECORD);
        ConditionsRecordCollection conditionsRecords = c.getCachedData();
        conditionsRecords = conditionsRecords.find(name);
        if (conditionsRecords.size() == 0) {
            throw new IllegalArgumentException("No ConditionsRecord with name: " + name);
        }              
        //if (conditionsRecords.size() > 1) {
        //    throw new IllegalArgumentException("Duplicate ConditionsRecord with name: " + name);
        //}
        return conditionsRecords;
    }
    
    /**
     * Find conditions records of all types in the database by run number.
     * @param run The run number.
     * @return The set of matching ConditionsRecords.
     */
    public static ConditionsRecordCollection find(int run) {
        
        ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();
        
        ConnectionManager manager = ConnectionManager.getConnectionManager();
        String db = manager.getConnectionParameters().getDatabase();
        String table = manager.getConnectionParameters().getConditionsTable();
                
        String query = "SELECT * FROM " 
                + db + "." + table                       
                + " WHERE "
                + "run_start <= "
                + run
                + " AND run_end >= "
                + run;
        
        ResultSet resultSet = manager.query(query);
            
        // Iterate over conditions records.
        try {
            while (resultSet.next()) {
                ConditionsRecord record = new ConditionsRecord();
                record.load(resultSet);
                conditionsRecords.add(record);
            } 
        } catch (SQLException x) {
            throw new RuntimeException("Database error", x);
        } 
                   
        return conditionsRecords;
    }
}