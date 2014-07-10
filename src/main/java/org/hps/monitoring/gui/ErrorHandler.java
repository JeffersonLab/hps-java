package org.hps.monitoring.gui;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * An error handling class.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ErrorHandler {

    Logger logger;
    Component component;
    Throwable error;
    String message;
        
    ErrorHandler(Component component, Logger logger) {
        this.logger = logger;
        this.component = component;
    }
    
    ErrorHandler setError(Throwable error) {
        this.error = error;
        this.message = error.getMessage();
        return this;
    }
    
    ErrorHandler setMessage(String message) {
        this.message = message;
        return this;
    }
        
    ErrorHandler printStackTrace() {
        error.printStackTrace();
        return this;
    }
    
    ErrorHandler printMessage() {        
        System.err.println(message);
        return this;
    }
    
    ErrorHandler log() {
        logger.log(Level.SEVERE, message);
        return this;
    }
        
    ErrorHandler showErrorDialog() {
        final Runnable runnable = new Runnable() {
            public void run() {        
                JOptionPane.showMessageDialog(
                        component,
                        error.getMessage(), 
                        "Application Error", 
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
        return this;
    }
    
    void raiseException() {
        throw new RuntimeException(message, error);
    }
    
    void exit() {
        System.err.println("Fatal error.  Application will exit.");
        System.exit(1);
    }        
               
    /*
    void handleError(final String message, boolean showDialog, boolean throwException) 
    */     
}
