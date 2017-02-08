/**
 * Implementation of database API for detector conditions
 * <p>
 * The {@link org.hps.conditions.database.DatabaseConditionsManager} has a set of converters for handling the 
 * conversion of conditions table data to typed collections.  The converters are created automatically using 
 * introspection of {@link org.hps.conditions.api.ConditionsObject} classes that have the
 * {@link org.hps.conditions.database.Table} and {@link org.hps.conditions.database.Field} annotations.
 *
 */
package org.hps.conditions.database;

