package org.hps.conditions.api;


/**
 * Thrown by methods of {@link ConditionsObject} or other associated classes
 * such as converters and collections.
 */
@SuppressWarnings("serial")
public final class ConditionsObjectException extends Exception {

    /**
     * The associated conditions object to the error.
     */
    private ConditionsObject object;

    /**
     * Error with a message.
     *
     * @param message The error message.
     */
    public ConditionsObjectException(final String message) {
        super(message);
    }

    /**
     * Error with an associated throwable.
     *
     * @param message The error message.
     * @param cause The error's cause.
     */
    public ConditionsObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Error with a message and object.
     *
     * @param message The error message.
     * @param object The associated conditions object.
     */
    public ConditionsObjectException(final String message, final ConditionsObject object) {
        super(message);
        this.object = object;
    }

    /**
     * Get the associated conditions object to the error.
     * @return The object associated with the error.
     */
    public ConditionsObject getConditionsObject() {
        return object;
    }
}
