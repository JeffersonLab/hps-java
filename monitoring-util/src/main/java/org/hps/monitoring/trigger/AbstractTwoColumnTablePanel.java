package org.hps.monitoring.trigger;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Class <code>AbstractTwoColumnTablePanel</code> is an implementation
 * of <code>AbstractTablePanel</code> that specifically handles tables
 * with two columns where the first column's cells are row headers and
 * the second column contains values.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see AbstractTablePanel
 */
public abstract class AbstractTwoColumnTablePanel extends AbstractTablePanel {
    // Static variables.
    private static final long serialVersionUID = 0L;
    
    // Table models.
    private TableTextModel localModel;
    private TableTextModel globalModel;
    
    // Table model mappings.
    private static final int COL_TITLE = 0;
    private static final int COL_VALUE = 1;
    
    /**
     * Instantiates an <code>AbstractTwoColumnTablePanel</code> object
     * with the indicated row names.
     * @param rowNames - The names of the rows.
     */
    public AbstractTwoColumnTablePanel(String[] rowNames) {
        super((Object[]) rowNames);
    }
    
    @Override
    protected JTable[] initializeTables(Object... args) {
        // The arguments should be a string array.
        if(!(args instanceof String[])) {
            throw new IllegalArgumentException("Row names must be strings!");
        }
        String[] rowNames = (String[]) args;
        
        // Initialize the table models. They should have two columns
        // (one for values and one for headers) and a number of rows
        // equal to the number of row names.
        localModel = new TableTextModel(rowNames.length, 2);
        globalModel = new TableTextModel(rowNames.length, 2);
        
        // Initialize the titles.
        for(int i = 0; i < rowNames.length; i++) {
            localModel.setValueAt(rowNames[i], i, COL_TITLE);
            globalModel.setValueAt(rowNames[i], i, COL_TITLE);
        }
        updatePanel(null, null);
        
        // Make a cell renderer.
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        // Create JTable objects to display the data.
        JTable localTable = new JTable(localModel);
        localTable.setRowSelectionAllowed(false);
        localTable.setColumnSelectionAllowed(false);
        localTable.setCellSelectionEnabled(false);
        localTable.setShowVerticalLines(false);
        localTable.getColumnModel().getColumn(0).setMinWidth(200);
        localTable.getColumnModel().getColumn(0).setMaxWidth(200);
        localTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        localTable.setFont(new Font("monospaced", localTable.getFont().getStyle(), localTable.getFont().getSize()));
        
        JTable globalTable = new JTable(globalModel);
        globalTable.setRowSelectionAllowed(false);
        globalTable.setColumnSelectionAllowed(false);
        globalTable.setCellSelectionEnabled(false);
        globalTable.setShowVerticalLines(false);
        globalTable.getColumnModel().getColumn(0).setMinWidth(200);
        globalTable.getColumnModel().getColumn(0).setMaxWidth(200);
        globalTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        globalTable.setFont(new Font("monospaced", globalTable.getFont().getStyle(), globalTable.getFont().getSize()));
        
        // Return the two tables.
        return new JTable[] { localTable, globalTable };
    }
    
    /**
     * Sets the value of the indicated row for the global statistical
     * table.
     * @param rowIndex - The row.
     * @param value - The new value.
     */
    protected void setGlobalRowValue(int rowIndex, String value) {
        globalModel.setValueAt(value, rowIndex, COL_VALUE);
    }
    
    /**
     * Sets the value of the indicated row for the local statistical
     * table.
     * @param rowIndex - The row.
     * @param value - The new value.
     */
    protected void setLocalRowValue(int rowIndex, String value) {
        localModel.setValueAt(value, rowIndex, COL_VALUE);
    }

}