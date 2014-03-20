package org.hps.conditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

/**
 * This class loads an XML configuration of conditions table meta data.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class ConditionsTableMetaDataXMLLoader {
    
    List<ConditionsTableMetaData> _tableDataList = null;
        
    @SuppressWarnings("unchecked")
    /**
     * This method expects an XML element containing child "table" elements.
     * @param element
     */
    void load(Element element) {
        
        _tableDataList = new ArrayList<ConditionsTableMetaData>();
        
        for (Iterator<?> iterator = element.getChildren("table").iterator(); iterator.hasNext();) {
            Element tableElement = (Element)iterator.next();
            String tableName = tableElement.getAttributeValue("name");
            
            //System.out.println("tableName: " + tableName);
            
            Element classesElement = tableElement.getChild("classes");
            Element classElement = classesElement.getChild("object");
            Element collectionElement = classesElement.getChild("collection");
            
            String className = classElement.getAttributeValue("class");
            String collectionName = collectionElement.getAttributeValue("class");
            
            //System.out.println("className: " + className);
            //System.out.println("collectionName: " + collectionName);
            
            Class<? extends ConditionsObject> objectClass;
            Class<?> rawObjectClass;
            try {
                rawObjectClass = Class.forName(className);
                //System.out.println("created raw object class: " + rawObjectClass.getSimpleName());
                if (!ConditionsObject.class.isAssignableFrom(rawObjectClass)) {
                    throw new RuntimeException("The class " + rawObjectClass.getSimpleName() + " does not extend ConditionsObject.");
                }
                objectClass = (Class<? extends ConditionsObject>)rawObjectClass;
                //System.out.println("created ConditionsObject class: " + objectClass.getSimpleName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }            
            
            Class<? extends ConditionsObjectCollection<?>> collectionClass;
            Class<?> rawCollectionClass;
            try {
                rawCollectionClass = Class.forName(collectionName);
                //System.out.println("created raw collection class: " + rawCollectionClass.getSimpleName());
                if (!ConditionsObjectCollection.class.isAssignableFrom(rawCollectionClass))
                    throw new RuntimeException("The class " + rawCollectionClass.getSimpleName() + " does not extend ConditionsObjectCollection.");
                collectionClass = (Class<? extends ConditionsObjectCollection<?>>)rawCollectionClass;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            
            ConditionsTableMetaData tableData = new ConditionsTableMetaData(tableName, objectClass, collectionClass);
            
            Element fieldsElement = tableElement.getChild("fields");
            
            for (Iterator<?> fieldsIterator = fieldsElement.getChildren("field").iterator(); fieldsIterator.hasNext();) {
                Element fieldElement = (Element)fieldsIterator.next();
                
                String fieldName = fieldElement.getAttributeValue("name");                                
                //System.out.println("field: " + fieldName);
                
                tableData.addField(fieldName);
            }
            
            _tableDataList.add(tableData);
            
            //System.out.println();
        }                      
    }    
    
    List<ConditionsTableMetaData> getTableMetaDataList() {
        return _tableDataList;
    }
    
    ConditionsTableMetaData findTableMetaData(String name) {
        for (ConditionsTableMetaData metaData : _tableDataList) {
            if (metaData.getTableName().equals(name))
                return metaData;
        }
        return null;
    }
}
