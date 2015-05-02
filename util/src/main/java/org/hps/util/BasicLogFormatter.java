package org.hps.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BasicLogFormatter extends Formatter {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        //System.out.printf("%s: format called\n",getClass().getSimpleName());
        //sb.append(new Date(record.getMillis()))
            sb.append(getClass().getSimpleName() + " ")
            .append(record.getLevel().getLocalizedName())
            .append(": ")
            .append(formatMessage(record))
            .append(LINE_SEPARATOR);

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
                // ignore
            }
        }
        
        return sb.toString();
    }
    
}