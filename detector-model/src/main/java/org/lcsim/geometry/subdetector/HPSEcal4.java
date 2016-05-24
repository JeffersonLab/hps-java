package org.lcsim.geometry.subdetector;

import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * Reconstruction version of HPS ECal with crystal array.
 */
public class HPSEcal4 extends HPSEcal3 {

    HPSEcal4(Element node) throws JDOMException {
        super(node);
    }
}