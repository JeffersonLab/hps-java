package org.hps.readout;

import java.io.FileWriter;
import java.io.IOException;

/**
 * <code>TempOutputWriter</code> is a debug class that is used to
 * facilitate easy output of readout data and logs to a text file for
 * analysis or comparison outside of the readout simulation. It can
 * be enabled or disabled through the {@link
 * org.hps.readout.TempOutputWriter#setEnabled(boolean)
 * setEnabled(boolean)} command. If set to to be disabled, no action
 * will be taken upon receiving a write command. This method should
 * be used if no debug output is desired.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TempOutputWriter {
    /**
     * Indicates whether the writer should actually do anything when
     * it receives a write command. If set to <code>false</code>, no
     * action will be taken.
     */
    private boolean enabled = true;
    private FileWriter writer;
    private final String filename;
    private static final String FILE_DIR = "C:\\cygwin64\\home\\Kyle\\readout_testing\\";
    
    /**
     * Instantiates the writer.
     * @param filename - The file name of the text file to which all
     * text should be written.
     */
    public TempOutputWriter(String filename) {
        this.filename = filename;
    }
    
    /**
     * Closes the writer file stream.
     */
    public void close() {
        System.out.println(FILE_DIR + filename);
        try { writer.close(); }
        catch (IOException e) { throw new RuntimeException(); }
    }
    
    /**
     * Initializes the file writer so that it can be used.
     */
    public void initialize() {
        try {
            writer = new FileWriter(FILE_DIR + filename);
            writer.write("");
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
    
    /**
     * Indicates whether or not the writer is enabled.
     * @return Returns <code>true</code> if the writer is active, and
     * <code>false</code> otherwise.
     */
    public boolean getEnabled() {
        return enabled;
    }
    
    /**
     * Sets the writer state. If set to <code>true</code>, it will
     * output all text given to the {@link
     * org.hps.readout.TempOutputWriter#write(String) write(String)}
     * method. If set to <code>false</code>, it will ignore these
     * calls and output nothing.
     * @param state - Whether the writer should write or not.
     */
    public void setEnabled(boolean state) {
        enabled = state;
    }
    
    @Override
    public String toString() {
        return String.format("Output writer for file \"%s\". Writer is %s enabled and is %s initialized.",
                FILE_DIR + filename, enabled ? "" : "not ", writer == null ? "not " : "");
    }
    
    /**
     * Appends the specified text to the output file. Note that a
     * newline character is automatically included. Also note that
     * this method will do nothing if the writer is disabled.
     * @param text - The text to be written.
     */
    public void write(String text) {
        if(writer != null && enabled) {
            try { writer.append(text + "\n"); }
            catch (IOException e) { throw new RuntimeException(); }
        }
    }
}
