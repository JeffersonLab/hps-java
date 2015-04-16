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
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.HasConfigurationModel;

/**
 * A <code>JPanel</code> which has a number of fields with the labels in the first column and the components for
 * showing/editing the fields in the second. It uses <code>GridBagConstraints</code> for layout.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
abstract class AbstractFieldsPanel extends JPanel implements PropertyChangeListener, HasConfigurationModel,
ActionListener, AddActionListener {

    /**
     * Default button height in pixels.
     */
    private static final int DEFAULT_BUTTON_HEIGHT = 50;

    /**
     * Default button width in pixels.
     */
    private static final int DEFAULT_BUTTON_WIDTH = 100;

    /**
     * The configuration model for the component.
     */
    private ConfigurationModel configurationModel;

    /**
     * Grid Y which is incremented as components are added.
     */
    private int currentGridY = 0;

    /**
     * Flag which sets if this component and its children are editable.
     */
    private boolean editable = false;

    /**
     * The default insets used for all internal <code>GridBagConstraints</code>.
     */
    private final Insets insets;

    /**
     * Class constructor.
     */
    protected AbstractFieldsPanel() {
        this.insets = new Insets(1, 1, 1, 1);
    }

    /**
     * Class constructor.
     *
     * @param insets The insets for the panel.
     * @param editable Editable setting.
     */
    protected AbstractFieldsPanel(final Insets insets, final boolean editable) {
        this.insets = insets;
        this.editable = editable;
    }

    /**
     * True if property change event should be accepted.
     *
     * @param evt the property change event
     * @return <code>true</code> if property change event should be accepted
     */
    boolean accept(final PropertyChangeEvent evt) {
        return !"ancestor".equals(evt.getPropertyName());
    }

    /**
     * Add an ActionListener to this component. By default this does nothing, but individual sub-components should
     * attach this to individual components.
     *
     * @param listener the AcitonListener to add
     */
    @Override
    public void addActionListener(final ActionListener listener) {
        // Sub-classes should add the listener to the appropriate child components.
    }

    /**
     * Add a button with text.
     *
     * @param text the text in the button
     * @return the button component
     */
    protected final JButton addButton(final String text) {
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        final JButton button = new JButton(text);
        button.setSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.add(button, c);
        ++this.currentGridY;
        return button;
    }

    /**
     * Add a check box.
     *
     * @param name the name of the check box
     * @param selected whether the check box is selected or not
     * @param enabled whether it is enabled or not
     * @return the JCheckBox component
     */
    protected final JCheckBox addCheckBox(final String name, final boolean selected, final boolean enabled) {

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.WEST;
        final JLabel label = new JLabel(name + ":");
        this.add(label, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.EAST;
        final JCheckBox checkbox = new JCheckBox();
        checkbox.setSelected(selected);
        checkbox.setEnabled(enabled);
        this.add(checkbox, c);

        ++this.currentGridY;

        return checkbox;
    }

    /**
     * Add a check box.
     *
     * @param name the name of the check box
     * @param tooltip the tooltip text
     * @param selected <code>true</code> if component is selected
     * @param enabled <code>true</code> if component enabled
     * @return The JCheckBox component.
     */
    protected final JCheckBox addCheckBox(final String name, final String tooltip, final boolean selected,
            final boolean enabled) {
        final JCheckBox c = this.addCheckBox(name, selected, enabled);
        c.setToolTipText(tooltip);
        return c;
    }

    /**
     * Add a combo box.
     *
     * @param name the name of the combo box
     * @param values the set of values for the combo box
     * @return the JComboBox component
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final JComboBox addComboBox(final String name, final Object[] values) {

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.WEST;
        final JLabel waitModeLabel = new JLabel(name);
        waitModeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        this.add(waitModeLabel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.EAST;
        final JComboBox combo = new JComboBox(values);
        combo.setEditable(this.editable);
        this.add(combo, c);

        ++this.currentGridY;

        return combo;
    }

    /**
     * Add a multiline combo box.
     *
     * @param name the name of the combo box
     * @param values the values for the combo box
     * @return the <code>JComboBox</code> component
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final JComboBox addComboBoxMultiline(final String name, final String[] values) {

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.WEST;
        final JLabel waitModeLabel = new JLabel(name + ":");
        waitModeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        this.add(waitModeLabel, c);
        ++this.currentGridY;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.WEST;
        final JComboBox combo = new JComboBox(values);
        combo.setEditable(this.editable);
        this.add(combo, c);

        ++this.currentGridY;

        return combo;
    }

    /**
     * Add a labeled JComponent to the panel.
     *
     * @param name the label text
     * @param component the component to add
     */
    void addComponent(final String name, final JComponent component) {

        // Add the label.
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.WEST;
        final JLabel label = new JLabel(name + ":");
        this.add(label, c);

        // Add the component.
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.EAST;
        this.add(component, c);

        ++this.currentGridY;
    }

    /**
     * Add a field.
     *
     * @param name the name of the field
     * @param size the size of the field
     * @return the <code>JTextField</code> component
     */
    protected final JTextField addField(final String name, final int size) {
        return this.addField(name, "", size, this.editable);
    }

    /**
     * Add a field.
     *
     * @param name the name of the field
     * @param value the default value of the field
     * @param size the size of the field
     * @return the <code>JTextField</code> component
     */
    protected final JTextField addField(final String name, final String value, final int size) {
        return this.addField(name, value, size, this.editable);
    }

    /**
     * Add a field.
     *
     * @param name the name of the field
     * @param value the default value of the field
     * @param size the size of the field
     * @param editable the editable setting
     * @return the <code>JTextField</code> component
     */
    protected final JFormattedTextField addField(final String name, final String value, final int size,
            final boolean editable) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.WEST;
        final JLabel label = new JLabel(name + ":");
        this.add(label, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = this.currentGridY;
        c.insets = this.insets;
        c.anchor = GridBagConstraints.EAST;
        // JFormattedTextField field = new JFormattedTextField(value, size);
        final JFormattedTextField field = new JFormattedTextField(value);
        field.setColumns(size);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setEditable(editable);
        field.setBackground(Color.WHITE);
        this.add(field, c);

        ++this.currentGridY;

        return field;
    }

    /**
     * Add a field.
     *
     * @param name the name of the field
     * @param value the default value of the field
     * @param tooltip the tooltip text
     * @param size the size of the field
     * @param editable the editable setting
     * @return the <code>JTextField</code> component
     */
    protected final JFormattedTextField addField(final String name, final String value, final String tooltip,
            final int size, final boolean editable) {
        final JFormattedTextField f = this.addField(name, value, size, editable);
        f.setToolTipText(tooltip);
        return f;
    }

    /**
     * Get the {@link org.hps.monitoring.application.model.ConfigurationModel} for this component.
     *
     * @return the {@link org.hps.monitoring.application.model.ConfigurationModel} for this component
     */
    @Override
    public ConfigurationModel getConfigurationModel() {
        return this.configurationModel;
    }

    /**
     * Handle a property change event.
     *
     * @param evt the property change event
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
    }

    /**
     * Sub-classes should override this method to add their own listeners to update from the model.
     *
     * @param model the configuration model
     */
    @Override
    public void setConfigurationModel(final ConfigurationModel model) {
        this.configurationModel = model;

        // This listener is used to push GUI values into the model.
        this.configurationModel.addPropertyChangeListener(this);
    }
}
