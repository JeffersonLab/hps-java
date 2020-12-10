package org.hps.online.example;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;

/**
 * Create an example histogram in a remote AIDA tree
 */
public class RemoteHistExampleDriver extends RemoteAidaDriver {

    private IHistogram1D tracks = null;

    private static final String DIR ="/subdet/tracker";

    public void startOfData() {
        tree.mkdirs(DIR);
        tree.cd(DIR);
        tracks = AIDA.defaultInstance().histogram1D("Tracks", 20, 0., 20.);
        super.startOfData();
    }

    public void process(EventHeader event) {
        tracks.fill(event.get(Track.class, "MatchedTracks").size());
    }
}
