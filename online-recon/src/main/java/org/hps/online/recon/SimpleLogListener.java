package org.hps.online.recon;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * Writes lines from <code>Tailer</code> to the client socket.
 */
public class SimpleLogListener extends TailerListenerAdapter {

    BufferedWriter bw;

    void setBufferedWriter(BufferedWriter bw) {
        this.bw = bw;
    }

    public void handle(String line) {
        try {
            bw.write(line + '\n');
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error writing log line", e);
        }
    }
}