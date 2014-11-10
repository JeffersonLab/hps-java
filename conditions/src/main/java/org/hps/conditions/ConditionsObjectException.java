package org.hps.conditions;

/**
 * Generic Exception type throw by methods of {@link ConditionsObject} or other
 * associated classes such as converters and collections.
 */
@SuppressWarnings("serial")
public final class ConditionsObjectException extends Exception {

    ConditionsObject object;

    public ConditionsObjectException(String message) {
        super(message);
    }

    public ConditionsObjectException(String message, ConditionsObject object) {
        super(message);
        this.object = object;
    }

    public ConditionsObject getConditionsObject() {
        return object;
    }
}