package org.lcsim.geometry.subdetector;

import hep.graphics.heprep.HepRep;
import hep.graphics.heprep.HepRepFactory;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.detector.converter.heprep.DetectorElementToHepRepConverter;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: HPSMuonCalorimeter.java,v 1.2 2013/01/25 00:13:14 jeremy Exp $
 */
public class HPSMuonCalorimeter extends AbstractSubdetector 
{
    HPSMuonCalorimeter(Element node) throws JDOMException 
    {
        super(node);
    }
    
    public void appendHepRep(HepRepFactory factory, HepRep heprep) 
    {
        DetectorElementToHepRepConverter.convert(
                getDetectorElement(), 
                factory, 
                heprep, 
                -1, 
                false, 
                getVisAttributes().getColor());
    }
}