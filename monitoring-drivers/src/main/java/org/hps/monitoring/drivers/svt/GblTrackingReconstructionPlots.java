package org.hps.monitoring.drivers.svt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.hps.analysis.ecal.HPSMCParticlePlotsDriver;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.HpsGblRefitter;
import org.hps.util.BasicLogFormatter;
import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.ParticleTypeClassifier;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;

public class GblTrackingReconstructionPlots extends Driver {
    private double _bfield;
    private static Logger logger = LogUtil.create(GblTrackingReconstructionPlots.class, new BasicLogFormatter());
    private AIDA aida = AIDA.defaultInstance();
    private String outputPlots = null;
    private final String trackCollectionName = "MatchedTracks";
    private final String gblTrackCollectionName = "GblTracks";
    IHistogram1D nTracks;
    IHistogram1D nTracksGbl;
    IHistogram1D nTracksDiff;
    IHistogram1D d0Diff;
    IHistogram1D z0Diff;
    IHistogram1D phiDiff;
    IHistogram1D slopeDiff;
    IHistogram1D rDiff;
    IHistogram1D pDiff;
    IHistogram1D d0Diff2;
    IHistogram1D z0Diff2;
    IHistogram1D phiDiff2;
    IHistogram1D slopeDiff2;
    IHistogram1D rDiff2;
    IHistogram1D pDiff2;
    IHistogram1D d0DiffGbl;
    IHistogram1D z0DiffGbl;
    IHistogram1D phiDiffGbl;
    IHistogram1D slopeDiffGbl;
    IHistogram1D rDiffGbl;
    IHistogram1D pDiffGbl;
    IPlotter plotter1;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter4;
    
    
    
    public GblTrackingReconstructionPlots() {
        // TODO Auto-generated constructor stub
        logger.setLevel(Level.INFO);
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }
    
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        IAnalysisFactory fac = aida.analysisFactory();
        
