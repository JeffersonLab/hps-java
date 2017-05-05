package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import org.hps.monitoring.ecal.eventdisplay.util.CrystalDataSet;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalDataSet.Motherboard;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalDataSet.Preamplifier;
import org.hps.monitoring.ecal.eventdisplay.util.EcalWiringManager;

/**
 * Class <code>CrystalFilterPanel</code> is an extension of <code>
 * JPanel</code> that allows users to select filters for crystals from
 * the properties stored in a <code>CrystalDataSet</code> object and
 * filter all the crystals present in an <code>EcalWiringManager</code>
 * object by said properties.<br/>
 * <br/>
 * <code>CrystalFilterPanel</code> alerts other classes that a filter
 * has been applied through <code>ActionListener</code> objects.
 * 
 * @see JPanel
 * @see CrystalDataSet
 * @see EcalWiringManager
 */
public final class CrystalFilterPanel extends JPanel {
    // Internal variables.
    private Dimension preferredSize = null;
    private final EcalWiringManager manager;
    private List<Point> filteredList = null;
    private static final long serialVersionUID = 1L;
    private CDSComparator cdsCompare = new CDSComparator();
    private List<ActionListener> alList = new ArrayList<ActionListener>();
    public static final int EVENT_FILTER_APPLIED = ActionEvent.RESERVED_ID_MAX + 10;
    public static final int EVENT_FILTER_REMOVED = ActionEvent.RESERVED_ID_MAX + 11;
    
    // Component variables.
    private JCheckBox[] checkActive;
    private JComboBox<?>[] comboField;
    private JButton buttonApply;
    private JButton buttonRemove;
    private JButton buttonClear;
    private JTextArea textStatus;
    
    // Component index references and related statics.
    private static final int INDEX_X = 0;
    private static final int INDEX_Y = 1;
    private static final int INDEX_APD = 2;
    private static final int INDEX_PREAMP = 3;
    private static final int INDEX_LED_CHANNEL = 4;
    private static final int INDEX_LED_DRIVER = 5;
    private static final int INDEX_FADC_SLOT = 6;
    private static final int INDEX_FADC_CHANNEL = 7;
    private static final int INDEX_SPLITTER = 8;
    private static final int INDEX_HV_GROUP = 9;
    private static final int INDEX_JOUT = 10;
    private static final int INDEX_MOTHERBOARD = 11;
    private static final int INDEX_CHANNEL = 12;
    private static final int INDEX_GAIN = 13;
    private static final int INDICES = 14;
    private static final Color COLOR_CHECK_FONT_ACTIVE = Color.BLACK;
    private static final Color COLOR_CHECK_FONT_INACTIVE = Color.GRAY;
    private static final String[] FIELD_NAME = { "x-Index", "y-Index", "APD Number",
        "Preamp Number", "LED Channel", "LED Driver", "FADC Slot", "FADC Channel",
        "Splitter Number", "H.V. Group", "Jout", "Motherboard", "Channel", "Gain" };
    
    // Spacing constants.
    private static final int hexternal = 15;
    private static final int vexternal = 15;
    private static final int hinternal = 10;
    private static final int vinternal = 5;
    
