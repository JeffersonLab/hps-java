package org.hps.conditions.database;

/**
 * This is the action that should be used to pick a conditions set if there are multiple conditions sets returned from a
 * query on type and name.
 *
 */
public enum MultipleCollectionsAction {
    /**
     * Throw an error.
     */
    ERROR,
    /**
     * Use the creation date.
     */
    LAST_CREATED,
    /**
     * Use the updated date.
     */
    LAST_UPDATED,
    /**
     * Use the largest run start number.
     */
    LATEST_RUN_START
}
