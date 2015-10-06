package org.lcsim.detector.converter.compact;

import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.GeometryReader;

public class HPSMuonCalorimeterTest extends TestCase 
{
    
    Detector detector = null;
    private static final String resource = "/org/lcsim/geometry/subdetector/HPSMuonCalorimeterTest.xml";
    
    public static Test suite() 
    {
        return new TestSuite(HPSMuonCalorimeterTest.class);
    }

    public void setUp() 
    {
        InputStream in = this.getClass().getResourceAsStream(resource);
        GeometryReader reader = new GeometryReader();
        try {
            detector = reader.read(in);
        } catch (Throwable x) {
            throw new RuntimeException(x);
        }
    }
    
    public void testMuon() 
    {
        IDetectorElement de = detector.getSubdetector("MUON").getDetectorElement();
        System.out.println("MUON has " + de.getChildren().size() + " children.");
        IIdentifierHelper helper = de.getIdentifierHelper();
        IIdentifierDictionary dict = helper.getIdentifierDictionary();        
        System.out.println(dict.toString());
        for (IDetectorElement child : de.getChildren()) {
            System.out.println(child.getName() + " => " + helper.unpack(child.getIdentifier()));
        }
    }
}