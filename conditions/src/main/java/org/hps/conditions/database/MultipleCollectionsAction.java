package org.hps.conditions.database;

/**
 * This is the action that should be used to pick a conditions set if there are multiple conditions sets returned from a
 * query on type and name.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public enum MultipleCollectionsAction {
    /**
     * Use the updated date.
     */
    LAST_UPDATED,
    /**
     * Use the creation date.
     */
    LAST_CREATED,
    /**
     * Use the largest run start number.
     */
    LATEST_RUN_START,
    /**
     * Throw an error.
     */
    ERROR
}
