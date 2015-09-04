package org.hps.record;

/**
 * This is a generic error type for exceptions that occur during event processing. It extends
 * <code>RuntimeException</code> so that methods need not declare a <code>throws</code> clause in order to use it.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
public class RecordProcessingException extends RuntimeException {

    /**
     * Class constructor.
     *
     * @param message the error message
     */
    public RecordProcessingException(final String message) {
        super(message);
    }

    /**
     * Class constructor.
     *
     * @param message the error message
     * @param x cause of the error
     */
    public RecordProcessingException(final String message, final Throwable x) {
        super(message, x);
    }
    
    /**
     * Class constructor.
     *
     * @param x cause of the error
     */
    public RecordProcessingException(final Throwable x) {
        super(x);
    }

}
