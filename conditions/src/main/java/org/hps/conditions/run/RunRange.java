package org.hps.conditions.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;

/**
 * Used with the {@link RunSpreadsheet} to find ranges of runs where columns have the same values so they can be assigned
 * a conditions record with a run start and end range.
 * <p>
 * Bad rows such as ones without run numbers or with invalid data values are skipped and not included in a range.
 * 
 * @author Jeremy McCormick
 */
public final class RunRange {

    /**
     * Find run ranges for conditions data given a set of column names and the full run spreadsheet. 
     * 
     * @param runSheet the run spreadsheet data (from CSV file)
     * @param columnNames the names of the columns
     * @return the list of run ranges
     */
    public static final List<RunRange> findRunRanges(final RunSpreadsheet runSheet, final Set<String> columnNames) {
        
        final List<RunRange> ranges = new ArrayList<RunRange>();

        final Iterator<CSVRecord> it = runSheet.getRecords().iterator();
        CSVRecord record = it.next();

        RunRange range = null;

        // Iterate over all the records.
        while (it.hasNext()) {
            record = it.next();
            // Is the record valid?
            if (isValidRecord(record, columnNames)) {
                if (range == null) {
                    // Create new range for the valid row. 
                    range = new RunRange(columnNames);
                } else {                    
                    // If this record is not in the range then add a new range.
                    if (!range.inRange(record)) {
                        // Add the current range and create a new one.
                        ranges.add(range);
                        range = new RunRange(columnNames);
                    }
                }                
                
                // Update the range from the current record.
                range.update(record);
            } else {
                if (range != null) {
                    // Add the range and set to null as this record is invalid.
                    ranges.add(range);
                    range = null;
                }
            }
        }

        return ranges;
    }
    
    /**
     * Return <code>true</code> if the <code>CSVRecord</code> is valid, which means it has a run number and 
     * data in the columns used by this range.
     * 
     * @param record the <code>CSVRecord</code> to check
     * @param columnNames the names of the columns
     * @return <code>true</code> if record is valid
     */
    private static boolean isValidRecord(final CSVRecord record, final Set<String> columnNames) {
        try {
            // Check if run number can be parsed.
            Integer.parseInt(record.get("run"));
        } catch (final NumberFormatException e) {
            // Run number is bad.
            return false;
        }
        for (final String columnName : columnNames) {
            // Check that required column data is not null, blank, or empty string.
            if (record.get(columnName) == null || "".equals(record.get(columnName)) || record.get(columnName).length() == 0) {                
                return false;
            }
        }
        return true;
    }

    /**
     * The names of the columns.
     */
    private final Set<String> columnNames;

    /**
     * The start of the run range.
     */
    private int runEnd = Integer.MIN_VALUE;

    /**
     * The end of the run range.
     */
    private int runStart = Integer.MAX_VALUE;

    /**
     * The mapping of column names to values.
     */
    private Map<String, String> values = new LinkedHashMap<String, String>();

    /**
     * Create a new range.
     * 
     * @param columnNames the names of the columns
     */
    RunRange(final Set<String> columnNames) {
        if (columnNames == null) {
            throw new IllegalArgumentException("columnNames is null");
        }
        this.columnNames = columnNames;
    }

    /**
     * Get the last run number in the range.
     * 
     * @return the last run number in the range
     */
    public int getRunEnd() {
        return runEnd;
    }

    /**
     * Get the first run number in the range.
     * 
     * @return the first run number in the range
     */
    public int getRunStart() {
        return runStart;
    }
    
    /**
     * Get get value of a field by column name.
     * 
     * @param columnName the column name
     * @return the value of the field
     */
    public String getValue(String columnName) {
        return this.values.get(columnName);
    }
    
    /**
     * Get the raw values of the data.
     * 
     * @return the raw data values
     */
    public Collection<String> getValues() {
        return this.values.values();
    }
    
    /**
     * Get the names of the columns used by this range.
     * 
     * @return the names of the columns
     */
    public Set<String> getColumnNames() {
        return this.columnNames;
    }

    /**
     * Return <code>true</code> if the record is in the range, e.g. its data values are the same.    
     * 
     * @param record the <code>CSVRecord</code> containing the run data
     * @return <code>true</code> if the the record is in range
     */
    private boolean inRange(final CSVRecord record) {
        for (final String columnName : columnNames) {
            if (!record.get(columnName).equals(this.values.get(columnName))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Update the range from a record.     
     * @param record the <code>CSVRecord</code> with the run's data
     */
    private void update(final CSVRecord record) {                
        final int run = Integer.parseInt(record.get("run"));
        if (run < runStart) {
            this.runStart = run;
        }
        if (run > runEnd) {
            this.runEnd = run;
        }
        if (values.size() == 0) {
            for (final String columnName : columnNames) {
                this.values.put(columnName, record.get(columnName));
            }
        }
    }
    
    /**
     * Convert this object to a string.
     * 
     * @return this object converted to a string
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RunRange { ");
        sb.append("runStart: " + this.runStart + ", ");
        sb.append("runEnd: " + this.runEnd + ", ");
        for (String columnName : columnNames) {
            sb.append(columnName + ": " + values.get(columnName) + ", " );
        }
        sb.setLength(sb.length() - 2);
        sb.append(" }");
        return sb.toString();
    }
    
    /**
     * Get the list of unique values from a set of run ranges.
     * 
     * @param ranges the run ranges
     * @return the list of unique values from the field values
     */
    public static List<Collection<String>> getUniqueValues(List<RunRange> ranges) {
        Iterator<RunRange> it = ranges.iterator();        
        List<Collection<String>> uniqueValuesList = new ArrayList<Collection<String>>();
        while(it.hasNext()) {        
            RunRange range = it.next();
            Collection<String> values = range.getValues();
            if (!contains(values, uniqueValuesList)) {
                uniqueValuesList.add(values);
            }           
        }
        return uniqueValuesList;
    }
    
    /**
     * Return <code>true</code> if the values are already in the unique values list.
     * 
     * @param values the list of field values for a row
     * @param uniqueValuesList the unique values list
     * @return <code>true</code> if the values are already in the unique values list
     */
    private static boolean contains(Collection<String> values, List<Collection<String>> uniqueValuesList) {
        for (Collection<String> uniqueValues : uniqueValuesList) {
            Iterator<String> valuesIterator = values.iterator();
            Iterator<String> uniqueValuesIterator = uniqueValues.iterator();
            boolean equals = true;
            while (valuesIterator.hasNext() && uniqueValuesIterator.hasNext()) {
                String value = valuesIterator.next(); 
                String uniqueValue = uniqueValuesIterator.next();
                if (!value.equals(uniqueValue)) {
                    equals = false;
                    break;
                }
            }
            if (equals) {
                return true;
            }
        }
        return false;
    }
}
