/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.lit;

import java.util.List;
import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;

/**
 *
 * @author ngraf
 */
public class HpsLitTrackFitterTest extends TestCase
{

    public void testIt() throws Exception
    {
        // generate a dummy detector
        HpsDetector hpsDet = newDetector();
        // create a simple magnetic field
        CbmLitField field = new ConstantMagneticField(0., -0.24, 0.);
        // create an extrapolator...
        CbmLitRK4TrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);
        // temporary track parameter for reuse
        CbmLitTrackParam parOut = new CbmLitTrackParam();
        CbmLitTrackParam parOut2 = new CbmLitTrackParam();

        // now the magnetic field map
        System.setProperty("disableSvtAlignmentConstants", "1");
        String detectorName = "HPS-EngRun2015-Nominal-v2-fieldmap";
        DatabaseConditionsManager cm = DatabaseConditionsManager.getInstance();
        cm.setDetector(detectorName, 0);
        Detector det = cm.getDetectorObject();

        FieldMap map = det.getFieldMap();
        //HpsMagField field = new HpsMagField()
        //the track to extrapolate
        // pos=(0.0,0.0,0.0) tx=0.05798555141702475 ty=0.03146740980528227 qp=-0.9469697640849427
        CbmLitTrackParam par = new CbmLitTrackParam();
        par.SetQp(-0.9469697640849427);
        par.SetTx(0.05798555141702475);
        par.SetTy(0.03146740980528227);
        List<DetectorPlane> struckPlanes = hpsDet.getPlanes();
        int j = 0;
        for (DetectorPlane p : struckPlanes) {
            LitStatus stat = extrap.Extrapolate(par, parOut, p, null);
            LitStatus stat2 = extrap.Extrapolate(par, parOut2, p.GetZpos(), null);
            
            System.out.println(j + " : par    " + par);
            System.out.println(j + " : parOut " + parOut);
            System.out.println(j + " : parOut2 " + parOut2);
            
            System.out.println(j + " : " + "x= " + parOut.GetX() + " y= " + parOut.GetY() + " z= " + parOut.GetZ());
            j++;
        }
    }

    HpsDetector newDetector()
    {
        double[] zees = {87.909118946011, 96.0939189018316, 187.97896366133614, 195.97552893546475, 287.9606833184293, 295.9480418112455, 486.50882817838055, 493.9921379568081, 686.3705011120256, 693.9397138855463, 889.457554538664, 896.9125606348301};
        String[] names = {"L1t_axial", "L1t_stereo", "L2t_axial", "L2t_stereo", "L3t_axial", "L3t_stereo", "L4t_axial", "L4t_stereo", "L5t_axial", "L5t_stereo", "L6t_axial", "L6t_stereo"};
        double[] phi = {3.141506045566059,
            -0.09995368832998253,
            3.141596601360339,
            -0.0994942520988551,
            3.141723085567417,
            -0.09962858546918252,
            2.518666641859735E-4,
            3.0916128359226525,
            3.957986839968619E-4,
            3.0922364767590538,
            3.141752209439217,
            -0.04930764414933564};

        double x0 = .1;
        HpsDetector det = new HpsDetector();

        for (int i = 0; i < zees.length; ++i) {
            CartesianThreeVector pos = new CartesianThreeVector(0., 0., zees[i]);
            CartesianThreeVector eta = new CartesianThreeVector(0., 0., 1.);
            DetectorPlane p = new DetectorPlane("p1", pos, eta, x0, phi[i]);
            det.addDetectorPlane(p);
        }
        return det;
    }

}
