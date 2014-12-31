package org.hps.conditions.database;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javassist.Modifier;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectUtilities;
import org.reflections.Reflections;

/**
 * This is a registry of all available conditions converters.
 * These classes are found using reflection.  Only converters
 * with a {@link Table} annotation are loaded, and the class
 * must not be abstract.
 * 
 * @see ConditionsObjectConverter
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConverterRegistry extends HashMap<Class<? extends ConditionsObject>, ConditionsObjectConverter> {
    
    private ConverterRegistry() {
    }
    
    /**
     * Automatically create converters for all conditions object classes.
     * @return The registry of converters.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ConverterRegistry create() {
        ConverterRegistry registry = new ConverterRegistry();
        Reflections reflections = new Reflections("org.hps.conditions");
        Set<Class<? extends ConditionsObject>> objectTypes = reflections.getSubTypesOf(ConditionsObject.class);
        for (Class<? extends ConditionsObject> objectType : objectTypes) {
            if (Modifier.isAbstract(objectType.getModifiers())) {
                continue;
            }
            if (objectType.getAnnotation(Table.class) == null) {
                continue;
            }
            MultipleCollectionsAction multipleCollectionsAction = MultipleCollectionsAction.ERROR;
            Class<?> converterClass = null;
            Converter converterAnnotation = objectType.getAnnotation(Converter.class);
            if (converterAnnotation != null) {
                multipleCollectionsAction = converterAnnotation.multipleCollectionsAction();
                if (!converterAnnotation.converter().equals(ConditionsObjectConverter.class)) {
                    converterClass = converterAnnotation.converter();
                }
            }
            
            final Class<? extends AbstractConditionsObjectCollection<? extends ConditionsObject>> collectionType = 
                    ConditionsObjectUtilities.getCollectionType(objectType);
            
            ConditionsObjectConverter converter = null;
            if (converterClass == null) {
                converter = new ConditionsObjectConverter() {
                    @Override
                    public Class getType() {
                        return collectionType;
                    }
                };
            } else {
                try {
                    Object object = converterClass.newInstance();
                    if (!(object instanceof ConditionsObjectConverter)) {
                        throw new RuntimeException("The Converter has the wrong type: " + object.getClass().getCanonicalName());
                    }
                    converter = (ConditionsObjectConverter) object;
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            converter.setMultipleCollectionsAction(multipleCollectionsAction);
            registry.put(converter.getType(), converter);
        }        
        return registry;
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Entry<Class<? extends ConditionsObject>, ConditionsObjectConverter> entry : entrySet()) {
            buff.append(entry.getValue().toString());
        }
        return buff.toString();
    }
}
