package org.hps.monitoring.trigger;

import javax.swing.table.AbstractTableModel;

/**
 * Class <code>TableTextModel</code> is a simple implementation of
 * <code>AbstractTableModel</code> that supports a definable number
 * of rows and columns which must be populated with <code>String</code>
 * data.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TableTextModel extends AbstractTableModel {
	// Serial UID.
	private static final long serialVersionUID = 0L;
	
	// Stored values.
	private final int rows, columns;
	private final String[][] values;
	
	/**
	 * Instantiates a new <code>TableTextModel</code> with the indicated
	 * number of rows and columns.
	 * @param rows - The number of rows.
	 * @param columns - The number of columns.
	 */
	public TableTextModel(int rows, int columns) {
		// Make sure that the arguments for rows and columns are valid.
		if(rows < 1) {
			throw new IllegalArgumentException("TableTextModel must have at least one row.");
		} else 	if(columns < 1) {
			throw new IllegalArgumentException("TableTextModel must have at least one column.");
		}
		
		// Define the number of rows and columns.
		this.rows = rows;
		this.columns = columns;
		
		// Instantiate the data storage array.
		values = new String[rows][columns];
	}
	
	@Override
	public int getRowCount() { return rows; }
	
	@Override
	public int getColumnCount() { return columns; }
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// Ensure that the value is within the allowed range.
		validateIndex(rowIndex, columnIndex);
		
		// Return the value.
		return values[rowIndex][columnIndex];
	}
	
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		// If the object is a string, pass it to the preferred handler.
		// This can also be performed if the value is null.
		if(value == null || value instanceof String) {
			setValueAt((String) value, rowIndex, columnIndex);
		}
		
		// Otherwise, cast the object to a string and use that instead.
		else { setValueAt(value.toString(), rowIndex, columnIndex); }
	}
	
	/**
	 * Sets the text for the indicated column and row of the table.
	 * @param value - The new text.
	 * @param rowIndex - The row.
	 * @param columnIndex - The column.
	 * @throws IndexOutOfBoundsException Occurs if the row and column
	 * are not a valid member of table model.
	 */
	public void setValueAt(String value, int rowIndex, int columnIndex) throws IndexOutOfBoundsException {
		// Ensure that the value is within the allowed range.
		validateIndex(rowIndex, columnIndex);
		
		// Set the value.
		values[rowIndex][columnIndex] = value;
	}
	
	/**
	 * Checks to make sure that a given row/column pointer refers to
	 * an extant position in the data array. In the event that the row
	 * and column values are not valid, an <code>IndexOutOfBounds</code>
	 * exception is thrown.
	 * @param rowIndex - The row index.
	 * @param columnIndex - The column index.
	 * @throws IndexOutOfBoundsException Occurs if the row and column
	 * are not a valid member of the data array.
	 */
	private void validateIndex(int rowIndex, int columnIndex) throws IndexOutOfBoundsException {
		if(rowIndex < 0 || rowIndex >= getRowCount()) {
			throw new IndexOutOfBoundsException(String.format("Row index %d is out of bounds.", rowIndex));
		} else if(columnIndex < 0 || columnIndex >= getColumnCount()) {
			throw new IndexOutOfBoundsException(String.format("Column index %d is out of bounds.", columnIndex));
		}
	}
}
