package org.hps.recon.tracking.lit;

import java.util.HashSet;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ngraf
 */
public class CbmLitMaterialInfoTest extends TestCase {

    public void testIt() {
        double fLength = 0.320; // Length of the material [cm]
        double fRL = 9.370; // Radiation length [cm]
        double fRho = 2.329; // Density [g/cm^3]
        double fZ = 14; // Atomic number
        double fA = 28.0855; // Atomic mass
        double fZpos = 100.; // Z position of the material
        String fName = "Silicon"; // Name of material

        CbmLitMaterialInfo silicon = new CbmLitMaterialInfo(fName, fZpos, fLength, fRho, fRL, fZ, fA);
        assertEquals(silicon.GetLength(), fLength);
        assertEquals(silicon.GetRL(), fRL);
        assertEquals(silicon.GetRho(), fRho);
        assertEquals(silicon.GetZ(), fZ);
        assertEquals(silicon.GetA(), fA);
        assertEquals(silicon.GetZpos(), fZpos);
        assertEquals(silicon.GetName(), fName);
        System.out.println(silicon);
        
        CbmLitMaterialInfo defaultSilicon = CbmLitMaterialInfo.getSilicon();
        System.out.println(defaultSilicon);
    }

}
