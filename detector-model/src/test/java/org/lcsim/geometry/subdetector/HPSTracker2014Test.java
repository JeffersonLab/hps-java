package org.lcsim.geometry.subdetector;

import java.io.InputStream;

import junit.framework.TestCase;

import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IDetectorElementStore;
import org.lcsim.detector.PhysicalVolumePath;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.GeometryReader;

public class HPSTracker2014Test extends TestCase {
    
    Detector det;
    public HPSTracker2014Test(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        GeometryReader geometryReader = new GeometryReader();
        geometryReader.setBuildDetailed(true);
        String pathToCompactFile = "/org/lcsim/geometry/subdetector/HPSTracker2014.xml";

        InputStream in = HPSTracker2014Test.class.getResourceAsStream(pathToCompactFile);
        det = geometryReader.read(in);
        
        System.out.printf("%s: detector name converted: %s\n",this.getClass().getSimpleName(), det.getName());               
    }
    
    public void test() {               
        IDetectorElementStore store =  DetectorElementStore.getInstance();
        System.out.printf("%s: Printing %d DE:\n",this.getClass().getSimpleName(), store.size());
        System.out.printf("%s: %50s %40s %50s %50s %s\n",this.getClass().getSimpleName(), "name", "pos", "path","mother", "expId");
        for(IDetectorElement e : store) {
            Identifier id = (Identifier) e.getIdentifier();
            IExpandedIdentifier expId = null;
            if(id.getGarbage()==false)
                expId = e.getExpandedIdentifier();
            System.out.printf("%s: %50s %40s %50s %50s %s\n",this.getClass().getSimpleName(), e.getName(),e.hasGeometryInfo()?e.getGeometry().getPosition().toString():" - ",e.hasGeometryInfo()?((PhysicalVolumePath)e.getGeometry().getPath()).toString():" - ",e.getParent()==null?" - ":e.getParent().getName(),expId==null?" no expId ":expId.toString());
        }
    }
   

}