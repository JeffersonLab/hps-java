package org.hps.monitoring.application;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A small JPanel with a date field and a label on its border.
 */
class DatePanel extends FieldPanel {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    DatePanel(String fieldName, String defaultValue, int size, boolean editable) {
        super(fieldName, defaultValue, size, editable);
    }

    DatePanel(String fieldName, Date defaultValue, SimpleDateFormat format, int size, boolean editable) {
        super(fieldName, format.format(defaultValue), size, editable);
    }

    void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    void setValue(Date date) {
        setValue(dateFormat.format(date));
    }

    Date getDateValue() {
        try {
            return dateFormat.parse(getValue());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
