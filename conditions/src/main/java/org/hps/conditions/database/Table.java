package org.hps.conditions.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to assign a class to one
 * or more database tables.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) 
public @interface Table {
    
    /**
     * Get the names of the tables.
     * @return The names of the tables.
     */
    public String[] names() default "";
}
