package org.hps.detector.ecal;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElementStore;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.GeometryReader;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;

public class HPSEcalAPITest extends TestCase {
 
    private HPSEcalDetectorElement api = null;
    private HPSEcal3 ecal = null;
    
    public void setUp() throws Exception {
        final GeometryReader geometryReader = new GeometryReader();
        final InputStream in = this.getClass().getResourceAsStream("/org/lcsim/geometry/subdetector/HPSEcal3Test.xml");
        final Detector detector = geometryReader.read(in);
        ecal = (HPSEcal3) detector.getSubdetector("Ecal");
        api = (HPSEcalDetectorElement) ecal.getDetectorElement();
    }
    
    public void testIdentifiers() {
        IIdentifierHelper helper = api.getIdentifierHelper();
        for (EcalCrystal crystal : api.getCrystals()) {
            IExpandedIdentifier expandedId = helper.createExpandedIdentifier();
            expandedId.setValue(helper.getFieldIndex("ix"), crystal.getX());
            expandedId.setValue(helper.getFieldIndex("iy"), crystal.getY());
            expandedId.setValue(helper.getFieldIndex("system"), api.getSystemID());
            IIdentifier id = helper.pack(expandedId);
            if (id.getValue() != crystal.getIdentifier().getValue()) {
                throw new RuntimeException("Reencoded ID " + id.getValue() + " does not match crystal ID " + crystal.getIdentifier().getValue());
            }            
            
            IDetectorElementStore deStore = DetectorElementStore.getInstance();           
            if (deStore.find(crystal.getIdentifier()).size() == 0) {
                throw new RuntimeException("Failed to find crystal ID in store.");
            } 
            
            if (deStore.find(id).size() == 0) {
                throw new RuntimeException("Failed to find repacked ID in store.");
            } 
        }        
    }
    
    public void testNeighborMap() {
        NeighborMap neighborMap = ecal.getNeighborMap();
        for (EcalCrystal crystal : api.getCrystals()) {
            List<EcalCrystal> neighborCrystals = api.getNeighbors(crystal);
            if (neighborCrystals.size() == 0) {
                throw new RuntimeException("Crystal has 0 neighbors.");
            }
            if (!neighborMap.containsKey(crystal.getIdentifier().getValue())) {
                throw new RuntimeException("Neighbor map does not contain ID.");
            }
            Set<Long> neighborIds = neighborMap.get(crystal.getIdentifier().getValue());
            for (long id : neighborIds) {
                boolean foundId = false;
                for (EcalCrystal neighborCrystal : neighborCrystals) {
                    if (neighborCrystal.getIdentifier().getValue() == id) {
                        foundId = true;
                        break;
                    } 
                }
                if (!foundId) {
                    throw new RuntimeException("Failed to find neighbor ID in map for " + crystal.getName());
                }
            }
        }
        
    }
    
    public void testHPSEcalAPI() throws Exception {

        assertEquals("The max X index is wrong.", 23, api.getXIndexMax());
        assertEquals("The min X index is wrong.", -23, api.getXIndexMin());
        assertEquals("The max Y index is wrong.", 5, api.getYIndexMax());
        assertEquals("The min Y index is wrong.", -5, api.getYIndexMin());

        for (final Integer yIndex : api.getYIndices()) {
            if (yIndex == 0) {
                continue;
            }
            for (final Integer xIndex : api.getXIndices()) {
                if (xIndex == 0) {
                    continue;
                }
                if ((yIndex == 1 || yIndex == -1) && xIndex <= -2 && xIndex >= -10) {
                    assertTrue("Indices should be in gap: " + xIndex + ", " + yIndex, api.isInBeamGap(xIndex, yIndex));
                    // Crystal is in the beam gap.
                    continue;
                }
                final EcalCrystal crystal = api.getCrystal(xIndex, yIndex);
                assertNotNull("Failed to find crystal at ix = " + xIndex + ", iy = " + yIndex, crystal);
            }
        }

        for (final Integer yIndex : api.getYIndices()) {
            final List<EcalCrystal> row = api.getRow(yIndex);
            if (Math.abs(yIndex) != 1) {
                assertEquals("Wrong number of crystals in row.", 46, row.size());
            } else {
                assertEquals("Wrong number of crystals in row.", 37, row.size());
            }
        }

        for (final Integer xIndex : api.getXIndices()) {
            final List<EcalCrystal> column = api.getColumn(xIndex);
            if (xIndex > -2 || xIndex < -10) {
                assertEquals("Wrong number of crystals in column.", 10, column.size());
            } else {
                assertEquals("Wrong number of crystals in column.", 8, column.size());
            }
        }
    }
}
