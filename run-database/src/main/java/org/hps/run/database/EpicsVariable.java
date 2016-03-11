package org.hps.run.database;

/**
 * Information about an EPICS variable including its name in the EPICS database, column name for the run database,
 * description of the variable, and type (either 2s or 20s).
 * <p>
 * This class is used to represent data from the <i>epics_variables</i> table in the run database.
 *
 * @see EpicsType
 * @see org.hps.run.database.EpicsVariableDao
 * @see org.hps.run.database.EpicsVariableDaoImpl
 * @author Jeremy McCormick, SLAC
 */
public final class EpicsVariable {

    /**
     * The name of the variable in the run database.
     */
    private final String columnName;

    /**
     * A description of the variable.
     */
    private final String description;

    /**
     * The name of the variable in the EPICs system.
     */
    private final String variableName;

    /**
     * The type of the variable (2s or 20s).
     */
    private final EpicsType variableType;

    /**
     * Create an EPICs variable.
     *
     * @param variableName the name of the variable
     * @param columnName the column name in the run db
     * @param description the variable's description
     * @param variableType the type of the variable
     */
    public EpicsVariable(final String variableName, final String columnName, final String description,
            final EpicsType variableType) {
        this.variableName = variableName;
        this.columnName = columnName;
        this.description = description;
        this.variableType = variableType;
    }

    /**
     * Create an EPICs variable.
     *
     * @param variableName the name of the variable
     * @param columnName the column name in the run database
     * @param description the variable's description
     * @param type the integer encoding of the type
     */
    public EpicsVariable(final String variableName, final String columnName, final String description, final int type) {
        this.variableName = variableName;
        this.columnName = columnName;
        this.description = description;
        this.variableType = EpicsType.fromInt(type);
    }

    /**
     * Get the column name.
     *
     * @return the column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Get the variable's description.
     *
     * @return the variable's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the variable name.
     *
     * @return the variable name
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Get the variable's type.
     *
     * @return the variable's type
     */
    public EpicsType getVariableType() {
        return variableType;
    }

    /**
     * Return this object converted to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return "EpicsVariable { variableName: " + variableName + ", columnName: " + columnName + ", description: "
                + description + ", variableType: " + variableType.name() + " }";
    }
}
