package org.lcsim.hps.examples;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.ArrayList;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Field;
import org.lcsim.hps.users.meeg.LCIOTrackAnalysis;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/*
 * Example analysis driver.
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: StarterAnalysisDriver.java,v 1.4 2013/10/24 18:11:43 meeg Exp $
 */
public class StarterAnalysisDriver extends Driver {

    int nevents = 0;
    int naccepted = 0;
    private boolean debug = false;
    private double bfield = 0.5;
    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D trkPz;
    IHistogram2D trkMC;

    @Override
    protected void detectorChanged(Detector detector) {
        for (String str : detector.getFields().keySet()) {
            System.out.println(str);
            ((Field) detector.getFields().get(str)).getName();
        }
        trkPz = aida.histogram1D("Track momentum (Pz)", 200, 0, 10.0);
        trkMC = aida.histogram2D("Track momentum vs. MCParticle momentum (Pz)", 200, 0, 10.0, 50, 0, 10.0);
    }

    @Override
    public void process(EventHeader event) {
        nevents++;

        List<LCIOTrackAnalysis> tkanalList = processTracks(event);

        //only look at events with 2 or more tracks
//        if (tkanalList.size() < 2) {
//            return;
//        }

//        System.out.println("Event with " + tkanalList.size() + " tracks");

        int ok = 0;
        for (LCIOTrackAnalysis tkanal : tkanalList) {   //remember, these tracks are in the lcsim tracking frame!     
            Track track = tkanal.getTrack();
            BaseTrackState ts = (BaseTrackState) track.getTrackStates().get(0);
//            ts.computeMomentum(bfield);
            trkPz.fill(ts.getMomentum()[0]);
//            BaseTrackState.computeMomentum(track.getTrackStates().get(0), 0.5);
            MCParticle mcp = tkanal.getMCParticle();
            if (mcp != null) {
                System.out.println("chisq: " + track.getChi2() + ", track pz: " + ts.getMomentum()[0] + ", MC momentum: " + mcp.getMomentum());
                trkMC.fill(ts.getMomentum()[0], mcp.getPZ());
                if (Math.abs(tkanal.getMCParticle().getPDGID()) == 611) {
                    ok++;
                }
                //do some stuff to makes sure tracks are great
                //is there an e+e- from the muonium
            }
        }

        if (ok == 2) {
            naccepted++;
        }
    }

    @Override
    public void endOfData() {

        System.out.println("# of muonium events= " + naccepted + "; # of total = " + nevents);
    }

    private List<LCIOTrackAnalysis> processTracks(EventHeader event) {
        List<LCIOTrackAnalysis> tkanalList = new ArrayList<LCIOTrackAnalysis>();

        List<Track> tracklist = event.get(Track.class, "MatchedTracks");

        if (debug) {
            List<List<TrackerHit>> hitlists = event.get(TrackerHit.class);
            for (List<TrackerHit> hitlist : hitlists) {
                System.out.println(event.getMetaData(hitlist).getName());
                for (TrackerHit hit : hitlist) {
                    System.out.println(hit);
                }
            }
        }

        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");
        if (debug) {
            System.out.println("HelicalTrackMCRelations:" + mcrelations.size());
        }
        for (LCRelation relation : mcrelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittomc.add(relation.getFrom(), relation.getTo());
                if (debug) {
                    System.out.println("to:" + relation.getTo());
                    System.out.println("from:" + relation.getFrom());
                }
            }
        }

        RelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, "HelicalTrackHitRelations");
        if (debug) {
            System.out.println("HelicalTrackHitRelations:" + hitrelations.size());
        }
        for (LCRelation relation : hitrelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittostrip.add(relation.getFrom(), relation.getTo());
                if (debug) {
                    System.out.println("to:" + relation.getTo());
                    System.out.println("from:" + relation.getFrom());
                }
            }
        }

        RelationalTable hittorotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, "RotatedHelicalTrackHitRelations");
        if (debug) {
            System.out.println("RotatedHelicalTrackHitRelations:" + rotaterelations.size());
        }
        for (LCRelation relation : rotaterelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittorotated.add(relation.getFrom(), relation.getTo());
                if (debug) {
                    System.out.println("to:" + relation.getTo());
                    System.out.println("from:" + relation.getFrom());
                }
            }
        }

        for (Track track : tracklist) {   //remember, these tracks are in the lcsim tracking frame!     
            BaseTrackState ts = (BaseTrackState) track.getTrackStates().get(0);
            ts.computeMomentum(bfield);
            tkanalList.add(new LCIOTrackAnalysis(track, hittomc, hittostrip, hittorotated));
        }
        return tkanalList;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setBfield(double bfield) {
        this.bfield = bfield;
    }
}
