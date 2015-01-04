package org.hps.recon.vertexing;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.hps.recon.tracking.BeamlineConstants;
import org.hps.recon.tracking.HPSTrack;
import org.hps.recon.tracking.HelixConverter;
import org.hps.recon.tracking.StraightLineTrack;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.swim.Helix;

/**
 * 
 * Class that vertexes two tracks taking into account fringe field
 * 
 * @author phansson
 *
 */
public class TwoTrackFringeVertexer extends TwoTrackVertexer {
    protected HelixConverter converter = new HelixConverter(0.);
    
    public void setTracks(Track track1, Track track2) {
    	SeedTrack s1 = (SeedTrack) track1;
        HelicalTrackFit htf1 = s1.getSeedCandidate().getHelix();
        HPSTrack hpstrk1 = new HPSTrack(htf1);
        SeedTrack s2 = (SeedTrack) track2;
        HelicalTrackFit htf2 = s2.getSeedCandidate().getHelix();
        HPSTrack hpstrk2 = new HPSTrack(htf2);
        boolean debug = false;
        
        Hep3Vector posAtConv1 = hpstrk1.getPositionAtZMap(100.0, BeamlineConstants.HARP_POSITION_TESTRUN, 5.0)[0];
        Hep3Vector posAtConv2 = hpstrk2.getPositionAtZMap(100.0, BeamlineConstants.HARP_POSITION_TESTRUN, 5.0)[0];

        StraightLineTrack slt1_conv = converter.Convert((Helix)hpstrk1.getTrajectory());
        StraightLineTrack slt2_conv = converter.Convert((Helix)hpstrk2.getTrajectory());
        
        A1 = new BasicHep3Vector(slt1_conv.x0(),slt1_conv.y0(),slt1_conv.z0());
        B1 = new BasicHep3Vector(slt2_conv.x0(),slt2_conv.y0(),slt2_conv.z0());

        double YZAtConv1[] = slt1_conv.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN);
        double YZAtConv2[] = slt2_conv.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN);
        
        A2 = new BasicHep3Vector(BeamlineConstants.HARP_POSITION_TESTRUN,YZAtConv1[0],YZAtConv1[1]);
        B2 = new BasicHep3Vector(BeamlineConstants.HARP_POSITION_TESTRUN,YZAtConv2[0],YZAtConv2[1]);
        
        if(debug) {
            System.out.printf("%s: original track1 direction at x=0 %s  \n",this.getClass().getSimpleName(),HelixUtils.Direction(hpstrk1,0.).toString());
            System.out.printf("%s: original track2 direction at x=0 %s  \n",this.getClass().getSimpleName(),HelixUtils.Direction(hpstrk2,0.).toString());
            System.out.printf("%s: track1 direction at conv %s  \n",this.getClass().getSimpleName(),hpstrk1.getTrajectory().getUnitTangentAtLength(0.).toString());
            System.out.printf("%s: track2 direction at conv %s  \n",this.getClass().getSimpleName(),hpstrk2.getTrajectory().getUnitTangentAtLength(0.).toString());
           
            
            System.out.printf("%s: pos at converter track1 %s  \n",this.getClass().getSimpleName(),posAtConv1.toString());
            System.out.printf("%s: pos at converter track2 %s  \n",this.getClass().getSimpleName(),posAtConv2.toString());
            //System.out.printf("%s: dir at converter track1 %s (at 0: %s) \n",this.getClass().getSimpleName(),dirConv1.toString(),dirZero1.toString());
            //System.out.printf("%s: dir at converter track2 %s (at 0: %s) \n",this.getClass().getSimpleName(),dirConv2.toString(),dirZero2.toString());
            System.out.printf("%s: A1 %s  \n",this.getClass().getSimpleName(),A1.toString());
            System.out.printf("%s: A2 %s  \n",this.getClass().getSimpleName(),A2.toString());
            System.out.printf("%s: B1 %s  \n",this.getClass().getSimpleName(),B1.toString());
            System.out.printf("%s: B2 %s  \n",this.getClass().getSimpleName(),B2.toString());
            
        }
    }
    
    
	
}
