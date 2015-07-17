package org.hps.record.run;

import java.sql.Connection;

/**
 * Abstract class for performing conversion of records in the run database into Java objects.
 * <p>
 * Sub-classes must implement the {@link #read()} method.
 *
 * @author Jeremy McCormick, SLAC
 * @param <T>
 */
public abstract class AbstractRunDatabaseReader<T> {

    /**
     * The database connection.
     */
    private Connection connection;

    /**
     * The object created from the table rows.
     */
    private T data;

    /**
     * The run number.
     */
    private int run = -1;

    /**
     * Get the database connection.
     *
     * @return the database connection
     */
    final Connection getConnection() {
        return this.connection;
    }

    /**
     * Get the data created from the {@link #read()} method being called.
     *
     * @return the data created from database records
     */
    final T getData() {
        return data;
    }

    /**
     * Get the run number.
     *
     * @return the run number
     */
    final int getRun() {
        return this.run;
    }

    /**
     * Read data from the database into a Java object accessible from the {@link #getData()} method.
     */
    abstract void read();

    /**
     * Set the database connection.
     *
     * @param connection the database connection
     */
    final void setConnection(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Set the object converted from database records.
     *
     * @param data the object converted from database records
     */
    final void setData(final T data) {
        this.data = data;
    }

    /**
     * Set the run number.
     *
     * @param run the run number
     */
    final void setRun(final int run) {
        this.run = run;
    }
}
