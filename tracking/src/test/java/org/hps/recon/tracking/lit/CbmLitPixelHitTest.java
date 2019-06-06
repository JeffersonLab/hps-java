package org.hps.recon.tracking.lit;
import junit.framework.TestCase;

/**
 *
 * @author Norman A Graf
 * 
 *  @version $Id:
 */
public class CbmLitPixelHitTest extends TestCase {
    
    boolean debug = true;

    public void testCbmLitPixelHit()
    {
        CbmLitPixelHit hit = new CbmLitPixelHit();
        double x = 1.;
        double y = 2.;
        double z = 3.;
        double dx = .001;
        double dy = .002;
        double dxdy = .003;
        double dz = .004;
        hit.SetX(x);
        hit.SetY(y);
        hit.SetZ(z);
        hit.SetDx(dx);
        hit.SetDy(dy);
        hit.SetDxy(dxdy);
        hit.SetDz(dz);
        if(debug) System.out.println(hit);
        assertTrue(hit.GetX()==x);
        assertTrue(hit.GetY()==y);
        assertTrue(hit.GetZ()==z);
        assertTrue(hit.GetDx()==dx);
        assertTrue(hit.GetDy()==dy);
        assertTrue(hit.GetDz()==dz);
        assertTrue(hit.GetDxy()==dxdy);
        
    }

}
