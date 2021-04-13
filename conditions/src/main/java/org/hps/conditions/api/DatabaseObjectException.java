package org.hps.conditions.api;

/**
 * Exception for errors that occur when performing operations on {@link DatabaseObject}s.
 */
public final class DatabaseObjectException extends Exception {

    /**
     * The object on which the error occurred.
     */
    private final DatabaseObject object;

    /**
     * Class constructor.
     *
     * @param message the message describing the error
     * @param object the object with the error
     */
    public DatabaseObjectException(final String message, final DatabaseObject object) {
        super(message);
        this.object = object;
    }

    /**
     * Class constructor.
     *
     * @param message the message describing the error
     * @param cause the cause of the error (another <code>Exception</code> that was caught)
     * @param object the object with the error
     */
    public DatabaseObjectException(final String message, final Throwable cause, final DatabaseObject object) {
        super(message, cause);
        this.object = object;
    }

    /**
     * Get the object with the error.
     *
     * @return the object that had the error
     */
    public DatabaseObject getDatabaseObject() {
        return this.object;
    }

}
