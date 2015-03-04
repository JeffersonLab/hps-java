package org.hps.monitoring.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.HasConfigurationModel;

/**
 * A <code>JPanel</code> which has a number of fields with the labels in the first column and the
 * components for showing/editing the fields in the second. It uses <code>GridBagConstraints</code>
 * for layout.
 */
// TODO: This should use features of JFormattedTextField instead of plain JTextField.
abstract class AbstractFieldsPanel extends JPanel implements PropertyChangeListener, HasConfigurationModel, ActionListener, AddActionListener {

    private int currY = 0;
    private Insets insets;
    private boolean editable = false;
    
    protected ConfigurationModel configurationModel;

    /**
     * Class constructor.
     * @param insets The insets for the panel.
     * @param editable Editable setting.
     */
    protected AbstractFieldsPanel(Insets insets, boolean editable) {
        this.insets = insets;
        this.editable = editable;
    }

    /**
     * Class constructor.
     */
    protected AbstractFieldsPanel() {
        this.insets = new Insets(1, 1, 1, 1);
    }

    /**
     * Add a field.
     * @param name The name of the field.
     * @param size The size of the field.
     * @return The JTextField component.
     */
    protected final JTextField addField(String name, int size) {
        return addField(name, "", size, this.editable);
    }

    /**
     * Add a field.
     * @param name The name of the field.
     * @param value The default value of the field.
     * @param size The size of the field.
     * @return The JTextField component.
     */
    protected final JTextField addField(String name, String value, int size) {
        return addField(name, value, size, this.editable);
    }

    /**
     * Add a field.
     * @param name The name of the field.
     * @param value The default value of the field.
     * @param tooltip The tooltip text.
     * @param size The size of the field.
     * @param editable The editable setting.
     * @return The JTextField component.
     */
    protected final JFormattedTextField addField(String name, String value, String tooltip, int size, boolean editable) {
        JFormattedTextField f = addField(name, value, size, editable);
        f.setToolTipText(tooltip);
        return f;
    }

    /**
     * Add a field.
     * @param name The name of the field.
     * @param value The default value of the field.
     * @param size The size of the field.
     * @param editable The editable setting.
     * @return The JTextField component.
     */
    protected final JFormattedTextField addField(String name, String value, int size, boolean editable) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel(name + ":");
        add(label, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.EAST;
        // JFormattedTextField field = new JFormattedTextField(value, size);
        JFormattedTextField field = new JFormattedTextField(value);
        field.setColumns(size);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setEditable(editable);
        field.setBackground(Color.WHITE);
        add(field, c);

        ++currY;

        return field;
    }

    /**
     * Add a combo box.
     * @param name The name of the combo box.
     * @param values The set of values for the combo box.
     * @return The JComboBox component.
     */
    protected final JComboBox addComboBox(String name, String[] values) {

        // System.out.println("addComboBox = " + name);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        JLabel waitModeLabel = new JLabel(name + ":");
        waitModeLabel.setHorizontalAlignment(JLabel.LEFT);
        add(waitModeLabel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.EAST;
        JComboBox combo = new JComboBox(values);
        // System.out.println("combo width = " + combo.getWidth());
        // System.out.println("combo width = " + combo.getSize().getWidth());
        combo.setEditable(editable);
        add(combo, c);

        ++currY;

        return combo;
    }

    /**
     * Add a multiline combo box.
     * @param name The name of the combo box.
     * @param values The values for the combo box.
     * @return The JComboBox component.
     */
    protected final JComboBox addComboBoxMultiline(String name, String[] values) {

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        JLabel waitModeLabel = new JLabel(name + ":");
        waitModeLabel.setHorizontalAlignment(JLabel.LEFT);
        add(waitModeLabel, c);
        ++currY;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        JComboBox combo = new JComboBox(values);
        // System.out.println("combo width = " + combo.getWidth());
        // System.out.println("combo width = " + combo.getSize().getWidth());
        combo.setEditable(editable);
        add(combo, c);

        ++currY;

        return combo;
    }

    /**
     * Add a check box.
     * @param name The name of the check box.
     * @param tooltip The tooltip text.
     * @param selected Whether the box is selected or not.
     * @param enabled Whether it is enabled or not.
     * @return The JCheckBox component.
     */
    protected final JCheckBox addCheckBox(String name, String tooltip, boolean selected, boolean enabled) {
        JCheckBox c = addCheckBox(name, selected, enabled);
        c.setToolTipText(tooltip);
        return c;
    }

    /**
     * Add a check box.
     * @param name The name of the check box.
     * @param selected Whether the check box is selected or not.
     * @param enabled Whether it is enabled or not.
     * @return The JCheckBox component.
     */
    protected final JCheckBox addCheckBox(String name, boolean selected, boolean enabled) {

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel(name + ":");
        add(label, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.EAST;
        JCheckBox checkbox = new JCheckBox();
        checkbox.setSelected(selected);
        checkbox.setEnabled(enabled);
        add(checkbox, c);

        ++currY;

        return checkbox;
    }

    protected final JButton addButton(String text) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = currY;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        JButton button = new JButton(text);
        button.setSize(new Dimension(100, 50));
        add(button, c);
        ++currY;
        return button;
    }

    /**
     * Add an ActionListener to this component. By default this does nothing, but individual
     * sub-components should attach this to individual components.
     * @param listener The AcitonListener to add.
     */
    @Override
    public void addActionListener(ActionListener listener) {
        // Sub-classes should add the listener to the appropriate child components.
    }
    
    /**
     * Sub-classes should override this method to add their own listeners to update from the model.
     */
    @Override
    public void setConfigurationModel(ConfigurationModel model) {
        this.configurationModel = model;
        
        // This listener is used to push GUI values into the model.
        this.configurationModel.addPropertyChangeListener(this);
    }        
    
    @Override
    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }
    
    boolean accept(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("ancestor")) {
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {        
    }    
}