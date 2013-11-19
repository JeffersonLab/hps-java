/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.util;
import java.io.IOException;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.lcio.LCIOWriter;

/**
 *
 * @author phansson
 */
public abstract class LCIOFilterDriver extends Driver {
    protected String outputFile;
    protected LCIOWriter writer;
    protected boolean debug = false;
    
    public LCIOFilterDriver() {        
    }
    
    public void setOutputFilePath(String output) {
        this.outputFile = output;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    abstract boolean eventFilter(EventHeader event);

    private void setupWriter() {
        // Cleanup existing writer.
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (IOException x) {
                System.err.println(x.getMessage());
            }
        }

        // Setup new writer.
        try {
            writer = new LCIOWriter(outputFile);
        } catch (IOException x) {
            throw new RuntimeException("Error creating writer", x);
        }


        try {
            writer.reOpen();
        } catch (IOException x) {
            throw new RuntimeException("Error rewinding LCIO file", x);
        }
    }

    protected void startOfData() {
        setupWriter();
    }

    protected void endOfData() {
        try {
            writer.close();
        } catch (IOException x) {
            throw new RuntimeException("Error rewinding LCIO file", x);
        }
    }

    protected void process(EventHeader event) {

        if(eventFilter(event)) {
            try {
                writer.write(event);
            } catch (IOException x) {
                throw new RuntimeException("Error writing LCIO file", x);
            }
        }
    }

    protected void suspend() {
        try {
            writer.flush();
        } catch (IOException x) {
            throw new RuntimeException("Error flushing LCIO file", x);
        }
    }
    
}
