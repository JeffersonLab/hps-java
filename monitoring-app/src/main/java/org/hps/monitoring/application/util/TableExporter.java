package org.hps.monitoring.application.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * This is a utility for exporting a JTable's model data to a text file.
 * <p>
 * Non-numeric fields such as strings are delimited by double quotes.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class TableExporter {

    /**
     * Export the given table to a text file.
     *
     * @param table the JTable component
     * @param path the output file path
     * @param fieldDelimiter the field delimiter to use
     * @throws IOException if there are errors writing the file
     */
    public static void export(final JTable table, final String path, final char fieldDelimiter) throws IOException {

        final StringBuffer buffer = new StringBuffer();
        final TableModel model = table.getModel();
        final int rowCount = model.getRowCount();
        final int columnCount = model.getColumnCount();

        // Column headers.
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            buffer.append("\"" + model.getColumnName(columnIndex) + "\"" + fieldDelimiter + ",");
        }
        buffer.setLength(buffer.length() - 1);
        buffer.append('\n');

        // Row data.
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                final Object value = model.getValueAt(rowIndex, columnIndex);
                if (Number.class.isAssignableFrom(model.getColumnClass(columnIndex))) {
                    buffer.append(value);
                } else {
                    buffer.append("\"" + value + "\"" + fieldDelimiter);
                }
                buffer.append(",");
            }
            buffer.setLength(buffer.length() - 1);
            buffer.append('\n');
        }

        // Write string buffer to file.
        final BufferedWriter out = new BufferedWriter(new FileWriter(path));
        out.write(buffer.toString());
        out.flush();
        out.close();
    }

    /**
     * Do not allow class instantiation.
     */
    private TableExporter() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}
