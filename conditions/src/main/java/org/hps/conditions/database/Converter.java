package org.hps.conditions.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hps.conditions.api.AbstractConditionsObjectConverter;

/**
 * This is an annotation for providing converter configuration for {@link org.hps.conditions.api.ConditionsObject}
 * classes.
 *
 * @see AbstractConditionsObjectConverter
 * @author Jeremy McCormick, SLAC
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Converter {
    /**
     * Get a custom converter class for the type. (Optional)
     *
     * @return the custom converter for the type
     */
    Class<?> converter() default AbstractConditionsObjectConverter.class;

    /**
     * Get the action to perform in the converter when multiple conditions are found for the current configuration of
     * run number, detector and tag in the manager.
     *
     * @return the multiple collections action
     */
    MultipleCollectionsAction multipleCollectionsAction() default MultipleCollectionsAction.LAST_CREATED;
}
