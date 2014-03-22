package org.hps.conditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.lcsim.conditions.ConditionsConverter;

/**
 * This class reads in an XML configuration specifying a list of converter classes,
 * e.g. from the config file for the {@link DatabaseConditionsManager}.
 *  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsConverterLoader {
    
    List<ConditionsConverter> _converterList;
    
    void load(Element element) {
        _converterList = new ArrayList<ConditionsConverter>();
        for (Iterator iterator = element.getChildren("converter").iterator(); iterator.hasNext(); ) {
            Element converterElement = (Element)iterator.next();
            try {
                Class converterClass = Class.forName(converterElement.getAttributeValue("class"));
                if (ConditionsConverter.class.isAssignableFrom(converterClass)) {
                    try {
                        //System.out.println("adding converter: " + converterClass.getSimpleName());
                        _converterList.add((ConditionsConverter)converterClass.newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("The converter class " + converterClass.getSimpleName() + " does not extend the correct base type.");
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    List<ConditionsConverter> getConverterList() {
        return _converterList;
    }
}