        Hep3Vector bfieldvec = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));
        _bfield = bfieldvec.y();
        //double bfac = 0.0002998 * bfield;
        //double bfac = Constants.fieldConversion * bfield;
        
        nTracks = aida.histogram1D("Seed tracks per event", 3, 0, 3);
        nTracksGbl = aida.histogram1D("Gbl tracks per event", 3, 0, 3);
        nTracksDiff = aida.histogram1D("Tracks per event Seed-Gbl", 6, -3, 3);
        plotter1 = fac.createPlotterFactory().create("Number of tracks per event");
        plotter1.setTitle("Other");
        //plotterFrame.addPlotter(plotter222);
        IPlotterStyle style1 = plotter1.style();
        style1.dataStyle().fillStyle().setColor("yellow");
        style1.dataStyle().errorBarStyle().setVisible(false);
        plotter1.createRegions(3, 1);
        plotter1.region(0).plot(nTracks);
        plotter1.region(1).plot(nTracksGbl);
        plotter1.region(2).plot(nTracksDiff);
        plotter1.show();
        
        d0Diff = aida.histogram1D("d0Diff", 25, -1.1, 1.1);
        z0Diff = aida.histogram1D("z0Diff", 25, -0.8, 0.8);
        slopeDiff = aida.histogram1D("slopeDiff", 25, -0.01, 0.01);
        phiDiff = aida.histogram1D("phiDiff", 25, -0.01, 0.01);
        rDiff = aida.histogram1D("rDiff", 25, -0.0001, 0.0001);
        pDiff = aida.histogram1D("pDiff", 25, -0.1, 0.1);

        plotter2 = fac.createPlotterFactory().create("Truth comparison");
        plotter2.setStyle(style1);
        plotter2.createRegions(3, 2);
        plotter2.region(0).plot(d0Diff);
        plotter2.region(1).plot(z0Diff);
        plotter2.region(2).plot(phiDiff);
        plotter2.region(3).plot(slopeDiff);
        plotter2.region(4).plot(rDiff);
        plotter2.region(5).plot(pDiff);
        plotter2.show();
        
        
        d0DiffGbl = aida.histogram1D("d0DiffGbl", 25, -1.1, 1.1);
        z0DiffGbl = aida.histogram1D("z0DiffGbl", 25, -0.8, 0.8);
        slopeDiffGbl = aida.histogram1D("slopeDiffGbl", 25, -0.01, 0.01);
        phiDiffGbl = aida.histogram1D("phiDiffGbl", 25, -0.01, 0.01);
        rDiffGbl = aida.histogram1D("rDiffGbl", 25, -0.0001, 0.0001);
        pDiffGbl = aida.histogram1D("pDiffGbl", 25, -0.1, 0.1);

        
        plotter3 = fac.createPlotterFactory().create("Truth comparison GBL");
        plotter3.setStyle(style1);
        plotter3.createRegions(3, 2);
        plotter3.region(0).plot(d0DiffGbl);
        plotter3.region(1).plot(z0DiffGbl);
        plotter3.region(2).plot(phiDiffGbl);
        plotter3.region(3).plot(slopeDiffGbl);
        plotter3.region(4).plot(rDiffGbl);
        plotter3.region(5).plot(pDiffGbl);
        plotter3.show();
        
        
        d0Diff2 = aida.histogram1D("d0Diff2", 25, -1.1, 1.1);
        z0Diff2 = aida.histogram1D("z0Diff2", 25, -0.8, 0.8);
        slopeDiff2 = aida.histogram1D("slopeDiff2", 25, -0.01, 0.01);
        phiDiff2 = aida.histogram1D("phiDiff2", 25, -0.01, 0.01);
        rDiff2 = aida.histogram1D("rDiff2", 25, -0.0001, 0.0001);
        pDiff2 = aida.histogram1D("pDiff2", 25, -0.1, 0.1);

        
        plotter4 = fac.createPlotterFactory().create("Seed vs GBL");
        plotter4.setStyle(style1);
        plotter4.createRegions(3, 2);
        plotter4.region(0).plot(d0Diff2);
        plotter4.region(1).plot(z0Diff2);
        plotter4.region(2).plot(phiDiff2);
        plotter4.region(3).plot(slopeDiff2);
        plotter4.region(4).plot(rDiff2);
        plotter4.region(5).plot(pDiff2);
        plotter4.show();
        
        
        
    }
    
    protected void process(EventHeader event) {
        
        List<Track> tracks;
        if(event.hasCollection(Track.class, trackCollectionName)) {
            tracks = event.get(Track.class, trackCollectionName);
        } else {
           logger.warning("no seed track collection");
           tracks = new ArrayList<Track>();
        }
        List<Track> gblTracks;
        if(event.hasCollection(Track.class, gblTrackCollectionName)) {
            gblTracks = event.get(Track.class, gblTrackCollectionName);
        } else {
           logger.warning("no gbl track collection");
           gblTracks = new ArrayList<Track>();
        }
        
        List<MCParticle> mcparticles;
        List<MCParticle> fsParticles;
        if( event.hasCollection(MCParticle.class) ) {
            mcparticles = event.get(MCParticle.class).get(0);
            fsParticles = HPSMCParticlePlotsDriver.makeGenFSParticleList(mcparticles);
        } else {
            logger.warning("no gbl track collection");
            mcparticles = new ArrayList<MCParticle>();
            fsParticles = new ArrayList<MCParticle>();
        }

        
        
        
        logger.info("Number of Tracks = " + tracks.size());
        logger.info("Number of GBL Tracks = " + gblTracks.size());
        logger.info("Number of MC particles = " + mcparticles.size());
        logger.info("Number of FS MC particles = " + fsParticles.size());
        
        
        
        Map<Track, MCParticle> trackTruthMatch = new HashMap<Track,MCParticle>();
        
        for(Track track : gblTracks) {
            MCParticle part = TrackUtils.getMatchedTruthParticle(track);
            trackTruthMatch.put(track, part);
            if(part!=null) {
                logger.info("Match track with q " + track.getCharge() + " p " + track.getMomentum()[0] + "," + track.getMomentum()[1] + ","  + track.getMomentum()[2]);
            } else {
                logger.info("no match for track with q " + track.getCharge() + " p " + track.getMomentum()[0] + "," + track.getMomentum()[1] + ","  + track.getMomentum()[2]);
            }
        }
        
        for(Track track : gblTracks) {
            
            logger.info("Track:");
            SeedTrack st = (SeedTrack)track;
            SeedCandidate seed = st.getSeedCandidate();
            HelicalTrackFit htf = seed.getHelix(); 
            logger.info(htf.toString());
            HelicalTrackFit pHTF = null;
            double pTruth = -1.;
            double pTrackTruth = -1.;
            if(trackTruthMatch.get(track)==null) {
                logger.info("no truth mc particle for this track");
            } else {
                MCParticle part = trackTruthMatch.get(track);
                pTruth = part.getMomentum().magnitude();
                pHTF = TrackUtils.getHTF(part,Math.abs(_bfield));
                pTrackTruth = pHTF.p(Math.abs(_bfield));
                logger.info("part: " + trackTruthMatch.get(track).getPDGID());
                logger.info("pHTF:");
                logger.info(pHTF.toString());
                logger.info("pTruth="+pTruth+" pTrackTruth="+pTrackTruth);
            }
            
            
            
            
            double d0 = htf.dca();
            double z0 = htf.z0();
            double C = htf.curvature();
            double phi = htf.phi0();
            double slope = htf.slope();
            double p = htf.p(Math.abs(_bfield));
            double d0Gbl = track.getTrackStates().get(0).getD0();
            double z0Gbl = track.getTrackStates().get(0).getZ0();
            double CGbl = track.getTrackStates().get(0).getOmega();
            double phiGbl = track.getTrackStates().get(0).getPhi();
            double slopeGbl = track.getTrackStates().get(0).getTanLambda();
            double pGbl = getMag(track.getTrackStates().get(0).getMomentum());
            logger.info("pGbl="+pGbl);

            if(pHTF!=null) {
                double d0Truth = pHTF.dca();
                double z0Truth = pHTF.z0();
                double CTruth = pHTF.curvature();
                double phiTruth = pHTF.phi0();
                double slopeTruth = pHTF.slope();
                logger.info("d0 " + d0 + " d0 trugh " + d0Truth);
                d0Diff.fill(d0-d0Truth);
                z0Diff.fill(z0-z0Truth);
                phiDiff.fill(phi-phiTruth);
                rDiff.fill(C-CTruth);
                slopeDiff.fill(slope-slopeTruth);
                pDiff.fill(p-pTruth);

                d0DiffGbl.fill(d0Gbl-d0Truth);
                z0DiffGbl.fill(z0Gbl-z0Truth);
                phiDiffGbl.fill(phiGbl-phiTruth);
                rDiffGbl.fill(CGbl-CTruth);
                slopeDiffGbl.fill(slopeGbl-slopeTruth);
                pDiffGbl.fill(pGbl-pTruth);
            }


            d0Diff2.fill(d0-d0Gbl);
            z0Diff2.fill(z0-z0Gbl);
            phiDiff2.fill(phi-phiGbl);
            rDiff2.fill(C-CGbl);
            slopeDiff2.fill(slope-slopeGbl);
            pDiff2.fill(p-pGbl);
            

        }
    
        
        
        
        
        nTracks.fill(tracks.size());
        nTracksGbl.fill(gblTracks.size());
        nTracksDiff.fill(tracks.size()-gblTracks.size());
        
        
        
        
    }
    
    
    private double getMag(double p[]) {
        return Math.sqrt(p[0]*p[0] + p[1]*p[1] + p[2]*p[2]);
    }
    
    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                logger.log(Level.SEVERE,"aid problem saving file",ex);
            }
        }
        //plotterFrame.dispose();
        //topFrame.dispose();
        //bottomFrame.dispose();
    }

    
    
}
