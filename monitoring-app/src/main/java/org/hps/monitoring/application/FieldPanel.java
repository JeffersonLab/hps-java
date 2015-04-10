package org.hps.monitoring.application;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * A panel with a label and a text field.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
class FieldPanel extends JPanel {

    /**
     * The default component border.
     */
    private static final Border DEFAULT_BORDER = BorderFactory.createLoweredBevelBorder();

    /**
     * The component with the field value.
     */
    private final JTextField field;

    /**
     * Class constructor.
     *
     * @param fieldName the name of the field for the label
     * @param defaultValue the default value
     * @param size the size of the field
     * @param editable <code>true</code> if field is editable
     */
    protected FieldPanel(final String fieldName, final String defaultValue, final int size, final boolean editable) {

        final TitledBorder title = BorderFactory.createTitledBorder(DEFAULT_BORDER, fieldName);
        title.setTitleJustification(TitledBorder.LEFT);

        this.field = new JTextField(defaultValue, size);
        this.field.setHorizontalAlignment(SwingConstants.RIGHT);
        this.field.setEditable(editable);
        this.field.setBorder(title);
        add(this.field);
    }

    /**
     * Get a <code>Double</code> value from the field.
     *
     * @return the <code>Double</code> value
     */
    final Double getDoubleValue() {
        return Double.parseDouble(getValue());
    }

    /**
     * Get an <code>Integer</code> value from the field.
     *
     * @return the <code>Integer</code> value
     */
    final Integer getIntegerValue() {
        return Integer.parseInt(getValue());
    }

    /**
     * Get a <code>Long</code> value from the field.
     *
     * @return the <code>Long</code> value
     */
    final Long getLongValue() {
        return Long.parseLong(getValue());
    }

    /**
     * Get the <code>String</code> value from the field.
     *
     * @return the <code>String</code> value from the field
     */
    final String getValue() {
        return this.field.getText();
    }

    /**
     * Set the field value from a <code>double</code>.
     *
     * @param value the <code>double</code> value
     */
    final void setValue(final double value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FieldPanel.this.field.setText(new Double(value).toString());
            }
        });
    }

    /**
     * Set the field value from an <code>int</code>.
     *
     * @param value the <code>int</code> value
     */
    final void setValue(final int value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FieldPanel.this.field.setText(new Integer(value).toString());
            }
        });
    }

    /**
     * Set the field value from a <code>long</code>.
     *
     * @param value the <code>long</code> value
     */
    final void setValue(final long value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FieldPanel.this.field.setText(new Long(value).toString());
            }
        });
    }

    /**
     * Set the field value from a <code>String</code>.
     *
     * @param value the <code>String</code> value
     */
    void setValue(final String value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FieldPanel.this.field.setText(value);
            }
        });
    }
}
