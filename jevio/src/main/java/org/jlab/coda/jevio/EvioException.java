package org.jlab.coda.jevio;

/**
 * This is a general exception used to indicate a problem in the Jevio package.
 * 
 * @author heddle
 * 
 */
@SuppressWarnings("serial")
public class EvioException extends Exception {

    /**
     * Create an EVIO Exception indicating an error specific to the EVIO system.
     * {@inheritDoc}<p/>
     *
     * @param message {@inheritDoc}<p/>
     */
    public EvioException(String message) {
        super(message);
    }

    /**
     * Create an EVIO Exception with the specified message and cause.
     * {@inheritDoc}<p/>
     *
     * @param message {@inheritDoc}<p/>
     * @param cause   {@inheritDoc}<p/>
     */
    public EvioException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create an EVIO Exception with the specified cause.
     * {@inheritDoc}<p/>
     *
     * @param cause {@inheritDoc}<p/>
     */
    public EvioException(Throwable cause) {
        super(cause);
    }

}
