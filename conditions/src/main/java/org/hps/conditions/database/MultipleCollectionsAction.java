package org.hps.conditions.database;

/**
 * This is the action that should be taken if there are multiple conditions sets
 * returned from a query on type and name.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public enum MultipleCollectionsAction {
    LAST_UPDATED,
    LAST_CREATED,
    LATEST_RUN_START,
    ERROR
}
