package org.hps.conditions;

/**
 * Generic Exception type throw by methods of {@link ConditionsObject}
 * or other associated classes such as converters and collections.
 */
@SuppressWarnings("serial")
public final class ConditionsObjectException extends Exception {

    ConditionsObject _object;

    public ConditionsObjectException(String message) {
        super(message);
    }

    public ConditionsObjectException(ConditionsObject object, String message) {
        super(message);
    }

    public ConditionsObject getConditionsObject() {
        return _object;
    }
}