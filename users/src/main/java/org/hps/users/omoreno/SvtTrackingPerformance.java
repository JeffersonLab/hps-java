package org.hps.users.omoreno;

//--- java ---//
import java.util.ArrayList;
import java.util.List;

//--- org.lcsim ---//
import org.lcsim.event.EventHeader;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

//--- hps-java ---//

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: SvtTrackingPerformance.java,v 1.2 2013/11/06 19:19:55 jeremy Exp $
 */
public class SvtTrackingPerformance extends Driver {

    boolean debug;
    int[] topLayer = new int[5];
    int[] bottomLayer = new int[5];

    // Collection names
    String stereoHitCollectionName = "RotatedHelicalTrackHits";

    protected void detectorChanged(Detector detector) {

    }

    public void process(EventHeader event) {

        // Check if the event has stereo hits
        if (!event.hasCollection(HelicalTrackHit.class, stereoHitCollectionName)) {
            System.out.println(this.getClass().getSimpleName() + " Stereo Hit Collection: " + stereoHitCollectionName + " was not found! Skipping event ...");
        }

        // Get the list of HelicalTrackHits
        List<HelicalTrackHit> stereoHits = new ArrayList<HelicalTrackHit>();

        for (HelicalTrackHit stereoHit : stereoHits) {

            // Loop over all stereo hits and arrange them by layer and by detector
            // volume
            int layer = this.getLayerNumber(stereoHit);

            //
            if (stereoHit.y() > 0)
                topLayer[layer - 1]++;
            else if (stereoHit.y() < 0)
                bottomLayer[layer - 1]++;
            else
                throw new RuntimeException("Invalid hit position - y = " + stereoHit.y());
        }

        // Check if there are four consecutive layers hit on either volume

    }

    private int getLayerNumber(HelicalTrackHit stereoHit) {

        if (debug)
            System.out.println(this.getClass().getSimpleName() + " : Stereo Hit z position = " + stereoHit.z());

        // Get the position along z
        int z = Math.round((float) stereoHit.z());

        switch (z) {
        case 100:
            return 1;
        case 200:
            return 2;
        case 300:
            return 3;
        case 500:
            return 4;
        case 700:
            return 5;
        default:
            throw new RuntimeException("Invalid value of z: " + z);
        }
    }

    private boolean consecutiveHits(int[] layer) {
        for (int index = 0; index < 2; index++) {
            if (layer[index] > 0 && layer[index + 1] > 0 && layer[index + 2] > 0 && layer[index + 4] > 0)
                return true;
        }
        return false;
    }
}
