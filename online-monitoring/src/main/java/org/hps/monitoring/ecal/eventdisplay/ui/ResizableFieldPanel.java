package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class <code>ResizableFieldPanel</code> is an extension of <code>
 * JPanel</code> that contains a series of "fields." Each field is
 * composed of a header label that displays its name and a value
 * label that displays its value. Field names are constant once set
 * but values may be changed.<br/>
 * <br/>
 * <code>ResizableFieldPanel</code> automatically handles the layout
 * of its fields, displaying the header label next to the value label
 * and inserting as many in a row as possible before wrapping to the
 * next row. <code>ResizableFieldPanel</code> ensures that each field
 * in a column is the same size as the other fields in the same column
 * and that all fields in the same column are aligned. Individual
 * columns may be different widths.<br/>
 * <br/>
 * The preferred width of a column is set by the preferred size of the
 * largest field in that column or a minimum row size, if it is set.
 * Fields that exceed the minimum row size are still granted their full
 * preferred size.
 * 
 * @see JPanel
 */
public class ResizableFieldPanel extends JPanel {
    // Local variables.
    private static final long serialVersionUID = 1L;
    private Font boldFont = getFont().deriveFont(Font.BOLD);
    private static final int horizontal = 10;
    private static final int vertical = 10;
    private Dimension oldSize = new Dimension(0, 0);
    private int oldFieldCount = 0;
    private int minDisplayWidth = 0;
    private Dimension userPreferred = null;
    private Dimension actualPreferred = new Dimension(0, 0);
    
    // Constituent components.
    private List<JLabel> headerList = new ArrayList<JLabel>();
    private List<JLabel> displayList = new ArrayList<JLabel>();
    
    // Class variables.
    /** The display text for a field with no value. */
    static final String NULL_VALUE = "---";
    
    /**
     * Instantiates a new <code>ResizableFieldPanel</code> with no
     * minimum column width.
     */
    public ResizableFieldPanel() { this(0); }
    
