package org.hps.conditions.api;

/**
 * This class is a simplistic representation of a packaged identifier for use in the conditions system.
 */
public abstract class AbstractIdentifier {

    /**
     * Encode the ID into a long.
     *
     * @return the ID encoded into a <code>long</code>
     */
    public abstract long encode();

    /**
     * Check if the ID is valid.
     *
     * @return <code>true</code> if valid
     */
    public abstract boolean isValid();
}
