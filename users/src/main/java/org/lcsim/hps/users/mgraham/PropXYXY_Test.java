/*
 * PropXYXY_Test.java
 *
 * Created on July 24, 2007, 10:15 PM
 *
 * $Id: PropXYXY_Test.java,v 1.2 2011/07/07 20:57:39 mgraham Exp $
 */

package org.lcsim.hps.users.mgraham;

//import junit.framework.TestCase;
import org.lcsim.recon.tracking.spacegeom.SpacePath;
import org.lcsim.recon.tracking.spacegeom.SpacePoint;
import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackDerivative;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.recon.tracking.trfxyp.PropXYXY;
import org.lcsim.recon.tracking.trfxyp.SurfXYPlane;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman Graf
 */
public class PropXYXY_Test extends Driver
{
    private boolean debug=true;
    /** Creates a new instance of PropXYXY_Test */
    public void testPropXYXY()
    {
        String ok_prefix = "PropXYXY (I): ";
        String error_prefix = "PropXYXY test (E): ";
        
        if(debug) System.out.println( ok_prefix
                + "-------- Testing component PropXYXY. --------" );
        
        
        //********************************************************************
        
        if(debug) System.out.println( ok_prefix + "Test constructor." );
        PropXYXY prop = new PropXYXY(0.5);
        if(debug) System.out.println( prop );
        
        //********************************************************************
        
        // Here we propagate some tracks both forward and backward and then
        // each back to the original track.  We check that the returned
        // track parameters match those of the original track.
        if(debug) System.out.println( ok_prefix + "Check reversibility." );
        
        double u1[]  ={10.,      10.,     10.,     10.,     10.,     10.,10.,10.};
        double u2[]  ={10.,      10.,     10.,     10.,     10.,     10.,11.,11.};
//        double phi1[]={  Math.PI/2.,    Math.PI/2.,   Math.PI/2.,   Math.PI/2.,   Math.PI/2.,   Math.PI/2., Math.PI/2.,0};
//        double phi2[]={5*Math.PI/6.,  5*Math.PI/6., 5*Math.PI/6., 5*Math.PI/6., 5*Math.PI/6., 5*Math.PI/6., 5*Math.PI/6.,0};
        double phi1[]={  Math.PI/2.,    Math.PI/2.,   Math.PI/2.,   Math.PI/2.,   Math.PI/2.,   Math.PI/2., 0,0};
        double phi2[]={5*Math.PI/6.,  5*Math.PI/6., 5*Math.PI/6., 5*Math.PI/6., 5*Math.PI/6., 5*Math.PI/6., 0,0};
        int sign_du[]={  1,       -1,      -1,       1,       1,       1 ,  1  ,1};
        double v[]   ={ -2.,       2.,     40.,     40.,     -2.,     -2.,  2. ,2 };
        double z[]   ={  3.,       3.,      3.,      3.,      3.,      3.,  3. ,3};
        double dvdu[]={-1.5,     -1.5,     1.5,     1.5,    -1.5,      0.,  1.5 ,1.5 };
        double dzdu[]={ 2.3,     -2.3,    -2.3,     2.3,     0.,       2.3,  2.3   ,-2.3};
        double qp[]  ={ 0.05,    -0.05,    0.05,   -0.05,    0.05,     0.05, 0.05  ,0.5};
        
        double maxdiff = 1.e-7;
        int ntrk = 8;
        int i;
        for ( i=0; i<ntrk; ++i )
        {
            if(debug) System.out.println( "********** Propagate track " + i + ". **********" );
            PropStat pstat = new PropStat();
            SurfXYPlane sxyp1 = new SurfXYPlane(u1[i],phi1[i]);
            SurfXYPlane sxyp2= new SurfXYPlane(u2[i],phi2[i]);
            TrackVector vec1 = new TrackVector();
            vec1.set(SurfXYPlane.IV     ,v[i]);     // v
            vec1.set(SurfXYPlane.IZ     ,z[i]);     // z
            vec1.set(SurfXYPlane.IDVDU  ,dvdu[i]);  // dv/du
            vec1.set(SurfXYPlane.IDZDU  ,dzdu[i]);  // dz/du
            vec1.set(SurfXYPlane.IQP    ,qp[i]);    //  q/p
            
            VTrack trv1 = new VTrack(sxyp1.newPureSurface(),vec1);
            if (sign_du[i]==1) trv1.setForward();
            else trv1.setBackward();
            if(debug) System.out.println( " starting: " + trv1 );
            System.out.println("PropDir.FORWARD_MOVE");
            VTrack trv2f = new VTrack(trv1);
            pstat = prop.vecDirProp(trv2f,sxyp2,PropDir.FORWARD_MOVE);
            if(debug) System.out.println( pstat );
            if(debug) System.out.println("pstat= "+pstat);
            Assert.assertTrue( pstat.forward() );
            if(debug) System.out.println( "  forward: " + trv2f );
            Assert.assertTrue(check_dz(trv1,trv2f)>=0.);

              System.out.println("PropDir.BACKWARD");
            VTrack trv2b = new VTrack(trv1);
            pstat = prop.vecDirProp(trv2b,sxyp2,PropDir.BACKWARD);
            if(debug) System.out.println( pstat );
            Assert.assertTrue( pstat.backward() );
            if(debug) System.out.println( " backward: " + trv2b );
            Assert.assertTrue(check_dz(trv1,trv2b)<=0.);

             System.out.println("PropDir.BACKWARD_MOVE");
            VTrack trv2fb = new VTrack(trv2f);
            pstat = prop.vecDirProp(trv2fb,sxyp1,PropDir.BACKWARD_MOVE);
            if(debug) System.out.println( pstat );
            Assert.assertTrue( pstat.backward() );
            if(debug) System.out.println( " f return: " + trv2fb );
            Assert.assertTrue(check_dz(trv2f,trv2fb)<=0.);
            
             System.out.println("PropDir.FORWARD");
            VTrack trv2bf = new VTrack(trv2b);
            pstat = prop.vecDirProp(trv2bf,sxyp1,PropDir.FORWARD);
            if(debug) System.out.println( pstat );
            Assert.assertTrue( pstat.forward() );
            if(debug) System.out.println( " b return: " + trv2bf );
            Assert.assertTrue(check_dz(trv2b,trv2bf)>=0.);
            
            double difff =
                    sxyp1.vecDiff(trv2fb.vector(),trv1.vector()).amax();
            double diffb =
                    sxyp1.vecDiff(trv2bf.vector(),trv1.vector()).amax();
            if(debug) System.out.println( "diffs: " + difff + ' ' + diffb );
            Assert.assertTrue( difff < maxdiff );
            Assert.assertTrue( diffb < maxdiff );
            
        }
        
        //********************************************************************
        
        // Repeat the above with errors.
        if(debug) System.out.println( ok_prefix + "Check reversibility with errors." );
        double evv[] =   {  0.01,   0.01,   0.01,   0.01,   0.01,   0.01  };
        double evz[] =   {  0.01,  -0.01,   0.01,  -0.01,   0.01,  -0.01  };
        double ezz[] =   {  0.25,   0.25,   0.25,   0.25,   0.25,   0.25, };
        double evdv[] =  {  0.004, -0.004,  0.004, -0.004,  0.004, -0.004 };
        double ezdv[] =  {  0.04,  -0.04,   0.04,  -0.04,   0.04,  -0.04, };
        double edvdv[] = {  0.01,   0.01,   0.01,   0.01,   0.01,   0.01  };
        double evdz[] =  {  0.004, -0.004,  0.004, -0.004,  0.004, -0.004 };
        double edvdz[] = {  0.004, -0.004,  0.004, -0.004,  0.004, -0.004 };
        double ezdz[] =  {  0.04,  -0.04,   0.04,  -0.04,   0.04,  -0.04  };
        double edzdz[] = {  0.02,   0.02,   0.02,   0.02,   0.02,   0.02  };
        double evqp[] =  {  0.004, -0.004,  0.004, -0.004,  0.004, -0.004 };
        double ezqp[] =  {  0.004, -0.004,  0.004, -0.004,  0.004, -0.004 };
        double edvqp[] = {  0.004, -0.004,  0.004, -0.004,  0.004, -0.004 };
        double edzqp[] = {  0.004, -0.004,  0.004, -0.004,  0.004, -0.004 };
        double eqpqp[] = {  0.01,   0.01,   0.01,   0.01,   0.01,   0.01  };
        
        maxdiff = 1.e-6;
        
        for ( i=0; i<ntrk; ++i )
        {
            if(debug) System.out.println( "********** Propagate track " + i + ". **********" );
            PropStat pstat = new PropStat();
            SurfXYPlane sxyp1 = new SurfXYPlane(u1[i],phi1[i]);
            SurfXYPlane sxyp2 = new SurfXYPlane(u2[i],phi2[i]);
            TrackVector vec1 = new TrackVector();
            vec1.set(SurfXYPlane.IV,    v[i]);     // v
            vec1.set(SurfXYPlane.IZ,    z[i]);     // z
            vec1.set(SurfXYPlane.IDVDU, dvdu[i]);  // dv/du
            vec1.set(SurfXYPlane.IDZDU, dzdu[i]);  // dz/du
            vec1.set(SurfXYPlane.IQP,   qp[i]);    //  q/p
            
            TrackError err1 = new TrackError();
            err1.set(SurfXYPlane.IV,SurfXYPlane.IV,       evv[i]);
            err1.set(SurfXYPlane.IV,SurfXYPlane.IZ,       evz[i]);
            err1.set(SurfXYPlane.IZ,SurfXYPlane.IZ,       ezz[i]);
            err1.set(SurfXYPlane.IV,SurfXYPlane.IDVDU,    evdv[i]);
            err1.set(SurfXYPlane.IZ,SurfXYPlane.IDVDU,    ezdv[i]);
            err1.set(SurfXYPlane.IDVDU,SurfXYPlane.IDVDU, edvdv[i]);
            err1.set(SurfXYPlane.IV,SurfXYPlane.IDZDU,    evdz[i]);
            err1.set(SurfXYPlane.IZ,SurfXYPlane.IDZDU,    ezdz[i]);
            err1.set(SurfXYPlane.IDVDU,SurfXYPlane.IDZDU, edvdz[i]);
            err1.set(SurfXYPlane.IDZDU,SurfXYPlane.IDZDU, edzdz[i]);
            err1.set(SurfXYPlane.IV,SurfXYPlane.IQP,      evqp[i]);
            err1.set(SurfXYPlane.IZ,SurfXYPlane.IQP,      ezqp[i]);
            err1.set(SurfXYPlane.IDVDU,SurfXYPlane.IQP,   edvqp[i]);
            err1.set(SurfXYPlane.IDZDU,SurfXYPlane.IQP,   edzqp[i]);
            err1.set(SurfXYPlane.IQP,SurfXYPlane.IQP,     eqpqp[i]);
            ETrack trv1 = new ETrack(sxyp1.newPureSurface(),vec1,err1);
            if(sign_du[i]==1) trv1.setForward();
            else trv1.setBackward();
            if(debug) System.out.println( " starting: " + trv1 );
            
            ETrack trv2f = new ETrack(trv1);
            pstat = prop.errDirProp(trv2f,sxyp2,PropDir.FORWARD);
            Assert.assertTrue( pstat.forward() );
            if(debug) System.out.println( "  forward: " + trv2f );
            ETrack trv2b = new ETrack(trv1);
            pstat = prop.errDirProp(trv2b,sxyp2,PropDir.BACKWARD);
            Assert.assertTrue( pstat.backward() );
            if(debug) System.out.println( " backward: " + trv2b );
            ETrack trv2fb = new ETrack(trv2f);
            pstat = prop.errDirProp(trv2fb,sxyp1,PropDir.BACKWARD);
            Assert.assertTrue( pstat.backward() );
            if(debug) System.out.println( " f return: " + trv2fb );
            ETrack trv2bf = new ETrack(trv2b);
            pstat = prop.errDirProp(trv2bf,sxyp1,PropDir.FORWARD);
            Assert.assertTrue( pstat.forward() );
            if(debug) System.out.println( " b return: " + trv2bf );
            double difff =
                    sxyp1.vecDiff(trv2fb.vector(),trv1.vector()).amax();
            double diffb =
                    sxyp1.vecDiff(trv2bf.vector(),trv1.vector()).amax();
            if(debug) System.out.println( "vec diffs: " + difff + ' ' + diffb );
            Assert.assertTrue( difff < maxdiff );
            Assert.assertTrue( diffb < maxdiff );
            TrackError dfb = trv2fb.error().minus(trv1.error());
            TrackError dbf = trv2bf.error().minus(trv1.error());
            double edifff = dfb.amax();
            double ediffb = dbf.amax();
            if(debug) System.out.println( "err diffs: " + edifff + ' ' + ediffb );
            Assert.assertTrue( edifff < maxdiff );
            Assert.assertTrue( ediffb < maxdiff );
        }
        
        //********************************************************************
        
        if(debug) System.out.println( ok_prefix + "Test Nearest Propagation" );
        
        PropStat pstat = new PropStat();
        SurfXYPlane sxyp1 = new SurfXYPlane(2.,Math.PI/3.);
        SurfXYPlane sxyp2 = new SurfXYPlane(3.,Math.PI/3.);
        
        TrackVector vec1 = new TrackVector();
        
        vec1.set(SurfXYPlane.IV     ,1.);     // v
        vec1.set(SurfXYPlane.IZ     ,1.);     // z
        vec1.set(SurfXYPlane.IDVDU  ,1.);     // dv/du
        vec1.set(SurfXYPlane.IDZDU  ,1.);     // dz/du
        vec1.set(SurfXYPlane.IQP    ,0.01);   //  q/p
        
        VTrack trv1 = new VTrack(sxyp1.newPureSurface(),vec1);
        trv1.setForward();
        
        if(debug) System.out.println( " starting: " + trv1 );
        VTrack trv2n = new VTrack(trv1);
        pstat = prop.vecDirProp(trv2n,sxyp2,PropDir.NEAREST);
        Assert.assertTrue( pstat.forward() );
        if(debug) System.out.println( " nearest: " + trv2n );
        
        trv1.setBackward();
        
        if(debug) System.out.println( " starting: " + trv1 );
        trv2n = new VTrack(trv1);
        pstat = prop.vecDirProp(trv2n,sxyp2,PropDir.NEAREST);
        Assert.assertTrue( pstat.backward() );
        if(debug) System.out.println( " nearest: " + trv2n );
        
        //********************************************************************
        
        if(debug) System.out.println( ok_prefix + "Test XXX_MOVE and Same Surface Propagation." );
        
        VTrack trvt0 = new VTrack(sxyp1.newPureSurface(),vec1);
        trvt0.setForward();
        VTrack trvt1 = new VTrack(trvt0);
        PropStat tst = prop.vecDirProp(trvt1,sxyp1,PropDir.NEAREST);
        Assert.assertTrue( tst.success() && trvt1.equals(trvt0) );
        tst = prop.vecDirProp(trvt1,sxyp1,PropDir.FORWARD);
        Assert.assertTrue( tst.success() && trvt1.equals(trvt0) );
        tst = prop.vecDirProp(trvt1,sxyp1,PropDir.BACKWARD);
        Assert.assertTrue( tst.success() && trvt1.equals(trvt0) );
        
        trvt1 =  new VTrack(trvt0);
        tst = prop.vecDirProp(trvt1,sxyp1,PropDir.NEAREST_MOVE);
        Assert.assertTrue( tst.success() && !trvt1.equals(trvt0) );
        trvt1 = new VTrack(trvt0);
        tst = prop.vecDirProp(trvt1,sxyp1,PropDir.FORWARD_MOVE);
        Assert.assertTrue( tst.success() && !trvt1.equals(trvt0) );
        trvt1 = new VTrack(trvt0);
        tst = prop.vecDirProp(trvt1,sxyp1,PropDir.BACKWARD_MOVE);
        Assert.assertTrue( tst.success() && !trvt1.equals(trvt0) );
        
        //********************************************************************
        
        if(debug) System.out.println( ok_prefix + "Test Zero B field propagation" );
        {
            
            PropXYXY prop0 = new PropXYXY(0.);
            if(debug) System.out.println( prop0 );
            Assert.assertTrue( prop0.bField() == 0. );
            
            double u=10.,phi=0.;
            Surface srf = new SurfXYPlane(u,phi);
            VTrack trv0 = new VTrack(srf);
            TrackVector vec = new TrackVector();
            vec.set(SurfXYPlane.IV, 2.);
            vec.set(SurfXYPlane.IZ, 10.);
            vec.set(SurfXYPlane.IDVDU, 4.);
            vec.set(SurfXYPlane.IDZDU, 2.);
            trv0.setVector(vec);
            trv0.setForward();
            u=4.;
            
            Surface srf_to = new SurfXYPlane(u,phi);
            
            VTrack trv = new VTrack(trv0);
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.FORWARD);
            Assert.assertTrue( !pstat.success() );
            
            trv = new VTrack(trv0);
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.BACKWARD);
            Assert.assertTrue( pstat.success() );
            
