package org.hps.plugin;

import hep.graphics.heprep.HepRepFactory;
import hep.graphics.heprep.HepRepInstance;
import hep.graphics.heprep.HepRepInstanceTree;
import hep.graphics.heprep.HepRepType;
import hep.graphics.heprep.HepRepTypeTree;
import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.heprep.HepRepCollectionConverter;
import org.lcsim.util.heprep.LCSimHepRepConverter;
import org.lcsim.util.swim.HelixSwimmer;
import org.lcsim.util.swim.HelixSwimmerYField;

/**
 * Convert an HPS track to heprep for Wired display.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class HPSTrackConverter implements HepRepCollectionConverter {

    private static final double[] ORIGIN = {0, 0, 0.00001};
    
    public boolean canHandle(Class k) {
        return Track.class.isAssignableFrom(k);
    }

    public void convert(EventHeader event, List collection, HepRepFactory factory, HepRepTypeTree typeTree, HepRepInstanceTree instanceTree) {
        try {
            event.getDetector();
        } catch (Exception x) {
            return;
        }
        
        if (collection.size() == 0)
            return;
        
        List<Track> trackList = (List<Track>) collection;
        Track firstTrack = trackList.get(0);
        
        LCMetaData meta = event.getMetaData(collection);
        String name = meta.getName();
        Detector detector = event.getDetector();

        double trackingRMax = detector.getConstants().get("tracking_region_radius").getValue();
        double trackingZMax = detector.getConstants().get("tracking_region_zmax").getValue();

        double[] field = detector.getFieldMap().getField(ORIGIN);
        HelixSwimmer helix = new HelixSwimmerYField(field[1]);
        
        HepRepType typeX = factory.createHepRepType(typeTree, name);
        typeX.addAttValue("layer", LCSimHepRepConverter.PARTICLES_LAYER);
        typeX.addAttValue("drawAs", "Line");

        typeX.addAttDef("pT", "Transverse momentum", "physics", "");
        typeX.addAttDef("dedX", "de/Dx", "physics", "GeV");
        typeX.addAttDef("dedX error", "", "physics", "GeV");
        typeX.addAttDef("Charge", "", "physics", "");
        typeX.addAttDef("Chi2", "", "physics", "");
        typeX.addAttDef("pX", "Momentum X", "physics", "GeV");
        typeX.addAttDef("pY", "Momentum Y", "physics", "GeV");
        typeX.addAttDef("pZ", "Momentum Z", "physics", "GeV");
        typeX.addAttDef("NDF", "Number Degrees Freedom", "physics", "");
        typeX.addAttDef("Reference Point X", "Reference Point X", "physics", "mm");
        typeX.addAttDef("Reference Point Y", "Reference Point Y", "physics", "mm");
        typeX.addAttDef("Reference Point Z", "Reference Point Z", "physics", "mm");
        typeX.addAttDef("d0", "d0", "physics", "");
        typeX.addAttDef("phi0", "phi0", "physics", "");
        typeX.addAttDef("omega", "omega", "physics", "");
        typeX.addAttDef("z0", "z0", "physics", "");
        typeX.addAttDef("s", "s", "physics", "");

        for (Track track : trackList) {
            
            helix.setTrack(track);
            double distanceToCylinder = helix.getDistanceToCylinder(trackingRMax, trackingZMax);

            TrackState ts = track.getTrackStates().get(0);
            double[] referencePoint = ts.getReferencePoint();
            double[] momentum = ts.getMomentum();

            HepRepInstance instanceX = factory.createHepRepInstance(instanceTree, typeX);
            double pt = field[2] * Constants.fieldConversion / Math.abs(ts.getParameter(2));

            instanceX.addAttValue("pT", pt);
            instanceX.addAttValue("dedX", track.getdEdx());
            instanceX.addAttValue("dedX error", track.getdEdxError());
            instanceX.addAttValue("Charge", track.getCharge());
            instanceX.addAttValue("Chi2", track.getChi2());
            instanceX.addAttValue("pX", momentum[0]);
            instanceX.addAttValue("pY", momentum[1]);
            instanceX.addAttValue("pZ", momentum[2]);
            instanceX.addAttValue("NDF", track.getNDF());
            instanceX.addAttValue("Reference Point X", referencePoint[0]);
            instanceX.addAttValue("Reference Point Y", referencePoint[1]);
            instanceX.addAttValue("Reference Point Z", referencePoint[2]);
            instanceX.addAttValue("d0", ts.getParameter(0));
            instanceX.addAttValue("phi0", ts.getParameter(1));
            instanceX.addAttValue("omega", ts.getParameter(2));
            instanceX.addAttValue("z0", ts.getParameter(3));
            instanceX.addAttValue("s", ts.getParameter(4));

            double dAlpha = 10; // 1cm

            for (int k = 0; k < 200; k++) {
                double d = k * dAlpha;
                if (d > distanceToCylinder)
                    break;
                Hep3Vector point = helix.getPointAtDistance(d);
                factory.createHepRepPoint(instanceX, point.x(), point.y(), point.z());
            }
        }
    }       
}