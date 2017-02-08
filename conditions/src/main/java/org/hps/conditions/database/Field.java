package org.hps.conditions.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a java <code>Annotation</code> for assigning a "get" method to one or more database table columns.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Field {

    /**
     * The names of the table columns associated with this method.
     *
     * @return the names of the table columns associated with this method
     */
    String[] names() default "";
}
