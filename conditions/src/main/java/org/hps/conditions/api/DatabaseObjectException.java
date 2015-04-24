package org.hps.conditions.api;

/**
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class DatabaseObjectException extends Exception {

    /**
     *
     */
    private final DatabaseObject object;

    /**
     * @param message
     * @param object
     */
    public DatabaseObjectException(final String message, final DatabaseObject object) {
        super(message);
        this.object = object;
    }

    /**
     * @param message
     * @param cause
     * @param object
     */
    public DatabaseObjectException(final String message, final Throwable cause, final DatabaseObject object) {
        super(message, cause);
        this.object = object;
    }

    /**
     * @return
     */
    public DatabaseObject getDatabaseObject() {
        return this.object;
    }

}
