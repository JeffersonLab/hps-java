package org.hps.recon.tracking.lit;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.TrackSurfaceDirection;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfzp.PropZZRK;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class ExtrapolatorTest extends TestCase
{

    double bY = -0.24;
    double[] zPlanes = {
        87.921,
        96.106,
        187.92,
        196.11,
        288.04,
        296.03,
        488.23,
        495.70,
        688.20,
        695.89,
        888.54,
        895.98
    };
    double x = 0.2565540552139282;
    double y = -0.005957666784524918;
    double z = 0.0;
    double tx = 0.03616011955965932;
    double ty = 0.03564583106361511;
    double qP = -0.896594726626001;

    public void testIt()
    {
        cbm();
        trf();
    }

    public void cbm()
    {
        System.out.println("testing it!");

        ConstantMagneticField field = new ConstantMagneticField(0., bY, 0.);
        CbmLitTrackParam p = new CbmLitTrackParam();
        p.SetX(0.2565540552139282);
        p.SetY(-0.005957666784524918);
        p.SetZ(0.0);
        p.SetTx(0.03616011955965932);
        p.SetTy(0.03564583106361511);
        p.SetQp(-0.896594726626001);

        CbmLitRK4TrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);

        CbmLitTrackParam parOut = new CbmLitTrackParam();
        for (int i = 0; i < zPlanes.length; ++i) {
            extrap.Extrapolate(p, parOut, zPlanes[i], null);
            System.out.println("extrap to z= " + zPlanes[i] + " " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
        }

    }

    public void trf()
    {
        org.lcsim.recon.tracking.magfield.ConstantMagneticField field = new org.lcsim.recon.tracking.magfield.ConstantMagneticField(0., -bY / 100., 0.);
        PropZZRK prop = new PropZZRK(field);
        List<SurfZPlane> planes = new ArrayList<SurfZPlane>();
        for (int i = 0; i < zPlanes.length; ++i) {
            planes.add(new SurfZPlane(zPlanes[i]));
        }
        TrackVector vec1 = new TrackVector();
        vec1.set(0, x);    // x
        vec1.set(1, y);    // y
        vec1.set(2, tx);   // dx/dz
        vec1.set(3, ty);   // dy/dz
        vec1.set(4, qP);   // q/p
        // create a VTrack at the origin.

        SurfZPlane zp0 = new SurfZPlane(0.);
        TrackSurfaceDirection tdir = TrackSurfaceDirection.TSD_FORWARD;
        VTrack trv0 = new VTrack(zp0.newPureSurface(), vec1, tdir);

        VTrack trv1 = new VTrack(trv0);
        System.out.println(" starting: " + trv1);
        PropDir dir = PropDir.FORWARD;
        for (int i = 0; i < zPlanes.length; ++i) {
            PropStat pstat = prop.vecDirProp(trv1, planes.get(i), dir);
            System.out.println(" ending: " + trv1);
            System.out.println(pstat);
        }

    }
}
