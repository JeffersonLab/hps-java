package org.hps.monitoring.application;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A small <code>JPanel</code> with a date field and a label on its border.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class DatePanel extends FieldPanel {

    /**
     * Default date formatting.
     */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Create a date panel.
     *
     * @param fieldName the field name for the label
     * @param defaultValue the default value
     * @param format the date formatter
     * @param size the size of the field
     * @param editable <code>true</code> to enable editing
     */
    DatePanel(final String fieldName, final Date defaultValue, final SimpleDateFormat format, final int size,
            final boolean editable) {
        super(fieldName, format.format(defaultValue), size, editable);
    }

    /**
     * Create a date panel with default date formatting.
     *
     * @param fieldName the field name for the label
     * @param defaultValue the default value
     * @param size the size of the field
     * @param editable <code>true</code> to enable editing
     */
    DatePanel(final String fieldName, final String defaultValue, final int size, final boolean editable) {
        super(fieldName, defaultValue, size, editable);
    }

    /**
     * Get the value of the field.
     *
     * @return the <code>Date</code> object
     */
    Date getDateValue() {
        try {
            return this.dateFormat.parse(getValue());
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the date formatter.
     *
     * @param dateFormat the date formatter
     */
    void setDateFormat(final SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * Set the value of the field.
     *
     * @param date the <code>Date</code> object
     */
    void setValue(final Date date) {
        setValue(this.dateFormat.format(date));
    }
}
