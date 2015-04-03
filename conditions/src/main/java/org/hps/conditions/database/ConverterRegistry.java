package org.hps.conditions.database;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javassist.Modifier;

import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectUtilities;
import org.reflections.Reflections;

/**
 * This is a registry of all available conditions converters. These classes are found using reflection. An anonymous
 * converter is created on the fly for {@link org.hps.conditions.api.ConditionsObject} classes with a {@link Table}, and
 * the class itself must not be abstract. If the {@link Converter} annotation is set on the class, then this is used to
 * instantiate the specific converter class instead.
 *
 * @see AbstractConditionsObjectConverter
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
public final class ConverterRegistry extends HashMap<Class<? extends ConditionsObject>, AbstractConditionsObjectConverter> {

    /**
     * Class should not be instantiated by users.
     * The {@link #create()} method should be used instead.
     */
    private ConverterRegistry() {
    }

    /**
     * Automatically create converters for all {@link org.hps.conditions.api.ConditionsObject} classes.
     * 
     * @return The registry of converters.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static ConverterRegistry create() {
        final ConverterRegistry registry = new ConverterRegistry();
        final Reflections reflections = new Reflections("org.hps.conditions");
        final Set<Class<? extends ConditionsObject>> objectTypes = reflections.getSubTypesOf(ConditionsObject.class);
        for (Class<? extends ConditionsObject> objectType : objectTypes) {
            if (Modifier.isAbstract(objectType.getModifiers())) {
                continue;
            }
            if (objectType.getAnnotation(Table.class) == null) {
                continue;
            }
            MultipleCollectionsAction multipleCollectionsAction = MultipleCollectionsAction.ERROR;
            Class<?> converterClass = null;
            final Converter converterAnnotation = objectType.getAnnotation(Converter.class);
            if (converterAnnotation != null) {
                multipleCollectionsAction = converterAnnotation.multipleCollectionsAction();
                if (!converterAnnotation.converter().equals(AbstractConditionsObjectConverter.class)) {
                    converterClass = converterAnnotation.converter();
                }
            }

            final Class<? extends BaseConditionsObjectCollection<? extends ConditionsObject>> 
                collectionType = ConditionsObjectUtilities.getCollectionType(objectType);

            AbstractConditionsObjectConverter converter = null;
            if (converterClass == null) {
                converter = new AbstractConditionsObjectConverter() {
                    @Override
                    public Class getType() {
                        return collectionType;
                    }
                };
            } else {
                try {
                    Object object = converterClass.newInstance();
                    if (!(object instanceof AbstractConditionsObjectConverter)) {
                        throw new RuntimeException("The Converter has the wrong type: "
                                + object.getClass().getCanonicalName());
                    }
                    converter = (AbstractConditionsObjectConverter) object;
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            converter.setMultipleCollectionsAction(multipleCollectionsAction);
            registry.put(converter.getType(), converter);
        }
        return registry;
    }

    /**
     * Convert the object to a string.
     * @return The object converted to a string.
     */
    @SuppressWarnings("rawtypes")
    public final String toString() {
        final StringBuffer buff = new StringBuffer();
        for (Entry<Class<? extends ConditionsObject>, AbstractConditionsObjectConverter> entry : entrySet()) {
            buff.append(entry.getValue().toString());
        }
        return buff.toString();
    }
}
