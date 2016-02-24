package org.hps.conditions.database;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javassist.Modifier;

import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.TableRegistry;
import org.reflections.Reflections;

/**
 * This is a registry of all available conditions converters. These classes are found using reflection. An anonymous
 * converter is created on the fly for {@link org.hps.conditions.api.ConditionsObject} classes with a {@link Table}, and
 * the class itself must not be abstract. If the {@link Converter} annotation is set on the class, then this is used to
 * instantiate the specific converter class instead.
 *
 * @see AbstractConditionsObjectConverter
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
public final class ConverterRegistry extends
        HashMap<Class<? extends ConditionsObject>, AbstractConditionsObjectConverter> {

    /**
     * Automatically create converters for all {@link org.hps.conditions.api.ConditionsObject} classes.
     *
     * @return the global registry of converters
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static ConverterRegistry create() {
        final ConverterRegistry registry = new ConverterRegistry();
        final Reflections reflections = new Reflections("org.hps.conditions");
        final Set<Class<? extends ConditionsObject>> objectTypes = reflections.getSubTypesOf(ConditionsObject.class);
        for (final Class<? extends ConditionsObject> objectType : objectTypes) {
            if (Modifier.isAbstract(objectType.getModifiers())) {
                // Abstract classes are not mapped to the db.
                continue;
            }
            if (objectType.getAnnotation(Table.class) == null) {
                // Explicit table mapping is required.
                continue;
            }

            // Class of the converter.
            Class<?> converterClass = null;

            // Annotation for converter parameters (optional).
            final Converter converterAnnotation = objectType.getAnnotation(Converter.class);
            if (converterAnnotation != null) {
                if (!converterAnnotation.converter().equals(AbstractConditionsObjectConverter.class)) {
                    // Set class of converter from annotation (usually default is fine).
                    converterClass = converterAnnotation.converter();
                }
            }

            // Type of the collection.
            final Class<? extends BaseConditionsObjectCollection<? extends ConditionsObject>> collectionType = TableRegistry
                    .getCollectionType(objectType);

            AbstractConditionsObjectConverter converter = null;
            if (converterClass == null) {
                // Create a generic/anonymous converter.
                converter = new AbstractConditionsObjectConverter() {
                    @Override
                    public Class getType() {
                        return collectionType;
                    }
                };
            } else {
                // Create a converter instance from the provided type in the annotation.
                try {
                    final Object object = converterClass.newInstance();
                    if (!(object instanceof AbstractConditionsObjectConverter)) {
                        throw new RuntimeException("The Converter has the wrong type: "
                                + object.getClass().getCanonicalName());
                    }
                    converter = (AbstractConditionsObjectConverter) object;
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            // Set the converter's strategy for disambiguating overlapping time validity.
            if (converterAnnotation != null && converterAnnotation.multipleCollectionsAction() != null) {
                converter.setMultipleCollectionsAction(converterAnnotation.multipleCollectionsAction());
            }

            // Register the converter by its conversion type.
            registry.put(converter.getType(), converter);
        }
        return registry;
    }

    /**
     * Class should not be instantiated by users. The {@link #create()} method should be used instead.
     */
    private ConverterRegistry() {
    }

    /**
     * Convert the object to a string.
     *
     * @return the object converted to a string
     */
    @Override
    @SuppressWarnings("rawtypes")
    public String toString() {
        final StringBuffer buff = new StringBuffer();
        for (final Entry<Class<? extends ConditionsObject>, AbstractConditionsObjectConverter> entry : this.entrySet()) {
            buff.append(entry.getValue().toString());
        }
        return buff.toString();
    }
}
