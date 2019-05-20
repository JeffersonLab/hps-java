package org.hps.test.it;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.evio.EvioToLcio;
import org.hps.test.util.TestOutputFile;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

public class HodoscopeDataConverterTest extends TestCase {
    
    private static final String DETECTOR = "HPS-HodoscopeTest-v1";
    private static final Integer RUN_NUMBER = 1000000;
    //private static String LOCAL_FILE_PATH = "/work/slac/hps-projects/projects/hodoscope-dev/hpshodo_000322_100evts.evio";
    private static final String OUTPUT_FILE_NAME = "hodo_cnv_test.slcio";
    //    private static final String TEST_FILE_URL = "http://lcsim.org/test/hps-java/hpshodo_000322_100evts.evio";
    private static final String TEST_FILE_URL = "http://lcsim.org/test/hps-java/hpsecal_000084_200events.evio";
    private static final boolean DEBUG = true;
    
    public void testHodoscopeDataConverter() throws IOException {
        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(TEST_FILE_URL));
        File testOutput = new TestOutputFile(HodoscopeDataConverterTest.class, OUTPUT_FILE_NAME);
        String args[] = {
                "-d",
                DETECTOR,
                "-R",
                RUN_NUMBER.toString(),
                testFile.getPath(),
                "-l",
                testOutput.getPath()
        };
        EvioToLcio.main(args);
                   
        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager.getInstance();
        loop.setLCIORecordSource(testOutput);
        loop.add(new HodoscopeDataDriver());
        loop.loop(100);
    }    
    
    private class HodoscopeDataDriver extends Driver {
        
        private HodoscopeDetectorElement hodo = null;
        private IIdentifierHelper helper = null;
        
        public void detectorChanged(Detector detector) {
            Subdetector subdet = detector.getSubdetector("Hodoscope");
            this.hodo = (HodoscopeDetectorElement) subdet.getDetectorElement();
            this.helper = hodo.getIdentifierHelper();
            System.out.println("Hodoscope pixels: ");
            for (IDetectorElement de : hodo.getChildren()) {
                System.out.println(de.getName() + " " + helper.unpack(de.getIdentifier()));
            }
            System.out.println();
        }
        
        public void process(EventHeader event) {
            if (DEBUG)
                System.out.println(">>>> Processing hodo event " + event.getEventNumber());
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, "HodoReadoutHits");
            if (hits.size() == 0) {
                throw new RuntimeException("HodoReadoutHits is empty!");
            }
            if (DEBUG)
                System.out.println("HodoReadoutHits has " + hits.size() + " hits.");
            for (RawTrackerHit hit : hits) {
                IIdentifier id = hit.getIdentifier();
                IExpandedIdentifier vals = helper.unpack(id);
                if (DEBUG)
                    System.out.println(vals);
                ExpandedIdentifier vals_no_hole = new ExpandedIdentifier(vals.size());
                for (int i = 0; i < vals.size() - 1; i++) {
                    vals_no_hole.setValue(i, vals.getValue(i));
                }
                if (DEBUG)
                    System.out.println(vals_no_hole + " (no hole)");
                IIdentifier id_no_hole = helper.pack(vals_no_hole);
                // This will crash if no pixel is found but that is fine.  --JM
                hit.setDetectorElement(this.hodo.findDetectorElement(id_no_hole).get(0));
                if (DEBUG) {
                    System.out.println("Hodo hit " + id + " assigned to pixel " + hit.getDetectorElement().getName());
                    System.out.println();
                }
            }
        }
    }    
}