    /**
     * Instantiates a new <code>ResizableFieldPanel</code> with the
     * indicated minimum column width.
     * @param minWidth - The minimum column width.
     */
    public ResizableFieldPanel(int minWidth) {
        // Component handles constituent component placement manually.
        super.setLayout(null);
        
        // Set the minimum component width.
        minDisplayWidth = minWidth >= 0 ? minWidth : 0;
        
        // Reset the constituent component placement whenever the base
        // component changes size.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Issue an order to reset the component layout.
                resetLayout();
            }
        });
    }
    
    /**
     * Adds a new field to the panel.
     * @param fieldName - The name of the field.
     * @throws NullPointerException Occurs if the field name is <code>
     * null</code>.
     */
    public void addField(String fieldName) throws NullPointerException {
        addField(fieldName, null);
    }
    
    /**
     * Adds a new field to the panel.
     * @param fieldName - The name of the field.
     * @param fieldValue - The initial value of the field.
     * @throws NullPointerException Occurs if the field name is <code>
     * null</code>.
     */
    public void addField(String fieldName, String fieldValue) throws NullPointerException {
        // Require that the field have a non-null name.
        if(fieldName == null) { throw new NullPointerException("Field names can not be null."); }
        
        // Null values default the default value.
        if(fieldValue == null) { fieldValue = NULL_VALUE; }
        
        // Create new labels to populate the component.
        JLabel header = new JLabel(fieldName + ":");
        header.setFont(boldFont);
        header.setHorizontalAlignment(JLabel.RIGHT);
        JLabel display = new JLabel(fieldValue);
        display.setFont(getFont());
        
        // Add the labels to the list.
        headerList.add(header);
        displayList.add(display);
        
        // Add the components to the base panel.
        add(header);
        add(display);
        
        // Reset the label layout.
        resetLayout();
    }
    
    /**
     * Sets the values of all fields to <code>NULL_VALUE</code>.
     */
    public void clearFields() {
        for(JLabel display : displayList) {
            display.setText(NULL_VALUE);
        }
    }
    
    /**
     * Gets the number of fields contained in the component.
     * @return Returns the number of fields as an <code>int</code>
     * primitive.
     */
    public int getFieldCount() { return headerList.size(); }
    
    /**
     * Generates a map that links field name to field index.
     * @return Returns a <code>Map</code> object linking the <code>
     * String</code> field name to the <code>int</code> field index.
     */
    public Map<String, Integer> getFieldNameIndexMap() {
        // Get the number of fields.
        int fields = headerList.size();
        
        // Create the map.
        Map<String, Integer> fieldMap = new HashMap<String, Integer>(fields);
        
        // Populate the map.
        for(int index = 0; index < fields; index++) {
            // Get the header name and remove the colon at the end.
            String header = headerList.get(index).getText();
            header = header.substring(0, header.length() - 1);
            
            // Add it to the map.
            fieldMap.put(header, new Integer(index));
        }
        
        // Return the map.
        return fieldMap;
    }
    
    /**
     * Gets the list of all field names in order of field index.
     * @return Returns the set of field names as an array of <code>
     * String</code> objects.
     */
    public String[] getFieldNames() {
        // Create an array for the field names.
        String[] fieldNames = new String[headerList.size()];
        
        // Populate the array.
        for(int index = 0; index < headerList.size(); index++) {
            String fieldName = headerList.get(index).getText();
            fieldNames[index] = fieldName.substring(0, fieldName.length() - 2);
        }
        
        // Return the result.
        return fieldNames;
    }
    
    /**
     * Gets the value for the field at the indicated index.
     * @param fieldIndex - The index of the field.
     * @return Returns the field value as a <code>String</code> object.
     * @throws IndexOutOfBoundsException Occurs if an invalid field index
     * is given.
     */
    public String getFieldValue(int fieldIndex) throws IndexOutOfBoundsException {
        // Validate the index.
        validateFieldIndex(fieldIndex);
        
        // Return the value of the requested field.
        return displayList.get(fieldIndex).getText();
    }
    
    @Override
    public Dimension getPreferredSize() {
        if(userPreferred == null) { return actualPreferred; }
        else { return userPreferred; }
    }
    
    /**
     * Inserts a field at the indicated index, if possible.
     * @param index - The index at which to insert the field.
     * @param fieldName - The name of the field.
     * @throws IndexOutOfBoundsException Occurs if the insertion index
     * is not a valid index within the component.
     * @throws NullPointerException Occurs if the field name is <code>
     * null</code>.
     */
    public void insertField(int index, String fieldName) throws IndexOutOfBoundsException, NullPointerException {
        insertField(index, fieldName, "");
    }
    
    /**
     * Inserts a field at the indicated index, if possible.
     * @param index - The index at which to insert the field.
     * @param fieldName - The name of the field.
     * @param fieldValue - The initial value of the field.
     * @throws IndexOutOfBoundsException Occurs if the insertion index
     * is not a valid index within the component.
     * @throws NullPointerException Occurs if the field name is <code>
     * null</code>.
     */
    public void insertField(int index, String fieldName, String fieldValue) throws IndexOutOfBoundsException, NullPointerException {
        // Require that the field have a non-null name.
        if(fieldName == null) { throw new NullPointerException("Field names can not be null."); }
        
        // Null values default the default value.
        if(fieldValue == null) { fieldValue = NULL_VALUE; }
        
        // Create new labels to populate the component.
        JLabel header = new JLabel(fieldName + ":");
        header.setFont(boldFont);
        header.setHorizontalAlignment(JLabel.RIGHT);
        JLabel display = new JLabel(fieldValue);
        display.setFont(getFont());
        
        // Add the labels to the list.
        headerList.add(index, header);
        displayList.add(index, display);
        
        // Add the components to the base panel.
        add(header);
        add(display);
        
        // Reset the label layout.
        resetLayout();
    }
    
    /**
     * Removes the field at the indicated index.
     * @param fieldIndex - The index of the field.
     * @throws IndexOutOfBoundsException Occurs if an invalid field index
     * is given.
     */
    public void removeField(int fieldIndex) throws IndexOutOfBoundsException {
        // Validate the index.
        validateFieldIndex(fieldIndex);
        
        // Remove the requested field.
        remove(headerList.get(fieldIndex));
        remove(displayList.get(fieldIndex));
        headerList.remove(fieldIndex);
        displayList.remove(fieldIndex);
        
        // Reset the layout.
        resetLayout();
    }
    
    /**
     * Sets the value of the field at the indicated index.
     * @param fieldIndex - The index of the field.
     * @param fieldValue - The new value of the field.
     * @throws IndexOutOfBoundsException Occurs if an invalid field index
     * is given.
     */
    public void setFieldValue(int fieldIndex, String fieldValue) throws IndexOutOfBoundsException {
        // Validate the index.
        validateFieldIndex(fieldIndex);
        
        // Set the value of the requested field.
        displayList.get(fieldIndex).setText(fieldValue);
    }
    
    @Override
    public void setFont(Font font) {
        // If the superclass font is null, the component is still
        // being initialized and needs to just run the superclass
        // method.
        if(getFont() == null) { super.setFont(font); }
        
        // Otherwise, set any constituent components to the correct
        // font as well.
        else {
            // If the font is being set to null, use the default
            // system font.
            if(font == null) { font = new Font(Font.DIALOG, Font.PLAIN, 11); }
            
            // Set the base panel's font to the indicated value.
            super.setFont(font);
            
            // Create a new bold font.
            boldFont = super.getFont().deriveFont(Font.BOLD);
            
            // Set each display label to the indicated font.
            for(JLabel display : displayList) { display.setFont(font); }
            
            // Set each header label to the bold font.
            for(JLabel header : headerList) { header.setFont(boldFont); }
        }
    }
    
    @Override
    public void setLayout(LayoutManager mgr) {
        // ResizableDisplayManager handles its own layout, so do nothing.
        return;
    }
    
    @Override
    public void setPreferredSize(Dimension preferredSize) {
        userPreferred = preferredSize;
    }
    
    /**
     * Repositions the fields to the appropriate positions given the
     * current width of the component.
     */
    private void resetLayout() {
        // If the neither the size of the component nor the number of
        // fields have changed, there is nothing to do here.
        if(headerList.size() == oldFieldCount && oldSize.width == getSize().width
                && oldSize.height == getSize().height) {
            return;
        }
        
        // Set the last width and field counts.
        oldSize = getSize();
        oldFieldCount = headerList.size();
        
        // Otherwise, determine the ideal maximum number of fields that
        // can be displayed in a single row.
        int availableWidth = getWidth() - horizontal;
        int max = 0;
        idealLoop:
        for(int index = 0; index < headerList.size(); index++) {
            // Subtract the necessary width for the header label.
            availableWidth -= headerList.get(index).getPreferredSize().width;
            availableWidth -= horizontal;
            
            // Subtract the required width for the display label.
            int displayWidth = displayList.get(index).getPreferredSize().width;
            if(displayWidth < minDisplayWidth) { displayWidth = minDisplayWidth; }
            availableWidth -= displayWidth;
            availableWidth -= horizontal;
            
            // Increment the maximum number of displayable headers.
            max++;
            
            // If the available width is depleted, exit the loop.
            if(availableWidth <= 0) { break idealLoop; }
        }
        
        // If the maximum displayable number is either 1 or equal to
        // actual number, the fields should be displayed with their
        // preferred widths.
        if(max == 1 || max == headerList.size()) {
            // Track the current position on the component.
            int curX = horizontal;
            int curY = vertical;
            
            // Add each field.
            for(int index = 0; index < headerList.size(); index++) {
                // Get the current header and display label.
                JLabel header = headerList.get(index);
                JLabel display = displayList.get(index);
                
                // Set the label sizes to the preferred size.
                header.setSize(header.getPreferredSize());
                display.setSize(display.getPreferredSize());
                
                // Set the label locations to the current correct place.
                header.setLocation(curX, curY);
                header.setSize(header.getPreferredSize());
                curX += header.getSize().width + horizontal;
                display.setLocation(curX, curY);
                display.setSize(header.getPreferredSize());
                curX += display.getWidth() + horizontal;
            }
            
            // The process is finished, so return.
            return;
        }
        
        // Otherwise, the labels in subsequent rows must be analyzed
        // to determine what the actual maximum number of labels per
        // row can be used. Try the ideal maximum first and work down
        // from there to the minimum of one per row.
        else {
            // Track the number of columns needed and the proper widths
            // of each column.
            int columns = 1;
            int[] columnWidth = { 0 };
            actualLoop:
            for(int actual = max; actual > 0; actual--) {
                // Store the actual necessary width for each component in
                // each column for the current row size.
                int[] actualColWidth = new int[actual + actual];
                
                // Track the current column.
                int curCol = 0;
                
                // Iterate over the labels and find the largest necessary
                // width for each column.
                for(int index = 0; index < headerList.size(); index++) {
                    // Get the widths needed for the current header and
                    // display labels.
                    int headerWidth = headerList.get(index).getPreferredSize().width;
                    int displayWidth = displayList.get(index).getPreferredSize().width;
                    if(displayWidth < minDisplayWidth) { displayWidth = minDisplayWidth; }
                    
                    // If the needed widths exceed the current maximum, they
                    // should replace it.
                    if(actualColWidth[2 * curCol] < headerWidth) { actualColWidth[2 * curCol] = headerWidth; }
                    if(actualColWidth[(2 * curCol) + 1] < displayWidth) { actualColWidth[(2 * curCol) + 1] = displayWidth; }
                    
                    // Increment the current column.
                    curCol++;
                    if(curCol == actual) { curCol = 0; }
                }
                
                // Check if the actual needed width is less than the width
                // available for the labels.
                availableWidth = getWidth() - horizontal;
                for(int width : actualColWidth) {
                    availableWidth -= width;
                    availableWidth -= horizontal;
                }
                
                // Store the results for this number of columns.
                columns = actual;
                columnWidth = actualColWidth;
                
                // If the result is either zero of positive, then there is
                // enough space available and the current number of columns
                // may be employed. Otherwise, continue to the next smaller
                // number of columns.
                if(availableWidth >= 0) { break actualLoop; }
            }
            
            // The necessary width for each column and the number of columns
            // should now be calculated. Set the constituent component
            // positions and sizes to match it.
            int curX = horizontal;
            int curY = vertical;
            int curCol = 0;
            for(int index = 0; index < headerList.size(); index++) {
                // Get the current header and display label.
                JLabel header = headerList.get(index);
                JLabel display = displayList.get(index);
                
                // Set the label sizes to the preferred size.
                header.setSize(header.getPreferredSize());
                display.setSize(display.getPreferredSize());
                
                // Set the label locations to the current correct place.
                header.setLocation(curX, curY);
                header.setSize(columnWidth[2 * curCol], header.getPreferredSize().height);
                curX += header.getSize().width + horizontal;
                display.setLocation(curX, curY);
                display.setSize(columnWidth[(2 * curCol) + 1], display.getPreferredSize().height);
                curX += display.getWidth() + horizontal;
                
                // Increment the current column index.
                curCol++;
                if(curCol == columns) {
                    curCol = 0;
                    curY += header.getPreferredSize().height + vertical;
                    curX = horizontal;
                }
            }
        }
        
        // Update the preferred size such that the preferred height will
        // encompass all of the rows.
        JLabel lastHeader = headerList.get(headerList.size() - 1);
        int preferredHeight = lastHeader.getY() + lastHeader.getHeight() + vertical;
        actualPreferred = new Dimension(getSize().width, preferredHeight);
    }
    
    /**
     * Throws an <code>IndexOutOfBoundsException</code> if the argument
     * index is not a valid field index. This is used to validate the
     * index given in methods requiring one.
     * @param index - The index to validate.
     * @throws IndexOutOfBoundsException Occurs if the index fails
     * to validate.
     */
    private void validateFieldIndex(int index) throws IndexOutOfBoundsException {
        if(index < 0 || index >= headerList.size()) {
            throw new IndexOutOfBoundsException(String.format("Index %d is not a valid field index.", index));
        }
    }
}
