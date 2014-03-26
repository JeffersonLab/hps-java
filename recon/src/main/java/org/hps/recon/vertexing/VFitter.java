package org.hps.recon.vertexing;

import java.util.List;
import org.lcsim.event.Track;
import org.lcsim.spacegeom.SpacePoint;
import org.lcsim.recon.vertexing.billoir.Vertex;

/**
 *
 * @author jstrube
 */
public interface VFitter {
    // better have an enumset of possible constraints
    // or better even some kind of map for the constraints
    public Vertex fit(List<Track> tracks, SpacePoint initialPosition, boolean withBeamConstraint);
}

