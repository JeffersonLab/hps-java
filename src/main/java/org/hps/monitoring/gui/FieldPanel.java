package org.hps.monitoring.gui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * A panel with a label and a text field.
 */
class FieldPanel extends JPanel {

    String fieldName;
    String defaultValue;
    JTextField field;

    static Border border = BorderFactory.createLoweredBevelBorder();

    FieldPanel(String fieldName, String defaultValue, int size, boolean editable) {

        this.fieldName = fieldName;
        this.defaultValue = defaultValue;

        TitledBorder title = BorderFactory.createTitledBorder(border, fieldName);
        title.setTitleJustification(TitledBorder.LEFT);

        field = new JTextField(defaultValue, size);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setEditable(editable);
        field.setBorder(title);
        add(field);
    }

    void setValue(final String value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                field.setText(value);
            }
        });
    }

    void setValue(final int value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                field.setText(new Integer(value).toString());
            }
        });
    }

    void setValue(final double value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                field.setText(new Double(value).toString());
            }
        });
    }

    void setValue(final long value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                field.setText(new Long(value).toString());
            }
        });
    }

    String getValue() {
        return field.getText();
    }

    Integer getIntegerValue() {
        return Integer.parseInt(getValue());
    }

    Double getDoubleValue() {
        return Double.parseDouble(getValue());
    }

    Long getLongValue() {
        return Long.parseLong(getValue());
    }
}