    public CrystalFilterPanel(EcalWiringManager dataManager) {
        // Store the manager.
        manager = dataManager;
        
        // Create the panel layout.
        SpringLayout layout = new SpringLayout();
        setLayout(layout);
        
        // Instantiate the check boxes.
        JCheckBox largestCheck = null;
        checkActive = new JCheckBox[INDICES];
        for(int index = 0; index < INDICES; index++) {
            // Instantiate the check box.
            checkActive[index] = new JCheckBox(FIELD_NAME[index]);
            checkActive[index].setSelected(false);
            checkActive[index].setForeground(COLOR_CHECK_FONT_INACTIVE);
            checkActive[index].addItemListener(new CheckListener(index));
            add(checkActive[index]);
            
            // Get the largest check box.
            if(largestCheck == null) { largestCheck = checkActive[index]; }
            else {
                if(largestCheck.getPreferredSize().width < checkActive[index].getPreferredSize().width) {
                    largestCheck = checkActive[index];
                }
            }
        }
        
        // Create sets to store all possible entries for each crystal
        // data field.
        Set<Integer>      xIndexSet      = new HashSet<Integer>();
        Set<Integer>      yIndexSet      = new HashSet<Integer>();
        Set<Integer>      apdSet         = new HashSet<Integer>();
        Set<Preamplifier> preampSet      = new HashSet<Preamplifier>();
        Set<Integer>      ledChannelSet  = new HashSet<Integer>();
        Set<Double>       ledDriverSet   = new HashSet<Double>();
        Set<Integer>      fadcSlotSet    = new HashSet<Integer>();
        Set<Integer>      fadcChannelSet = new HashSet<Integer>();
        Set<Integer>      splitterSet    = new HashSet<Integer>();
        Set<Integer>      hvGroupSet     = new HashSet<Integer>();
        Set<Integer>      joutSet        = new HashSet<Integer>();
        Set<Integer>      channelSet     = new HashSet<Integer>();
        Set<Integer>      gainSet        = new HashSet<Integer>();
        
        // Iterate over all crystal data fields to obtain the set of
        // all possible entries.
        for(CrystalDataSet cds : manager) {
            xIndexSet.add(cds.getCrystalXIndex());
            yIndexSet.add(cds.getCrystalYIndex());
            apdSet.add(cds.getAPDNumber());
            preampSet.add(cds.getPreamplifierNumber());
            ledChannelSet.add(cds.getLEDChannel());
            ledDriverSet.add(cds.getLEDDriver());
            fadcSlotSet.add(cds.getFADCSlot());
            fadcChannelSet.add(cds.getFADCChannel());
            splitterSet.add(cds.getSplitterNumber());
            hvGroupSet.add(cds.getHighVoltageGroup());
            joutSet.add(cds.getJout());
            channelSet.add(cds.getChannel());
            gainSet.add(cds.getGain());
        }
        
        // Dump all of the set data into a list for each field.
        Integer[]      xIndex      = xIndexSet.toArray(new Integer[xIndexSet.size()]);
        Integer[]      yIndex      = yIndexSet.toArray(new Integer[yIndexSet.size()]);
        Integer[]      apd         = apdSet.toArray(new Integer[apdSet.size()]);
        Preamplifier[] preamp      = preampSet.toArray(new Preamplifier[preampSet.size()]);
        Integer[]      ledChannel  = ledChannelSet.toArray(new Integer[ledChannelSet.size()]);
        Double[]       ledDriver   = ledDriverSet.toArray(new Double[ledDriverSet.size()]);
        Integer[]      fadcSlot    = fadcSlotSet.toArray(new Integer[fadcSlotSet.size()]);
        Integer[]      fadcChannel = fadcChannelSet.toArray(new Integer[fadcChannelSet.size()]);
        Integer[]      splitter    = splitterSet.toArray(new Integer[splitterSet.size()]);
        Integer[]      hvGroup     = hvGroupSet.toArray(new Integer[hvGroupSet.size()]);
        Integer[]      jout        = joutSet.toArray(new Integer[joutSet.size()]);
        Integer[]      channel     = channelSet.toArray(new Integer[channelSet.size()]);
        Integer[]      gain        = gainSet.toArray(new Integer[gainSet.size()]);
        
        // Generate the list of entries for the motherboard spinner.
        //String[] motherboard = { "Top", "Bottom", "Left", "Right", "Top-Left", "Top-Right",
        //        "Bottom-Left", "Bottom-Right" };
        
        // Sort all the lists in their natural order.
        Arrays.sort(xIndex);
        Arrays.sort(yIndex);
        Arrays.sort(apd);
        Arrays.sort(preamp);
        Arrays.sort(ledChannel);
        Arrays.sort(ledDriver);
        Arrays.sort(fadcSlot);
        Arrays.sort(fadcChannel);
        Arrays.sort(splitter);
        Arrays.sort(hvGroup);
        Arrays.sort(jout);
        //Arrays.sort(motherboard);
        Arrays.sort(channel);
        Arrays.sort(gain);
        
        // Instantiate each of the field combo boxes.
        comboField = new JComboBox[INDICES];
        comboField[INDEX_X]            = new JComboBox<Integer>(xIndex);
        comboField[INDEX_Y]            = new JComboBox<Integer>(yIndex);
        comboField[INDEX_APD]          = new JComboBox<Integer>(apd);
        comboField[INDEX_PREAMP]       = new JComboBox<Preamplifier>(preamp);
        comboField[INDEX_LED_CHANNEL]  = new JComboBox<Integer>(ledChannel);
        comboField[INDEX_LED_DRIVER]   = new JComboBox<Double>(ledDriver);
        comboField[INDEX_FADC_SLOT]    = new JComboBox<Integer>(fadcSlot);
        comboField[INDEX_FADC_CHANNEL] = new JComboBox<Integer>(fadcChannel);
        comboField[INDEX_SPLITTER]     = new JComboBox<Integer>(splitter);
        comboField[INDEX_HV_GROUP]     = new JComboBox<Integer>(hvGroup);
        comboField[INDEX_JOUT]         = new JComboBox<Integer>(jout);
        comboField[INDEX_MOTHERBOARD]  = new JComboBox<Position>(Position.values());
        comboField[INDEX_CHANNEL]      = new JComboBox<Integer>(channel);
        comboField[INDEX_GAIN]         = new JComboBox<Integer>(gain);
        
        // Set the properties of the combo boxes.
        for(JComboBox<?> combo : comboField) {
            combo.setEnabled(false);
            combo.setEditable(false);
            add(combo);
        }
        
        // Instantiate and set the properties of the apply button.
        buttonApply = new JButton("Apply Filter");
        buttonApply.setToolTipText("Highlight all crystals that match the selected filter.");
        buttonApply.setEnabled(false);
        add(buttonApply);
        buttonApply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Apply the active filter.
                applyFilter();
                
                // Enable the remove filter button.
                buttonRemove.setEnabled(true);
            }
        });
        
        // Instantiate and set the properties of the remove button.
        buttonRemove = new JButton("Remove Filter");
        buttonRemove.setToolTipText("Clear all filter-related crystal highlighting.");
        buttonRemove.setEnabled(false);
        add(buttonRemove);
        buttonRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Remove the list of filtered crystals.
                removeFilter();
                
                // Disable the remove filter button.
                buttonRemove.setEnabled(false);
            }
        });
        
        // Instantiate and set the properties of the remove button.
        buttonClear = new JButton("Clear Filters");
        buttonClear.setToolTipText("Reset all filters to the default and disable them.");
        add(buttonClear);
        buttonClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Remove the list of filtered crystals.
                removeFilter();
                
                // Disable the remove filter button.
                buttonRemove.setEnabled(false);
                
                // Disable all of the filter check boxes.
                for(JCheckBox check : checkActive) {
                    check.setSelected(false);
                }
                
                // Set the selected filter value for each filter to
                // the original value.
                for(JComboBox<?> combo : comboField) {
                    combo.setSelectedIndex(0);
                }
            }
        });
        
        // Instantiate the filter status text area.
        textStatus = new JTextArea();
        textStatus.setEditable(false);
        textStatus.setText("Active Filters: None");
        textStatus.setFont(new Font(Font.MONOSPACED, getFont().getStyle(), getFont().getSize()));
        JScrollPane textScroller = new JScrollPane(textStatus);
        add(textScroller);
        
        // Determine if the check boxes or the spinners have a larger
        // preferred height.
        boolean checkLarger = true;
        if(checkActive[INDEX_X].getPreferredSize().height < comboField[INDEX_X].getPreferredSize().height) {
            checkLarger = false;
        }
        
        // Place the filter components onto the layout.
        for(int index = 0; index < INDICES; index++) {
            // The first component needs to use the top of the panel
            // for its upper border.
            if(index == 0) {
                layout.putConstraint(SpringLayout.NORTH, checkActive[index], vexternal, SpringLayout.NORTH, this);
                layout.putConstraint(SpringLayout.NORTH, comboField[index], vexternal, SpringLayout.NORTH, this);
            }
            // Subsequent panels are locked to the previous entry's
            // lower border.
            else {
                if(checkLarger) {
                    layout.putConstraint(SpringLayout.NORTH, checkActive[index], vinternal, SpringLayout.SOUTH, checkActive[index - 1]);
                    layout.putConstraint(SpringLayout.NORTH, comboField[index], vinternal, SpringLayout.SOUTH, checkActive[index - 1]);
                }
                else {
                    layout.putConstraint(SpringLayout.NORTH, checkActive[index], vinternal, SpringLayout.SOUTH, comboField[index - 1]);
                    layout.putConstraint(SpringLayout.NORTH, comboField[index], vinternal, SpringLayout.SOUTH, comboField[index - 1]);
                }
            }
            
            // All check boxes have left borders locked to the left side
            // of the panel. All spinners have left borders locked to
            // the right border of the widest check box. Finally, all of
            // the spinners have right borders locked the right side of
            // the panel.
            layout.putConstraint(SpringLayout.WEST, checkActive[index], hexternal, SpringLayout.WEST, this);
            layout.putConstraint(SpringLayout.WEST, comboField[index], hinternal, SpringLayout.EAST, largestCheck);
            layout.putConstraint(SpringLayout.EAST, comboField[index], -hexternal, SpringLayout.EAST, this);
        }
        
        // Place the status text area.
        JComponent previous = comboField[INDICES - 1];
        if(checkLarger) { previous = checkActive[INDICES - 1]; }
        layout.putConstraint(SpringLayout.NORTH, textScroller, vexternal, SpringLayout.SOUTH, previous);
        layout.putConstraint(SpringLayout.WEST, textScroller, hexternal, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, textScroller, -hexternal, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.SOUTH, textScroller, -hexternal, SpringLayout.NORTH, buttonApply);
        
        // Place the command buttons onto the layout.
        layout.putConstraint(SpringLayout.SOUTH, buttonApply, -vexternal, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.WEST, buttonApply, hexternal, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.SOUTH, buttonRemove, -vexternal, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.WEST, buttonRemove, hinternal, SpringLayout.EAST, buttonApply);
        layout.putConstraint(SpringLayout.SOUTH, buttonClear, -vexternal, SpringLayout.SOUTH, this);
        layout.putConstraint(SpringLayout.WEST, buttonClear, hinternal, SpringLayout.EAST, buttonRemove);
    }
    
    /**
     * Adds an <code>ActionListener</code> object to this component.
     * @param listener - The listener to add.
     */
    public void addActionListener(ActionListener listener) {
        if(listener != null) { alList.add(listener); }
    }
    
    /**
     * Gets the list of <code>ActionListener</code> objects attached to
     * this component.
     * @return Returns the listeners as an array of <code>ActionListener
     * </code> objects.
     */
    public ActionListener[] getActionListeners() {
        return alList.toArray(new ActionListener[alList.size()]);
    }
    
    /**
     * Gets the list of crystals that pass the applied filter.
     * @return Returns a <code>List</code> collection of <code>Point
     * </code> objects representing the LCSim coordinates of every
     * crystal that passes the applied filter. If no filter is active,
     * returns <code>null</code>.
     */
    public List<Point> getFilteredCrystals() { return filteredList; }
    
    @Override
    public Dimension getPreferredSize() {
        // If there is a user defined preferred size, use that.
        if(preferredSize != null) { return preferredSize; }
        
        // Track the minimum size needed to display the panel.
        int preferredHeight =  4 * vexternal;
        
        // Determine if the combo boxes or check boxes are larger.
        int checkHeight = checkActive[INDEX_X].getPreferredSize().height;
        int comboHeight = comboField[INDEX_X].getPreferredSize().height;
        boolean checkLarger = checkHeight > comboHeight;
        int compHeight = checkLarger ? checkHeight : comboHeight;
        
        // Get the preferred height of the filters.
        preferredHeight += (INDICES - 1) * (vinternal + compHeight);
        
        // Add the button preferred height.
        preferredHeight += buttonApply.getPreferredSize().height;
        
        // Add 50 for the text area.
        preferredHeight += 150;
        
        // The preferred width is the width needed by the buttons.
        int preferredWidth = hexternal + hexternal + hinternal + hinternal;
        preferredWidth += buttonApply.getPreferredSize().width;
        preferredWidth += buttonRemove.getPreferredSize().width;
        preferredWidth += buttonClear.getPreferredSize().width;
        
        // Return the result.
        return new Dimension(preferredWidth, preferredHeight);
    }
    
    /**
     * Indicates whether or not a filter is currently applied.
     * @return Returns <code>true</code> if there is an active filter
     * and <code>false</code> if there is not.
     */
    public boolean isActive() { return filteredList != null; }
    
    /**
     * Removes the indicated <code>ActionListener</code> object from
     * this component if it exists.
     * @param listener - The listener to remove.
     */
    public void removeActionListener(ActionListener listener) {
        alList.remove(listener);
    }
    
    @Override
    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }
    
    /**
     * Applies the filter defined by the currently selected filter
     * options. If no filter options are selected, this does nothing.
     */
    private void applyFilter() {
        // Make sure that at least one filter option is selected.
        boolean isEnabled = false;
        activeLoop:
        for(JCheckBox check : checkActive) {
            if(check.isEnabled()) {
                isEnabled = true;
                break activeLoop;
            }
        }
        if(!isEnabled) { return; }
        
        // Clear the filter parameters.
        cdsCompare.clearFilters();
        
        // Store the textual form of the active filters.
        StringBuffer filterBuffer = new StringBuffer("Active Filters:\n");
        
        // Set the filter parameters.
        if(checkActive[INDEX_X].isSelected()) {
            int val = (Integer) comboField[INDEX_X].getSelectedItem();
            cdsCompare.addXIndexFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "x-Index", val));
        }
        if(checkActive[INDEX_Y].isSelected()) {
            int val = (Integer) comboField[INDEX_Y].getSelectedItem();
            cdsCompare.addYIndexFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "y-Index", val));
        }
        if(checkActive[INDEX_APD].isSelected()) {
            int val = (Integer) comboField[INDEX_APD].getSelectedItem();
            cdsCompare.addAPDFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "APD Number", val));
        }
        if(checkActive[INDEX_PREAMP].isSelected()) {
            Preamplifier val = (Preamplifier) comboField[INDEX_PREAMP].getSelectedItem();
            cdsCompare.addPreampFilter((Preamplifier) comboField[INDEX_PREAMP].getSelectedItem());
            filterBuffer.append(String.format("    %-20s :: %s%n", "APD Number", val.toString()));
        }
        if(checkActive[INDEX_LED_CHANNEL].isSelected()) {
            int val = (Integer) comboField[INDEX_LED_CHANNEL].getSelectedItem();
            cdsCompare.addLEDChannelFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "LED Channel Number", val));
        }
        if(checkActive[INDEX_LED_DRIVER].isSelected()) {
            double val = (Double) comboField[INDEX_LED_DRIVER].getSelectedItem();
            cdsCompare.addLEDDriverFilter(val);
            filterBuffer.append(String.format("    %-20s :: %.1f%n", "LED Driver", val));
        }
        if(checkActive[INDEX_FADC_SLOT].isSelected()) {
            int val = (Integer) comboField[INDEX_FADC_SLOT].getSelectedItem();
            cdsCompare.addFADCSlotFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "FADC Slot Number", val));
        }
        if(checkActive[INDEX_FADC_CHANNEL].isSelected()) {
            int val = (Integer) comboField[INDEX_FADC_CHANNEL].getSelectedItem();
            cdsCompare.addFADCChannelFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "FADC Channel Number", val));
        }
        if(checkActive[INDEX_SPLITTER].isSelected()) {
            int val = (Integer) comboField[INDEX_SPLITTER].getSelectedItem();
            cdsCompare.addSplitterFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "Splitter Number", val));
        }
        if(checkActive[INDEX_HV_GROUP].isSelected()) {
            int val = (Integer) comboField[INDEX_HV_GROUP].getSelectedItem();
            cdsCompare.addHVGroupFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "High Voltage Group", val));
        }
        if(checkActive[INDEX_JOUT].isSelected()) {
            int val = (Integer) comboField[INDEX_JOUT].getSelectedItem();
            cdsCompare.addJoutFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "Jout", val));
        }
        if(checkActive[INDEX_MOTHERBOARD].isSelected()) {
            Position val = (Position) comboField[INDEX_MOTHERBOARD].getSelectedItem();
            cdsCompare.addMotherboardFilter((Position) comboField[INDEX_MOTHERBOARD].getSelectedItem());
            filterBuffer.append(String.format("    %-20s :: %s%n", "Motherboard Position", val.toString()));
        }
        if(checkActive[INDEX_CHANNEL].isSelected()) {
            int val = (Integer) comboField[INDEX_CHANNEL].getSelectedItem();
            cdsCompare.addChannelFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "Channel Number", val));
        }
        if(checkActive[INDEX_GAIN].isSelected()) {
            int val = (Integer) comboField[INDEX_GAIN].getSelectedItem();
            cdsCompare.addGainFilter(val);
            filterBuffer.append(String.format("    %-20s :: %d%n", "Gain", val));
        }
        
        // Update the status pane.
        textStatus.setText(filterBuffer.toString());
        
        // Populate the filtered crystals list.
        filteredList = new ArrayList<Point>();
        for(CrystalDataSet cds : manager) {
            if(cdsCompare.passesFilter(cds)) { filteredList.add(cds.getCrystalIndex()); }
        }
        
        // Throw an event to indicate that a filter was applied.
        ActionEvent event = new ActionEvent(this, EVENT_FILTER_APPLIED, "Filter Applied");
        for(ActionListener lst : alList) {
            lst.actionPerformed(event);
        }
    }
    
    /**
     * Removes any active filters.
     */
    private void removeFilter() {
        // Make sure that there is an active filter to remove.
        if(filteredList == null) { return; }
        
        // Clear the filter list.
        filteredList = null;
        
        // Update the status panel text.
        textStatus.setText("Active Filters: None");
        
        // Throw an event to indicate that a filter was removed.
        ActionEvent event = new ActionEvent(this, EVENT_FILTER_REMOVED, "Filter Removed");
        for(ActionListener lst : alList) {
            lst.actionPerformed(event);
        }
    }
    
    /**
     * Enumerable <code>Position</code> specifies a location on the
     * calorimeter.
     * 
     */
    private enum Position {
        TOP(true, null), BOTTOM(false, null), LEFT(null, true), RIGHT(null, false),
        TOP_LEFT(true, true), TOP_RIGHT(true, false), BOTTOM_LEFT(false, true), BOTTOM_RIGHT(false, false);
        
        // Internal variables.
        private String name = null;
        private final Boolean isTop;
        private final Boolean isLeft;
        
        /**
         * Instantiates a new position.
         * @param isTop - <code>true</code> if the position is on the
         * top and <code>false</code> if it is on the bottom. <code>
         * null</code> indicates that it does not differentiate between
         * this axis.
         * @param isLeft - <code>true</code> if the position is on the
         * left and <code>false</code> if it is on the right. <code>
         * null</code> indicates that it does not differentiate between
         * this axis.
         */
        private Position(Boolean isTop, Boolean isLeft) {
            this.isTop = isTop;
            this.isLeft = isLeft;
        }
        
        /**
         * Checks whether a given motherboard position is consistent
         * with the position enumerable.
         * @param mb - The motherboard to check.
         * @return Returns <code>true</code> if the motherboard position
         * is consistent and <code>false</code> otherwise.
         */
        public boolean matchesPosition(Motherboard mb) {
            // If this position needs to check top or bottom status,
            // do so. Return false if it fails.
            if(isTop != null && isTop != mb.isTop()) {
                return false;
            }
            
            // If this position needs to check left or right status,
            // do so. Return false if it fails.
            if(isLeft != null && isLeft != mb.isLeft()) {
                return false;
            }
            
            // If it passes all checks, return true.
            return true;
        }
        
        @Override
        public String toString() {
            // If the name has already been defined, return it.
            if(name != null) { return name; }
            
            // Otherwise, the name must be parsed from the system name.
            // Create a string buffer to contain the parsed characters.
            StringBuffer nameBuffer = new StringBuffer();
            
            // Iterate over the characters in the name and process them.
            boolean nextUpperCase = true;
            for(char c : name().toCharArray()) {
                if(c == '_') {
                    nameBuffer.append(' ');
                    nextUpperCase = true;
                }
                else if(nextUpperCase) {
                    nameBuffer.append(c);
                    nextUpperCase = false;
                }
                else { nameBuffer.append(Character.toLowerCase(c)); }
            }
            
            // Store the result.
            name = nameBuffer.toString();
            
            // Return the result.
            return name;
        }
    }
    
    /**
     * Class <code>CheckListener</code> is an implementation of <code>
     * ItemListener</code> that is linked to a specific component index
     * and handles the enabling and disabling of filters.
     * 
     * @see ItemListener
     */
    private class CheckListener implements ItemListener {
        // Store the index of the components associated with the listener.
        private final int index;
        
        /**
         * Instantiates a new <code>CheckListener</code> that is linked
         * to the specified index.
         * @param componentIndex - The index of the component.
         */
        public CheckListener(int componentIndex) { index = componentIndex; }
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            // Set the component properties according the selection
            // status of the check box.
            if(checkActive[index].isSelected()) {
                checkActive[index].setForeground(COLOR_CHECK_FONT_ACTIVE);
                comboField[index].setEnabled(true);
            }
            else {
                checkActive[index].setForeground(COLOR_CHECK_FONT_INACTIVE);
                comboField[index].setEnabled(false);
            }
            
            // If any of the filter check boxes are active, the active
            // filter button should be clickable.
            boolean active = false;
            applyCheckLoop:
            for(JCheckBox check : checkActive) {
                if(check.isSelected()) {
                    active = true;
                    break applyCheckLoop;
                }
            }
            
            // Set the apply button to the appropriate setting.
            buttonApply.setEnabled(active);
        }
    }
    
    /**
     * Class <code>CDSComparator</code> allows a set of filters for
     * each property in a <code>CrystalDataSet</code> to be defined.
     * It then will check to see if a given <code>CrystalDataSet</code>
     * object matches the <code>CDSComparator</code> properties.
     * 
     */
    private class CDSComparator {
        // Filters.
        private Integer filterX = null;
        private Integer filterY = null;
        private Integer filterAPD = null;
        private Preamplifier filterPreamp = null;
        private Integer filterLEDChannel = null;
        private Double filterLEDDriver = null;
        private Integer filterFADCSlot = null;
        private Integer filterFADCChannel = null;
        private Integer filterSplitter = null;
        private Integer filterHVGroup = null;
        private Integer filterJout = null;
        private Position motherboard = null;
        private Integer filterChannel = null;
        private Integer filterGain = null;
        
        /**
         * Enables and sets the value of the x-index filter.
         * @param xIndex - The filter value.
         */
        public void addXIndexFilter(int xIndex) { filterX = new Integer(xIndex); }
        
        /**
         * Enables and sets the value of the y-index filter.
         * @param yIndex - The filter value.
         */
        public void addYIndexFilter(int yIndex) { filterY = new Integer(yIndex); }
        
        /**
         * Enables and sets the value of the APD number filter.
         * @param apd - The filter value.
         */
        public void addAPDFilter(int apd) { filterAPD = new Integer(apd); }
        
        /**
         * Enables and sets the value of the preamplifier number filter.
         * @param preamp - The filter value.
         */
        public void addPreampFilter(Preamplifier preamp) { filterPreamp = preamp; }
        
        /**
         * Enables and sets the value of the LED channel number filter.
         * @param ledChannel - The filter value.
         */
        public void addLEDChannelFilter(int ledChannel) { filterLEDChannel = new Integer(ledChannel); }
        
        /**
         * Enables and sets the value of the LED driver filter.
         * @param ledDriver - The filter value.
         */
        public void addLEDDriverFilter(double ledDriver) { filterLEDDriver = new Double(ledDriver); }
        
        /**
         * Enables and sets the value of the FADC slot filter.
         * @param fadcSlot - The filter value.
         */
        public void addFADCSlotFilter(int fadcSlot) { filterFADCSlot = new Integer(fadcSlot); }
        
        /**
         * Enables and sets the value of the FADC channel filter.
         * @param fadcChannel - The filter value.
         */
        public void addFADCChannelFilter(int fadcChannel) { filterFADCChannel = new Integer(fadcChannel); }
        
        /**
         * Enables and sets the value of the splitter number filter.
         * @param splitterNum - The filter value.
         */
        public void addSplitterFilter(int splitterNum) { filterSplitter = new Integer(splitterNum); }
        
        /**
         * Enables and sets the value of the high voltage group filter.
         * @param hvGroup - The filter value.
         */
        public void addHVGroupFilter(int hvGroup) { filterHVGroup = new Integer(hvGroup); }
        
        /**
         * Enables and sets the value of the Jout filter.
         * @param jout - The filter value.
         */
        public void addJoutFilter(int jout) { filterJout = new Integer(jout); }
        
        /**
         * Enables and sets the value of the motherboard position filter.
         * @param mb - The filter value.
         */
        public void addMotherboardFilter(Position mb) { motherboard = mb; }
        
        /**
         * Enables and sets the value of the channel number filter.
         * @param channelNum - The filter value.
         */
        public void addChannelFilter(int channelNum) { filterChannel = new Integer(channelNum); }
        
        /**
         * Enables and sets the value of the gain filter.
         * @param gain - The filter value.
         */
        public void addGainFilter(int gain) { filterGain = new Integer(gain); }
        
        /**
         * Resets all of the filters to the default disabled state.
         */
        public void clearFilters() {
            filterX = null;
            filterY = null;
            filterAPD = null;
            filterPreamp = null;
            filterLEDChannel = null;
            filterLEDDriver = null;
            filterFADCSlot = null;
            filterFADCChannel = null;
            filterSplitter = null;
            filterHVGroup = null;
            filterJout = null;
            motherboard = null;
            filterChannel = null;
            filterGain = null;
        }
        
        /**
         * Checks to see if a given <code>CrystalDataSet</code> object
         * matches all of the defined filter parameters.
         * @param cds - The data set to check.
         * @return Returns <code>true</code> if the <code>CrystalDataSet
         * </code> matches the defined filter parameters and <code>false
         * </code> otherwise.
         */
        public boolean passesFilter(CrystalDataSet cds) {
            // Apply the filter for the crystal x-index.
            if(filterX != null) {
                if(cds.getCrystalXIndex() != filterX) { return false; }
            }
            
            // Apply the filter for the crystal y-index.
            if(filterY != null) {
                if(cds.getCrystalYIndex() != filterY) { return false; }
            }
            
            // Apply the filter for the crystal APD number.
            if(filterAPD != null) {
                if(cds.getAPDNumber() != filterAPD) { return false; }
            }
            
            // Apply the filter for the associated preamplifier.
            if(filterPreamp != null) {
                if(cds.getPreamplifierNumber() != filterPreamp) { return false; }
            }
            
            // Apply the filter for the crystal LED channel number.
            if(filterLEDChannel != null) {
                if(cds.getLEDChannel() != filterLEDChannel) { return false; }
            }
            
            // Apply the filter for the crystal LED driver.
            if(filterLEDDriver != null) {
                if(cds.getLEDDriver() != filterLEDDriver) { return false; }
            }
            
            // Apply the filter for the crystal FADC slot.
            if(filterFADCSlot != null) {
                if(cds.getFADCSlot() != filterFADCSlot) { return false; }
            }
            
            // Apply the filter for the crystal FADC channel.
            if(filterFADCChannel != null) {
                if(cds.getFADCChannel() != filterFADCChannel) { return false; }
            }
            
            // Apply the filter for the crystal splitter number.
            if(filterSplitter != null) {
                if(cds.getSplitterNumber() != filterSplitter) { return false; }
            }
            
            // Apply the filter for the crystal high voltage group.
            if(filterHVGroup != null) {
                if(cds.getHighVoltageGroup() != filterHVGroup) { return false; }
            }
            
            // Apply the filter for the Jout property.
            if(filterJout != null) {
                if(cds.getJout() != filterJout) { return false; }
            }
            
            // Apply the filter for the motherboard position.
            if(motherboard != null) {
                if(!motherboard.matchesPosition(cds.getMotherboard())) { return false; }
            }
            
            // Apply the filter for the crystal channel number.
            if(filterChannel != null) {
                if(cds.getChannel() != filterChannel) { return false; }
            }
            
            // Apply the filter for the crystal gain.
            if(filterGain != null) {
                if(cds.getGain() != filterGain) { return false; }
            }
            
            // If all filters succeeded, return true.
            return true;
        }
    }
}