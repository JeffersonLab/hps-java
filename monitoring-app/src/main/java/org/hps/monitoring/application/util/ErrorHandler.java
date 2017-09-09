package org.hps.monitoring.application.util;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * An error handling class which is able to do any of the following, depending on how the users wants to handle the
 * error.
 * <p>
 * <ul>
 * <li>Print a message</li>
 * <li>Print the stack trace</li>
 * <li>Log message to a Logger</li>
 * <li>Show an error dialog</li>
 * <li>Raise an exception</li>
 * <li>Exit the application</li>
 * </ul>
 * <p>
 * It mostly uses the "builder" pattern so that the various handling methods can be easily chained, where appropriate.
 * Some methods are not available for chaining when it doesn't make sense.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ErrorHandler {

    /**
     * The component used for showing dialog windows.
     */
    private final Component component;

    /**
     * The thrown error.
     */
    private Throwable error;

    /**
     * A logger for reporting error messages.
     */
    private final Logger logger;

    /**
     * The error message.
     */
    private String message;

    /**
     * Class constructor.
     *
     * @param component The GUI component to which this object is assigned.
     * @param logger The logger to which messages will be written.
     */
    public ErrorHandler(final Component component, final Logger logger) {
        this.logger = logger;
        this.component = component;
    }

    /**
     * Exit the application.
     * <p>
     * This is not a chain-able method for obvious reasons.
     */
    public void exit() {
        System.err.println("Fatal error.  Application will exit.");
        System.exit(1);
    }

    /**
     * Log the error message to the <code>Logger</code>.
     *
     * @return this object
     */
    public ErrorHandler log() {
        this.logger.log(Level.SEVERE, this.message, this.error);
        return this;
    }

    /**
     * Print the error message to System.err.
     *
     * @return This object.
     */
    public ErrorHandler printMessage() {
        System.err.println(this.message);
        return this;
    }

    /**
     * Print the full stack trace of the error to System.err.
     *
     * @return this object
     */
    public ErrorHandler printStackTrace() {
        this.error.printStackTrace();
        return this;
    }

    /**
     * Immediately re-throw the error as a <code>RuntimeException</code>.
     * <p>
     * Additional methods cannot be chained to this as they would not be executed.
     */
    public void raiseException() {
        throw new RuntimeException(this.message, this.error);
    }

    /**
     * Set the error that occurred.
     * <p>
     * This should always be called first when method chaining.
     *
     * @param error the error which is any type of <code>Throwable</code>
     * @return this object
     */
    public ErrorHandler setError(final Throwable error) {
        this.error = error;
        this.message = error.getMessage();
        return this;
    }

    /**
     * Set the error message, if it differs from the exception's message.
     *
     * @param message the error message
     * @return this object
     */
    public ErrorHandler setMessage(final String message) {
        this.message = message;
        return this;
    }

    /**
     * Show an error dialog with the message.
     *
     * @return this object
     */
    public ErrorHandler showErrorDialog() {
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                JOptionPane.showMessageDialog(ErrorHandler.this.component, ErrorHandler.this.error.getMessage(),
                        "Application Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
        return this;
    }

    /**
     * Show an error dialog with a custom message and title.
     *
     * @param message the error message
     * @param title the title of the dialog window
     * @return this object
     */
    public ErrorHandler showErrorDialog(final String message, final String title) {
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                JOptionPane.showMessageDialog(ErrorHandler.this.component, message, title, JOptionPane.ERROR_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
        return this;
    }
}