            trv = new VTrack(trv0);
            trv.setBackward();
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.BACKWARD);
            Assert.assertTrue( !pstat.success() );
            
            trv = new VTrack(trv0);
            trv.setBackward();
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.FORWARD);
            Assert.assertTrue( pstat.success() );
            
            trv = new VTrack(trv0);
            trv.setForward();
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.NEAREST);
            Assert.assertTrue( pstat.success() );
            
            Assert.assertTrue( pstat.backward() );
            Assert.assertTrue( trv.vector(SurfXYPlane.IDVDU) == trv0.vector(SurfXYPlane.IDVDU) );
            Assert.assertTrue( trv.vector(SurfXYPlane.IDZDU) == trv0.vector(SurfXYPlane.IDZDU) );
            Assert.assertTrue(trv.surface().pureEqual(srf_to));
            
            check_zero_propagation(trv0,trv,pstat);
            
            srf_to = new SurfXYPlane(4.,Math.PI/16.);
            trv = new VTrack(trv0);
            trv.setForward();
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.NEAREST);
            Assert.assertTrue( pstat.success() );
            check_zero_propagation(trv0,trv,pstat);
            
            srf_to = new SurfXYPlane(14.,Math.PI/4.);
            trv = new VTrack(trv0);
            trv.setForward();
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.NEAREST);
            Assert.assertTrue( pstat.success() );
            check_zero_propagation(trv0,trv,pstat);
            
            srf_to = new SurfXYPlane(14.,Math.PI/2.);
            trv = new VTrack(trv0);
            trv.setForward();
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.NEAREST);
            Assert.assertTrue( pstat.success() );
            check_zero_propagation(trv0,trv,pstat);
            
            srf_to = new SurfXYPlane(14.,Math.PI);
            trv = new VTrack(trv0);
            trv.setForward();
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.NEAREST);
            Assert.assertTrue( pstat.success() );
            check_zero_propagation(trv0,trv,pstat);
            
            srf_to = new SurfXYPlane(14.,Math.PI*5./4.);
            trv = new VTrack(trv0);
            trv.setSurface(new SurfXYPlane(14.,Math.PI*7./4.));
            trv.setForward();
            VTrack tmp = new VTrack(trv);
            VTrack der = new VTrack(trv);
            pstat = prop0.vecDirProp(trv,srf_to,PropDir.NEAREST);
            Assert.assertTrue( pstat.success() );
            check_zero_propagation(tmp,trv,pstat);
            check_derivatives(prop0,der,srf_to);
            
        }
        
        
        //********************************************************************
        
        if(debug) System.out.println( ok_prefix + "Test cloning." );
        Assert.assertTrue( prop.newPropagator() != null);
        
        //********************************************************************
        
        if(debug) System.out.println( ok_prefix + "Test the field." );
        Assert.assertTrue( prop.bField() == 2.0 );
        
        //********************************************************************
        
        {
            if(debug) System.out.println("===========================================\n");
            PropXYXY p1 = new PropXYXY(2.);
            PropXYXY p2 = new PropXYXY(-2.);
            SurfXYPlane xy1 = new SurfXYPlane(5.,0.);
            SurfXYPlane xy2 = new SurfXYPlane(3.,0.);
            ETrack tre0 = new ETrack( xy2.newSurface() );
            TrackVector vec = new TrackVector();
            TrackError err = new TrackError();
            
            vec.set(0, 0.);
            vec.set(1, 0.);
            vec.set(2, 0.0);
            vec.set(3, 0.1);
            vec.set(4, -0.1);
            err.set(0,0, 0.1);
            err.set(1,1, 0.1);
            err.set(2,2, 0.1);
            err.set(3,3, 0.1);
            err.set(4,4, 0.1);
            tre0.setVector(vec);
            tre0.setError(err);
            tre0.setBackward();
            
            ETrack tre1 = new ETrack(tre0);
            ETrack tre2 = new ETrack(tre0);
            PropStat pstat2=p1.errDirProp(tre1,xy1,PropDir.BACKWARD);
            Assert.assertTrue(pstat2.success());
            pstat2=p2.errDirProp(tre2,xy1,PropDir.BACKWARD);
            Assert.assertTrue(pstat2.success());
            
            if(debug) System.out.println("tre1= \n"+tre1+'\n'+"tre2= \n"+tre2+'\n');
            if(debug) System.out.println("===========================================\n");
            
            
            
            
            
            
            //********************************************************************
            
            if(debug) System.out.println( ok_prefix
                    + "------------- All tests passed. -------------" );
        }
    }
    
    static void  check_zero_propagation(  VTrack trv0,  VTrack trv,  PropStat  pstat)
    {
        
        SpacePoint sp = trv.spacePoint();
        SpacePoint sp0 = trv0.spacePoint();
        
        SpacePath sv = trv.spacePath();
        SpacePath sv0 = trv0.spacePath();
        
        Assert.assertTrue( Math.abs(sv0.dx() - sv.dx())<1e-7 );
        Assert.assertTrue( Math.abs(sv0.dy() - sv.dy())<1e-7 );
        Assert.assertTrue( Math.abs(sv0.dz() - sv.dz())<1e-7 );
        
        double x0 = sp0.x();
        double y0 = sp0.y();
        double z0 = sp0.z();
        double x1 = sp.x();
        double y1 = sp.y();
        double z1 = sp.z();
        
        double dx = sv.dx();
        double dy = sv.dy();
        double dz = sv.dz();
        
        double prod = dx*(x1-x0)+dy*(y1-y0)+dz*(z1-z0);
        double moda = Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0) + (z1-z0)*(z1-z0));
        double modb = Math.sqrt(dx*dx+dy*dy+dz*dz);
        double st = pstat.pathDistance();
        Assert.assertTrue( Math.abs(prod-st) < 1.e-7 );
        Assert.assertTrue( Math.abs(Math.abs(prod) - moda*modb) < 1.e-7 );
    }
    
    static void check_derivatives(  Propagator prop,  VTrack trv0,  Surface srf)
    {
        for(int i=0;i<4;++i)
            for(int j=0;j<4;++j)
                check_derivative(prop,trv0,srf,i,j);
    }
    
    static void check_derivative(  Propagator  prop,  VTrack  trv0,  Surface  srf,int i,int j)
    {
        
        double dx = 1.e-3;
        VTrack trv = new VTrack(trv0);
        TrackVector vec = trv.vector();
        boolean forward = trv0.isForward();
        
        VTrack trv_0 = new VTrack(trv0);
        TrackDerivative der = new TrackDerivative();
        PropStat pstat = prop.vecProp(trv_0,srf,der);
        Assert.assertTrue(pstat.success());
        
        TrackVector tmp= new TrackVector(vec);
        tmp.set(j, tmp.get(j)+dx);
        trv.setVector(tmp);
        if(forward) trv.setForward();
        else trv.setBackward();
        
        VTrack trv_pl = new VTrack(trv);
        pstat = prop.vecProp(trv_pl,srf);
        Assert.assertTrue(pstat.success());
        
        TrackVector vecpl = trv_pl.vector();
        
        tmp= new TrackVector(vec);
        tmp.set(j,tmp.get(j)-dx);
        trv.setVector(tmp);
        if(forward) trv.setForward();
        else trv.setBackward();
        
        VTrack trv_mn = new VTrack(trv);
        pstat = prop.vecProp(trv_mn,srf);
        Assert.assertTrue(pstat.success());
        
        TrackVector vecmn = trv_mn.vector();
        
        double dy = (vecpl.get(i)-vecmn.get(i))/2.;
        
        double dydx = dy/dx;
        
        double didj = der.get(i,j);
        
        if( Math.abs(didj) > 1e-10 )
            Assert.assertTrue( Math.abs((dydx - didj)/didj) < 1e-4 );
        else
            Assert.assertTrue( Math.abs(dydx) < 1e-4 );
    }
    //**********************************************************************
    //Checker of correct z propagation
    
    static double check_dz(VTrack trv1, VTrack trv2)
    {
        double z1    = trv1.vector().get(SurfXYPlane.IZ);
        double z2    = trv2.vector().get(SurfXYPlane.IZ);
        double dzdu  = trv1.vector().get(SurfXYPlane.IDZDU);
        int sign_du = (trv1.isForward())?1:-1;
        return (z2-z1)*dzdu*sign_du;
    }
}
