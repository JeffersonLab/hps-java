package org.hps.conditions.api;

/**
 * This class is a simplistic representation of a packaged identifier for use in
 * the conditions system.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public abstract class AbstractIdentifier {

    /**
     * Encode the ID into a long.
     *
     * @return The ID encoded into a long.
     */
    public abstract long encode();

    /**
     * Check if the ID is valid.
     *
     * @return True if valid.
     */
    public abstract boolean isValid();
}
