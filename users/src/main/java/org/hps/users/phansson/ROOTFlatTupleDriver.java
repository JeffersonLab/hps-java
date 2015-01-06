package org.hps.users.phansson;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.ref.plotter.PlotterRegion;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.analysis.ecal.HPSMCParticlePlotsDriver;
//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.readout.ecal.TriggerData;
import org.hps.recon.tracking.BeamlineConstants;
import org.hps.recon.tracking.EventQuality;
import org.hps.recon.tracking.HPSTrack;
import org.hps.recon.tracking.StraightLineTrack;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.TwoParticleVertexer;
import org.hps.recon.vertexing.TwoTrackFringeVertexer;
import org.hps.recon.vertexing.TwoTrackVertexer;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.ParticleTypeClassifier;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HitIdentifier;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Analysis class which makes a text file compatible with ROOT Tree.
 * @author phansson <phansson@slac.stanford.edu>
 * @version $id: $
 */
public class ROOTFlatTupleDriver extends Driver {

    private FileWriter fileWriter = null;
    private PrintWriter printWriter = null;
    private String outputNameTextTuple = "twotrackAnlysisTuple.txt";
    private String trackCollectionName = "MatchedTracks";
    private boolean doPrintBranchInfoLine = true; //firs tline in text file
    private AIDA aida = AIDA.defaultInstance();
    private int totalEvents=0;
    private int totalTwoTrackEvents=0;
    private int totalMCEvents=0;
    private int totalTwoTrackMCEvents=0;
	private int nEventsWithGoodRegionCluster=0;
    private boolean hideFrame = false;
    private String outputPlotFileName;
    private String ecalClusterCollectionName = "EcalClusters";
    private String stereoHitCollectionName = "RotatedHelicalTrackHits";
    private String triggerDecisionCollectionName = "TriggerBank";
    private String MCParticleCollectionName = "MCParticle";
    private String _stripClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private double targetPosition = BeamlineConstants.HARP_POSITION_TESTRUN;
    private boolean _debug;
    private HitIdentifier _ID = new HitIdentifier();
    private TwoTrackVertexer vertexer = new TwoTrackVertexer();
    private TwoTrackFringeVertexer fringeVertexer = new TwoTrackFringeVertexer();
    private TwoParticleVertexer particleVertexer = new TwoParticleVertexer();
	private boolean _keepAllTracks = true;
    private EventQuality.Quality trk_quality_def = EventQuality.Quality.MEDIUM;
    private IPlotter _plotterParticleVertex;
    private IPlotter _plotterTrackVertex;
    private IPlotter _plotterTrackVertexFr;
    private IPlotter _plotterTrackVertexNonBend;
    private IPlotter _plotterTrackMult;
    private IPlotter _plotterTrackAtConv;
    private IPlotter _plotterTrackMatch;
    private IHistogram1D _vtxpos_x;
    private IHistogram1D _vtxpos_y;
    private IHistogram1D _vtxpos_z;
    private IHistogram1D _vtxposfr_x;
    private IHistogram1D _vtxposfr_y;
    private IHistogram1D _vtxposfr_z;
    private IHistogram1D _vtxposnonb_x;
    private IHistogram1D _vtxposnonb_y;
    private IHistogram1D _vtxposnonb_dy;
    private IHistogram1D _vtxposnonb_z;
    private IHistogram1D _partvtxpos_x;
    private IHistogram1D _partvtxpos_y;
    private IHistogram1D _partvtxpos_z;
    private IHistogram1D _vtxposnonb_xAtZ0;
    private IHistogram1D _vtxposnonb_zAtTarget;
    private IHistogram1D _vtxposnonb_angle1;
    private IHistogram1D _vtxposnonb_angle2;
    private IHistogram2D _ntrks_px;
    private IHistogram1D _trk_y_at_conv_top_pos;
    private IHistogram1D _trk_z_at_conv_top_pos;
    private IHistogram1D _trk_y_at_conv_top_pos_fr;
    private IHistogram1D _trk_z_at_conv_top_pos_fr;
    private IHistogram1D _trk_y_at_conv_bot_pos;
    private IHistogram1D _trk_z_at_conv_bot_pos;
    private IHistogram1D _trk_y_at_conv_bot_pos_fr;
    private IHistogram1D _trk_z_at_conv_bot_pos_fr;
    private IHistogram1D _trk_y_at_conv_top_neg;
    private IHistogram1D _trk_z_at_conv_top_neg;
    private IHistogram1D _trk_y_at_conv_top_neg_fr;
    private IHistogram1D _trk_z_at_conv_top_neg_fr;
    private IHistogram1D _trk_y_at_conv_bot_neg;
    private IHistogram1D _trk_z_at_conv_bot_neg;
    private IHistogram1D _trk_y_at_conv_bot_neg_fr;
    private IHistogram1D _trk_z_at_conv_bot_neg_fr;
    private IHistogram1D _trkmatch_top_dy;
    private IHistogram1D _trkmatch_top_dx;
    private IHistogram1D _trkmatch_bot_dy;
    private IHistogram1D _trkmatch_bot_dx;
    private IHistogram1D _trkmatch_top_plus_dx;
    private IHistogram1D _trkmatch_bot_plus_dx;
    private IHistogram1D _trkmatch_top_minus_dx;
    private IHistogram1D _trkmatch_bot_minus_dx;
    
	    
    private class CmpTrack implements Comparable<CmpTrack> {
        private Track _track;
        public CmpTrack(Track track) {
            _track = track;
        }
        @Override
        public int compareTo(CmpTrack t) {
                int v = ((Double)(t._track.getTrackStates().get(0).getMomentum()[0]*100000 - _track.getTrackStates().get(0).getMomentum()[0]*100000)).intValue();
                //System.out.printf("%s: _track = %f _t = %f  => %d \n",this.getClass().getSimpleName(),_track.getTrackStates().get(0).getMomentum()[0],t._track.getTrackStates().get(0).getMomentum()[0],v);
                return v;
        }
    }
    
    
    public void setDebug(boolean v) {
        this._debug = v;
    }
    public void setOutputPlotFileName(String filename) {
        outputPlotFileName = filename;
    }
    public void setOutputNameTextTuple(String filename) {
        this.outputNameTextTuple = filename;
    }
    
    public void setHideFrame(boolean hide) {
        hideFrame = hide;
    }
    
    public void setTrackCollectionName(String name) {
        trackCollectionName = name;
    }
    
    public void setTargetPosition(double pos) {
        targetPosition = pos;
    }
    

    
    public ROOTFlatTupleDriver() {
    }

    
    @Override
    public void detectorChanged(Detector detector) {

        createWriter();

        if(printWriter==null)
            System.out.println("printWriter is null");
            
        fillTextTupleBranches();

        makePlots();
       
        printEcalInfo(detector);

    }
    
    
    @Override
    public void process(EventHeader event) {

        if(this._debug) 
            System.out.println(this.getClass().getSimpleName() + ": processing event " + totalEvents + " which has event nr " + event.getEventNumber());
            
        
        
        totalEvents++;
        
        this.vertexer.clear();
        this.fringeVertexer.clear();
        this.particleVertexer.clear();
        
        
        List<Track> tracklist = null;
        if(event.hasCollection(Track.class,trackCollectionName)) {        
            tracklist = event.get(Track.class, trackCollectionName);
            if(_debug) System.out.println(this.getClass().getSimpleName() + ": Number of Tracks = " + tracklist.size() + " in event " + event.getEventNumber());
        } else {
             if(_debug)  System.out.println(this.getClass().getSimpleName() + ": No track collection in event " + event.getEventNumber());
             return;
        }
        
        
        //if(tracklist.size()<2) {
            //if(_debug) 
        //        System.out.printf("%s: event %d has only %d tracks \n",this.getClass().getSimpleName(),event.getEventNumber(),tracklist.size());  
        //    System.exit(1);
        //    return;
        //}
        
        ArrayList<CmpTrack> tracks = new ArrayList<CmpTrack>();
        for(int i=0;i<tracklist.size();++i) {
            Track trk = tracklist.get(i);
			if( _keepAllTracks) {
                tracks.add(new CmpTrack(trk));
			}
            else if(TrackUtils.isGoodTrack(trk, tracklist, trk_quality_def)) {
                //System.out.printf("%s: trk momentum (%.3f,%.3f,%.3f) chi2=%.3f\n",this.getClass().getSimpleName(),trk.getTrackStates().get(0).getMomentum()[0],trk.getTrackStates().get(0).getMomentum()[1],trk.getTrackStates().get(0).getMomentum()[2],trk.getChi2());
                if(_debug) {
                    int cuts = TrackUtils.passTrackSelections(trk, tracklist, trk_quality_def);
                    System.out.printf("%s: track cuts: \n%s\n",this.getClass().getSimpleName(),EventQuality.instance().print(cuts));
                    System.out.printf("%s: trk momentum (%.3f,%.3f,%.3f) chi2=%.3f\n",this.getClass().getSimpleName(),trk.getTrackStates().get(0).getMomentum()[0],trk.getTrackStates().get(0).getMomentum()[1],trk.getTrackStates().get(0).getMomentum()[2],trk.getChi2());
                }
                if(trk.getChi2()>10. && _debug) {
                    System.out.printf("%s: trk momentum (%.3f,%.3f,%.3f) chi2=%.3f\n",this.getClass().getSimpleName(),trk.getTrackStates().get(0).getMomentum()[0],trk.getTrackStates().get(0).getMomentum()[1],trk.getTrackStates().get(0).getMomentum()[2],trk.getChi2());
                    int cuts = TrackUtils.passTrackSelections(trk, tracklist, trk_quality_def);
                    System.out.printf("%s: track cuts: \n%s\n",this.getClass().getSimpleName(),EventQuality.instance().print(cuts));
                    //System.exit(0);
                }
                tracks.add(new CmpTrack(trk));            
			} else {
               if(_debug) System.out.println(this.getClass().getSimpleName() + ": trk failed track selections (event nr " + event.getEventNumber() + ")\n" + trk.toString());
            }
        }
        
        Collections.sort(tracks);
        
        Hep3Vector vtxPos = null;
        Hep3Vector vtxPosFringe = null;
        Hep3Vector vtxPosNonBend = null;
        
        if(tracks.size()>1) {
            Track trk1 = tracks.get(0)._track;
            Track trk2 = tracks.get(1)._track;
            
            vertexer.setTracks(trk1, trk2);
            vertexer.fitVertex();            
			Vertex vtx = vertexer.getFittedVertex();
			if (vtx != null) {
				vtxPos = vertexer.getFittedVertex().getPosition();
				fringeVertexer.setTracks(trk1, trk2);
				fringeVertexer.fitVertex();
				vtxPosFringe = fringeVertexer.getFittedVertex().getPosition();
				
				if(this._debug) {
					System.out.printf("%s: vtxPos=%s\n", this.getClass().getSimpleName(),vtxPos.toString());
					System.out.printf("%s: vtxPosFringe=%s\n", this.getClass().getSimpleName(),vtxPosFringe.toString());
				}
				
				if(vtxPos.x() != vtxPos.x()) {
					System.out.printf("%s: vtxPos is NaN -> Skip\n",this.getClass().getSimpleName());
					vtxPos = null;
				}
				if(vtxPosFringe.x() != vtxPosFringe.x()) {
					System.out.printf("%s: vtxPosFringe is NaN -> Skip\n",this.getClass().getSimpleName());
					vtxPos = null;
				}
				
				this._vtxpos_x.fill(vtxPos.x());
				this._vtxpos_y.fill(vtxPos.y());
				this._vtxpos_z.fill(vtxPos.z());            
				
				if(vtxPosFringe!=null) {
					this._vtxposfr_x.fill(vtxPosFringe.x());
					this._vtxposfr_y.fill(vtxPosFringe.y());
					this._vtxposfr_z.fill(vtxPosFringe.z());            
				}
			}				
            
            boolean useFringe = false;
            StraightLineTrack[] slts = this.getSLTs(trk1, trk2, useFringe);
            double zAtCross = this.getCrossingS(trk1, trk2);
            double[] xyAtZ1 = slts[0].calculateXYAtZ(zAtCross);
            double[] xyAtZ2 = slts[1].calculateXYAtZ(zAtCross);

            Hep3Vector[] vtxNonBend = {new BasicHep3Vector(xyAtZ1[0],xyAtZ1[1],zAtCross),new BasicHep3Vector(xyAtZ2[0],xyAtZ2[1],zAtCross)};
            //System.out.printf("%s: vtxNonBend=%s\n", this.getClass().getSimpleName(),vtxNonBend.toString());
            
            this._vtxposnonb_x.fill(vtxNonBend[0].x());
            this._vtxposnonb_y.fill(vtxNonBend[0].y());
            this._vtxposnonb_y.fill(vtxNonBend[1].y());
            this._vtxposnonb_dy.fill(vtxNonBend[0].y()-vtxNonBend[1].y());
            this._vtxposnonb_z.fill(vtxNonBend[0].z());

            this._vtxposnonb_xAtZ0.fill(slts[0].calculateXYAtZ(0.)[0]);
            this._vtxposnonb_zAtTarget.fill(slts[0].getYZAtX(this.targetPosition)[1]);
            this._vtxposnonb_angle1.fill(Math.atan(slts[0].dzdx()));
            this._vtxposnonb_angle2.fill(Math.atan(slts[1].dzdx()));
            
            vtxPosNonBend = vtxNonBend[0];

        }
        
        
        List<Cluster> clusters = new ArrayList<Cluster>();

        if(!event.hasCollection(Cluster.class, ecalClusterCollectionName)) {
            if(_debug) {
                System.out.println(this.getClass().getSimpleName() + ": event doesn't have a ecal cluster collection ");
            }
        } else {
            clusters = event.get(Cluster.class, ecalClusterCollectionName); 
        
            if(_debug) {
                System.out.println(this.getClass().getSimpleName() + ": found " + clusters.size() + " ecal clusters " + event.getEventNumber());
            }

			boolean goodRegion = false;
			for(Cluster c : clusters) {
                int iy = c.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                int ix = c.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                double E = c.getEnergy();
				int evtnr = event.getEventNumber();
                int clsize = c.getSize();
				if( iy>0 && ix>0 && E>0.6) {
					//printWriter.format("%d %5.5f %5d %5d %5d ",evtnr,E,ix,iy,clsize);
					goodRegion = true;
				}
			}
			if( goodRegion ) nEventsWithGoodRegionCluster++;

        }
        
        Hep3Vector vtxPosMC = null;
        MCParticle electron=null;
        MCParticle positron=null;
        if(event.hasCollection(MCParticle.class,this.MCParticleCollectionName)) {
            totalMCEvents++;
            List<MCParticle> mcparticles = event.get(MCParticle.class,this.MCParticleCollectionName);
            List<MCParticle> fsParticles = HPSMCParticlePlotsDriver.makeGenFSParticleList(mcparticles);
                 
            for(MCParticle part : fsParticles) {
                if(ParticleTypeClassifier.isElectron(part.getPDGID())) {
                    if(electron==null) {
                        electron = part;
                    } else {
                        if(part.getEnergy()>electron.getEnergy()) {
                            electron = part;
                        }
                    }
                }
                if(ParticleTypeClassifier.isPositron(part.getPDGID())) {
                    if(positron==null) {
                        positron = part;
                    } else {
                        if(part.getEnergy()>positron.getEnergy()) {
                            positron = part;
                        }
                    }
                }
            }

            if(electron!=null && positron!=null) {
                particleVertexer.setParticle(electron, positron);
                particleVertexer.fitVertex();
                vtxPosMC = particleVertexer.getFittedVertex().getPosition();
                if(this._debug) System.out.printf("%s: vtxPosMC=%s org1 %s org2 %s\n", this.getClass().getSimpleName(),vtxPosMC.toString(),electron.getOrigin().toString(),positron.getOrigin().toString());
                this._partvtxpos_x.fill(vtxPosMC.x());
                this._partvtxpos_y.fill(vtxPosMC.y());
                this._partvtxpos_z.fill(vtxPosMC.z());
                totalTwoTrackMCEvents++;
            }
        }
        

        
        totalTwoTrackEvents++;
        try {
            this.fillTextTuple(electron, positron, tracks, vtxPosMC, vtxPos, vtxPosFringe, vtxPosNonBend, clusters, event);
        } catch (IOException ex) {
            Logger.getLogger(ROOTFlatTupleDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(this._debug) System.out.println(this.getClass().getSimpleName() + ": # two track events so far = "+totalTwoTrackEvents);
        //System.exit(0);
        
    }

    
    
    @Override
    public void endOfData() {
        
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Events = "+this.totalEvents);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Events filling tuple = "+this.totalTwoTrackEvents);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of events with MC collection = "+this.totalMCEvents);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of events with e+e- ID'd = "+this.totalTwoTrackMCEvents);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of events with cl in good region = "+this.nEventsWithGoodRegionCluster);
		
        
        if (!"".equals(outputPlotFileName)) {
            try {
                aida.saveAs(outputPlotFileName);
            } catch (IOException ex) {
                Logger.getLogger(TrigRateDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
            }
        }
        
        if(printWriter==null) {
            //Fill branches to text file (this is because detectorChanged() is called only if there are events to process
            createWriter();
            fillTextTupleBranches();
        }
            
        
        System.out.printf("%s: printWriter close \n",this.getClass().getSimpleName());
        printWriter.close();
        try {
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(ROOTFlatTupleDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        
                
    }
    
    
  
    
    private boolean isFileEmpty(String fileName) {
        File f = new File(fileName);
        return f.length() == 0; //return zero also in case file doesn't exist
    }
    
    
    private void fillTextTupleBranches() {
        
        if(!doPrintBranchInfoLine) {
            throw new RuntimeException("Trying to fill tuple branches with flag being set to false?!");
        }
        
        String br_line = "";                 
        br_line+="evtnr/I:";
        br_line+="ntrks_top/I:ntrks_bot/I:ntrks100_top/I:ntrks100_bot/I:ntrks200_top/I:ntrks200_bot/I:ntrks300_top/I:ntrks300_bot/I:ntrks400_top/I:ntrks400_bot/I:ntrks500_top/I:ntrks500_bot/I:";
        br_line+="e_px/F:e_py/F:e_pz/F:p_px/F:p_py/F:p_pz/F:";
        
        for(int itrk=1;itrk<=4;++itrk) {
            String trk_str = String.format("trk%d", itrk);

            br_line+=""+trk_str+"_d0/F:"+trk_str+"_phi0/F:"+trk_str+"_R/F:"+trk_str+"_z0/F:"+trk_str+"_slope/F:";
            br_line+=""+trk_str+"_q/I:"+trk_str+"_chi2/F:"+trk_str+"_px/F:"+trk_str+"_py/F:"+trk_str+"_pz/F:"+trk_str+"_nhits/I:";
            for(int iLayer=1;iLayer<=5;++iLayer) br_line+=""+trk_str+"_hit"+iLayer+"_x/F:"+""+trk_str+"_hit"+iLayer+"_y/F:"+""+trk_str+"_hit"+iLayer+"_z/F:";
            for(int iLayer=1;iLayer<=5;++iLayer) {
                br_line+=""+trk_str+"_res"+iLayer+"_y/F:"+""+trk_str+"_res"+iLayer+"_z/F:";
                br_line+=""+trk_str+"_eres"+iLayer+"_y/F:"+""+trk_str+"_eres"+iLayer+"_z/F:";
                br_line+=""+trk_str+"_drdphi"+iLayer+"/F:"+""+trk_str+"_msdrphi"+iLayer+"/F:";
                br_line+=""+trk_str+"_dz"+iLayer+"/F:"+""+trk_str+"_msdz"+iLayer+"/F:";
                //br_line+=""+trk_str+"_ures"+iLayer+"/F:"+""+trk_str+"_ureserr"+iLayer+"/F:";
            }
            for(int iLayer=1;iLayer<=10;++iLayer) br_line+=""+trk_str+"_strip"+iLayer+"_u/F:"+""+trk_str+"_strip"+iLayer+"_time/F:"+""+trk_str+"_strip"+iLayer+"_E/F:";
            
            br_line+=""+trk_str+"_conv_y/F:"+trk_str+"_conv_z/F:";
            br_line+=""+trk_str+"_fr_conv_x/F:"+trk_str+"_fr_conv_y/F:"+trk_str+"_fr_conv_z/F:";
            br_line+=""+trk_str+"_target_y/F:"+trk_str+"_target_z/F:";
            br_line+=""+trk_str+"_fr_target_x/F:"+trk_str+"_fr_target_y/F:"+trk_str+"_fr_target_z/F:";
            //br_line+=""+trk_str+"_ecal_y/F:"+trk_str+"_ecal_z/F:";
            br_line+=""+trk_str+"_ecal_y/F:"+trk_str+"_ecal_z/F:"+trk_str+"_fr_ecal_x/F:"+trk_str+"_fr_ecal_y/F:"+trk_str+"_fr_ecal_z/F:";
            br_line+=""+trk_str+"_ecal_sm_y/F:"+trk_str+"_ecal_sm_z/F:"+trk_str+"_fr_ecal_sm_x/F:"+trk_str+"_fr_ecal_sm_y/F:"+trk_str+"_fr_ecal_sm_z/F:";
            //br_line+=""+trk_str+"_fr_ecal_x/F:"+trk_str+"_fr_ecal_y/F:"+trk_str+"_fr_ecal_z/F:";
            //br_line+=""+trk_str+"_fr_ecal_x/F:";
            
        }
        //
        for(int iLayer=1;iLayer<=10;++iLayer) br_line+="top_strip"+iLayer+"_n/F:";
        for(int iLayer=1;iLayer<=10;++iLayer) br_line+="bot_strip"+iLayer+"_n/F:";
        for(int iLayer=1;iLayer<=10;iLayer+=2) br_line+="top_stereo"+iLayer+"_n/F:";
        for(int iLayer=1;iLayer<=10;iLayer+=2) br_line+="bot_stereo"+iLayer+"_n/F:";
        br_line+="vtx_truth_x/F:vtx_truth_y/F:vtx_truth_z/F:";
        br_line+="vtx_x/F:vtx_y/F:vtx_z/F:";
        br_line+="vtx_fr_x/F:vtx_fr_y/F:vtx_fr_z/F:";
        br_line+="vtx_nonbend_x/F:vtx_nonbend_y/F:vtx_nonbend_z/F:";
        br_line+="cl1_E/F:cl1_ix/I:cl1_iy/I:cl1_x/F:cl1_y/F:cl1_z/F:cl1_n/I:";
        br_line+="cl2_E/F:cl2_ix/I:cl2_iy/I:cl2_x/F:cl2_y/F:cl2_z/F:cl2_n/I:";
        br_line+="cl3_E/F:cl3_ix/I:cl3_iy/I:cl3_x/F:cl3_y/F:cl3_z/F:cl3_n/I:";
        br_line+="ncl_top/I:ncl_bot/I:";
        br_line+="trig_top/I:trig_bot/I";
        
        if(printWriter==null) {
            System.out.println("hmm 1");
        }
        printWriter.println(br_line);

        doPrintBranchInfoLine = false;

    }
    
    
    /**
     * Find the closest ECal cluster in deltaR. Return null if no match
     * @param trk
     * @param clusters
     * @return clostest cluster.
     */

    private Cluster findEcalCluster(Track trk, List<Cluster> clusters) {
    	Cluster matched_cluster = null;
    	double drMin = 9999999.9;
    	double drMax = 9999999.9;
    	double dr;
    	Hep3Vector pos_cl,pos_trk;
    	for(Cluster cluster : clusters) {
    		pos_cl = new BasicHep3Vector(cluster.getPosition());
    		pos_trk = TrackUtils.extrapolateTrack(trk,pos_cl.z());
    		dr = VecOp.sub(pos_cl, pos_trk).magnitude();
    		if( dr < drMax) {
    			if( dr < drMin ) {
    				matched_cluster = cluster;
    				drMin = dr;
    			}
    		}
    	}
    	return matched_cluster;
    }
    
    private void fillTextTuple(MCParticle e, MCParticle p, List<CmpTrack> tracks, Hep3Vector vtxPosParticle, Hep3Vector vtxPos, Hep3Vector vtxPosFr, Hep3Vector vtxPosNonBend, List<Cluster> clusters, EventHeader event) throws IOException {
        if(doPrintBranchInfoLine) {
            throw new RuntimeException("Need to fill tuple branches first!?");
        }
        
        //Event info
        printWriter.format("%5d ",event.getEventNumber());
        if(tracks.size()>0) {
        for(int icut=0;icut<=5;++icut) {
            int ntrks[] = getNtracks(tracks,icut*0.1);
            printWriter.format("%5d %5d ",ntrks[0],ntrks[1]);
            this._ntrks_px.fill(0.1*icut, ntrks[0]+ntrks[1]);
        }
        } else {
            printWriter.format("%5d %5d %5d %5d %5d %5d %5d %5d %5d %5d %5d %5d ", -9999999, -9999999, -9999999, -9999999, -9999999, -9999999, -9999999, -9999999, -9999999, -9999999, -9999999, -9999999 );
        }
        
        //Truth
        if(e!=null && p!=null) printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f %5.5f ", e.getPX(),e.getPY(),e.getPZ(), p.getPX(),p.getPY(),p.getPZ() );
        else printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f %5.5f ", -9999999., -9999999., -9999999., -9999999., -9999999., -9999999. );
        
        
        for (int itrk=0;itrk<4;itrk++) {
            Track trk1 = null;
            if(tracks.size()>itrk) trk1 = tracks.get(itrk)._track;
           
            if(trk1!=null) {
                SeedTrack st1 = (SeedTrack) trk1;
                HelicalTrackFit helix1 = st1.getSeedCandidate().getHelix();     
                List<TrackerHit> hitsOnTrack1 = trk1.getTrackerHits();
                HashMap<Integer,HelicalTrackHit> hits1 = getHitMap(hitsOnTrack1,helix1);

                printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",helix1.dca(),helix1.phi0(),helix1.R(),helix1.z0(),helix1.slope());
                printWriter.format("%5d %5.5f %5.5f %5.5f %5.5f %5d ",trk1.getCharge(),trk1.getChi2(), trk1.getTrackStates().get(0).getMomentum()[0],trk1.getTrackStates().get(0).getMomentum()[1],trk1.getTrackStates().get(0).getMomentum()[2],hitsOnTrack1.size());
                // stupid but I want to keep one line per event so default in case there is not hits in all layers
                for(int iLayer=0;iLayer<5;++iLayer) {
                    HelicalTrackHit hitOnLayer = hits1.get(iLayer*2+1);// = this.getHitOnLayer(iLayer, hitsOnTrack);
                    if (hitOnLayer != null) printWriter.format("%5.5f %5.5f %5.5f ", hitOnLayer.getPosition()[0],hitOnLayer.getPosition()[1],hitOnLayer.getPosition()[2]);
                    else printWriter.format("%5.5f %5.5f %5.5f ", -9999999.9, -9999999.9, -9999999.9);
                }

                //Get the helix for residual calculation
                for(int iLayer=0;iLayer<5;++iLayer) {
                    HelicalTrackHit hitOnLayer = hits1.get(iLayer*2+1);// = this.getHitOnLayer(iLayer, hitsOnTrack);
                    if (hitOnLayer != null) {
                        //printWriter.format("\n%s\n","X11");
                        Map<String,Double> res = TrackUtils.calculateTrackHitResidual(hitOnLayer, helix1, true);
                        if( !res.isEmpty() ) {
                        	printWriter.format("%5.5f %5.5f ",res.get("resy"),res.get("resz"));
                        	printWriter.format("%5.5f %5.5f ",res.get("erry"),res.get("errz"));
                        	printWriter.format("%5.5f %5.5f ", res.get("drphi"),res.get("msdrphi"));
                        	printWriter.format("%5.5f %5.5f ", res.get("dz_res"),res.get("msdz"));
                        } else {
                            printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                            printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                            printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                            printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);                        	
                        }
                    }
                    else {
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                    }
                }
                
                HashMap<Integer,List<HelicalTrackStrip>> striphits1 = this.getStripHitsMap(hitsOnTrack1);        
                for(int iLayer=1;iLayer<=10;++iLayer) {
                    HelicalTrackStrip strip=null;
                    if(striphits1.containsKey(iLayer)) strip = striphits1.get(iLayer).get(0);
                    if(strip!=null) {
                        printWriter.format("%5.5f %5.5f %5.5f ", strip.umeas(),strip.time(),strip.dEdx()/DopedSilicon.ENERGY_EHPAIR);
                    }
                    else {
                        printWriter.format("%5.5f %5.5f %5.5f ", -99999999.9, -99999999.9, -99999999.9);
                    }
                }

                //Track at converter
                Hep3Vector posAtConverter = TrackUtils.extrapolateTrack(trk1,BeamlineConstants.HARP_POSITION_TESTRUN);
                if(beamlinePosOk(posAtConverter))  printWriter.format("%5.5f %5.5f ", posAtConverter.x(),posAtConverter.y()); //note rotation from JLab->tracking
                else printWriter.format("%5.5f %5.5f ", -9999999.9,-9999999.9);
                HPSTrack hpstrk1 = new HPSTrack(helix1);
                Hep3Vector posAtConverterFringe1 = hpstrk1.getPositionAtZMap(100., BeamlineConstants.HARP_POSITION_TESTRUN, 5.0)[0];
                if (beamlinePosOk(posAtConverterFringe1)) printWriter.format("%5.5f %5.5f %5.5f ", posAtConverterFringe1.z(),posAtConverterFringe1.x(),posAtConverterFringe1.y()); //note rotation from JLab->tracking
                else printWriter.format("%5.5f %5.5f %5.5f ", -9999999.9,-9999999.9,-9999999.9);
                
                Hep3Vector posAtNomTarget1 = TrackUtils.extrapolateTrack(trk1,0);
                if(beamlinePosOk(posAtNomTarget1)) printWriter.format("%5.5f %5.5f ", posAtNomTarget1.x(),posAtNomTarget1.y()); //note rotation from JLab->tracking
                else printWriter.format("%5.5f %5.5f ", -9999999.9,-9999999.9);

                Hep3Vector posAtNomTargetFringe1 = hpstrk1.getPositionAtZMap(100., 0.0, 5.0)[0];
                if (beamlinePosOk(posAtNomTargetFringe1)) printWriter.format("%5.5f %5.5f %5.5f ", posAtNomTargetFringe1.z(),posAtNomTargetFringe1.x(),posAtNomTargetFringe1.y()); //note rotation from JLab->tracking
                else printWriter.format("%5.5f %5.5f %5.5f ", -9999999.9,-9999999.9,-9999999.9);

                Hep3Vector posAtECal = TrackUtils.extrapolateTrack(trk1,BeamlineConstants.ECAL_FACE_TESTRUN);  
                Hep3Vector posAtECalFringe1 = hpstrk1.getPositionAtZMap(BeamlineConstants.DIPOLE_EDGE_TESTRUN - 100, BeamlineConstants.ECAL_FACE_TESTRUN, 5.0, false)[0];
                if(beamlinePosOk(posAtECal)) {
                	//printWriter.format("%5.5f %5.5f ",posAtECal.x(),posAtECal.y()); //note rotation from JLab->tracking
                	printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",posAtECal.x(),posAtECal.y(),posAtECalFringe1.z(),posAtECalFringe1.x(),posAtECalFringe1.y()); //note rotation from JLab->tracking
                } 
                else printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9,-9999999.9,-9999999.9);
                
                Cluster matched_cluster = findEcalCluster(trk1, clusters);
                if(matched_cluster !=null) {
                	double[] pos_cluster = matched_cluster.getPosition();
                	posAtECal = TrackUtils.extrapolateTrack(trk1,pos_cluster[2]);
                	if(beamlinePosOk(posAtECal)) {
                		posAtECalFringe1 = hpstrk1.getPositionAtZMap(BeamlineConstants.DIPOLE_EDGE_TESTRUN - 100, pos_cluster[2], 5.0, false)[0];
                		printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",posAtECal.x(),posAtECal.y(),posAtECalFringe1.z(),posAtECalFringe1.x(),posAtECalFringe1.y()); //note rotation from JLab->tracking
						if(_debug) {
							System.out.printf("clpos:%5.5f %5.5f %5.5f  trk: %5.5f %5.5f %5.5f %5.5f %5.5f \n",pos_cluster[0],pos_cluster[1],pos_cluster[2],posAtECal.x(),posAtECal.y(),posAtECalFringe1.z(),posAtECalFringe1.x(),posAtECalFringe1.y()); //note rotation from JLab->tracking

							if(clusters.size()==1) {
								if(pos_cluster[1] >0) {
									_trkmatch_top_dy.fill(pos_cluster[1] - posAtECal.y());
									if(trk1.getCharge()>0) {
										_trkmatch_top_plus_dx.fill(pos_cluster[0] - posAtECal.x());
									} else {
										_trkmatch_top_minus_dx.fill(pos_cluster[0] - posAtECal.x());
									}
								} else {
									_trkmatch_bot_dy.fill(pos_cluster[1] - posAtECal.y());
									if(trk1.getCharge()>0) {
										_trkmatch_bot_plus_dx.fill(pos_cluster[0] - posAtECal.x());
									} else {
										_trkmatch_bot_minus_dx.fill(pos_cluster[0] - posAtECal.x());
									}
								}
							}
						}
                	} 
                	else printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9,-9999999.9,-9999999.9);
                } 
                else printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9,-9999999.9,-9999999.9);
                
                

                
                if(beamlinePosOk(posAtConverter)) {
                    if(TrackUtils.isTopTrack(trk1, 4)) {
                        if(trk1.getCharge()>0) {
                            this._trk_y_at_conv_top_pos.fill(posAtConverter.x());
                            this._trk_z_at_conv_top_pos.fill(posAtConverter.y());
                        } else {
                            this._trk_y_at_conv_top_neg.fill(posAtConverter.x());
                            this._trk_z_at_conv_top_neg.fill(posAtConverter.y());
                        }
                    } else {
                        if(trk1.getCharge()>0) {
                            this._trk_y_at_conv_bot_pos.fill(posAtConverter.x());
                            this._trk_z_at_conv_bot_pos.fill(posAtConverter.y());
                        } else {
                            this._trk_y_at_conv_bot_neg.fill(posAtConverter.x());
                            this._trk_z_at_conv_bot_neg.fill(posAtConverter.y());
                        }
                    }
                }
                if(beamlinePosOk(posAtConverterFringe1)) {
                    if(TrackUtils.isTopTrack(trk1, 4)) {
                        if(trk1.getCharge()>0) {
                            this._trk_y_at_conv_top_pos_fr.fill(posAtConverterFringe1.x());
                            this._trk_z_at_conv_top_pos_fr.fill(posAtConverterFringe1.y());
                        } else {
                            this._trk_y_at_conv_top_neg_fr.fill(posAtConverterFringe1.x());
                            this._trk_z_at_conv_top_neg_fr.fill(posAtConverterFringe1.y());
                        }
                    } else {
                        if(trk1.getCharge()>0) {
                            this._trk_y_at_conv_bot_pos_fr.fill(posAtConverterFringe1.x());
                            this._trk_z_at_conv_bot_pos_fr.fill(posAtConverterFringe1.y());
                        } else {
                            this._trk_y_at_conv_bot_neg_fr.fill(posAtConverterFringe1.x());
                            this._trk_z_at_conv_bot_neg_fr.fill(posAtConverterFringe1.y());
                        }
                    }
                }
                
                
                
            }
            else {

                printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9,-9999999.9,-9999999.9);
                printWriter.format("%5d %5.5f %5.5f %5.5f %5.5f %5d ",-9999999,-9999999.9, -9999999.9,-9999999.9,-9999999.9,-9999999);
                for(int iLayer=0;iLayer<5;++iLayer) {
                    printWriter.format("%5.5f %5.5f %5.5f ", -9999999.9, -9999999.9, -9999999.9);
                }
                for(int iLayer=0;iLayer<5;++iLayer) {
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                        printWriter.format("%5.5f %5.5f ", -9999999.9, -9999999.9);
                }
                for(int iLayer=1;iLayer<=10;++iLayer) {
                    printWriter.format("%5.5f %5.5f %5.5f ", -99999999.9, -99999999.9, -99999999.9);
                }
                
                printWriter.format("%5.5f %5.5f ",-9999999.9,-9999999.9); //note rotation from JLab->tracking
                printWriter.format("%5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9); //note rotation from JLab->tracking
                printWriter.format("%5.5f %5.5f ",-9999999.9,-9999999.9); //note rotation from JLab->tracking
                printWriter.format("%5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9); //note rotation from JLab->tracking
                printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9,-9999999.9,-9999999.9); //note rotation from JLab->tracking
                printWriter.format("%5.5f %5.5f %5.5f %5.5f %5.5f ",-9999999.9,-9999999.9,-9999999.9,-9999999.9,-9999999.9); //note rotation from JLab->tracking
                
            }
        }
        //printWriter.format("\n%s\n","X1");



        //printWriter.format("\n%s\n","X2");


        

        HashMap<Integer,List<SiTrackerHitStrip1D>> allstriphits = this.getAllStripHitsMap(event,true);
        //System.out.printf("%s: %d strip hits in event\n",this.getClass().getSimpleName(),allstriphits.size());
        for(int iLayer=1;iLayer<=10;++iLayer) {
            if(allstriphits.containsKey(iLayer)) {
                printWriter.format("%5d ", allstriphits.get(iLayer).size());
                //System.out.printf("%s: layer %d has %d strip hits\n",this.getClass().getSimpleName(),iLayer,allstriphits.get(iLayer).size());
            }
            else {
                printWriter.format("%5d ", -99999999);
                //System.out.printf("%s: layer %d has 0 strip hits\n",this.getClass().getSimpleName(),iLayer);
            }
        }
        allstriphits = this.getAllStripHitsMap(event,false);
        for(int iLayer=1;iLayer<=10;++iLayer) {
            if(allstriphits.containsKey(iLayer)) printWriter.format("%5d ", allstriphits.get(iLayer).size());
            else printWriter.format("%5d ", -99999999);
        }

        List<HelicalTrackHit> stereoHits = new ArrayList<HelicalTrackHit>();
        if(event.hasCollection(HelicalTrackHit.class, stereoHitCollectionName)) {
            stereoHits = event.get(HelicalTrackHit.class, stereoHitCollectionName);
        } 

       
        HashMap<Integer,List<HelicalTrackHit>> allstereohits = getAllStereoHitsMap(stereoHits,true);
        for(int iLayer=1;iLayer<=10;iLayer+=2) {
            if(allstereohits.containsKey(iLayer)) printWriter.format("%5d ", allstereohits.get(iLayer).size());
            else printWriter.format("%5d ", -99999999);
        }
        allstereohits = getAllStereoHitsMap(stereoHits,false);
        for(int iLayer=1;iLayer<=10;iLayer+=2) {
            if(allstereohits.containsKey(iLayer)) printWriter.format("%5d ", allstereohits.get(iLayer).size());
            else printWriter.format("%5d ", -99999999);
        }
        //printWriter.format("\n%s\n","X4");
        
        //Particle vtx
        if(vtxPosParticle!=null) printWriter.format("%5.5f %5.5f %5.5f ", vtxPosParticle.x(),vtxPosParticle.y(),vtxPosParticle.z() );
        else printWriter.format("%5.5f %5.5f %5.5f ", -9999999., -9999999., -9999999. );
        //Track vtx
        if(vtxPos!=null) printWriter.format("%5.5f %5.5f %5.5f ", vtxPos.x(),vtxPos.y(),vtxPos.z() );
        else printWriter.format("%5.5f %5.5f %5.5f ", -9999999., -9999999., -9999999. );
        if(vtxPosFr!=null) printWriter.format("%5.5f %5.5f %5.5f ", vtxPosFr.x(),vtxPosFr.y(),vtxPosFr.z() );
        else printWriter.format("%5.5f %5.5f %5.5f ", -9999999., -9999999., -9999999. );
        if(vtxPosNonBend!=null) printWriter.format("%5.5f %5.5f %5.5f ", vtxPosNonBend.x(),vtxPosNonBend.y(),vtxPosNonBend.z() );
        else printWriter.format("%5.5f %5.5f %5.5f ", -9999999., -9999999., -9999999. );
        int ncl_t=0; int ncl_b=0;
        for(int i=0;i<3;++i) {
            if(clusters==null) {
                printWriter.format("%5.5f %5d %5d %5.5f %5.5f %5.5f %5d ",-999999.9,-999999,-999999,-999999.,-999999.,-999999.,-999999);
            }
            else if(clusters.size()<=i) {
                printWriter.format("%5.5f %5d %5d %5.5f %5.5f %5.5f %5d ",-999999.9,-999999,-999999,-999999.,-999999.,-999999.,-999999);
            } else {
                //for(Cluster cl : clusters) {
                int iy = clusters.get(i).getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                int ix = clusters.get(i).getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                double pos[] = clusters.get(i).getPosition();
                double E = clusters.get(i).getEnergy();
                int clsize = clusters.get(i).getSize();
                printWriter.format("%5.5f %5d %5d %5.5f %5.5f %5.5f %5d ",E,ix,iy,pos[0],pos[1],pos[2],clsize);
                if( iy > 0) ncl_t++;
                else ncl_b++;
            }
        }
        printWriter.format("%5d %5d ",ncl_t,ncl_b);
        
        GenericObject triggerData = getTriggerInfo(event);
        if(triggerData==null) {
        	printWriter.format("%5d %5d",0,0);
        }
        else {
        	printWriter.format("%5d %5d",TriggerData.getTopTrig(triggerData),TriggerData.getBotTrig(triggerData));
        	if(_debug) {
        		System.out.printf("trigger top %d  bot %d  OR %d AND %d\n", TriggerData.getTopTrig(triggerData), TriggerData.getBotTrig(triggerData),TriggerData.getOrTrig(triggerData),TriggerData.getAndTrig(triggerData));
        	}
        }
        printWriter.println();
        
    }
    
    
    /**
     * Check that object is not null or undefined
     * @param pos to check
     * @return
     */
    private boolean beamlinePosOk(Hep3Vector pos) {
		if( pos == null) {
			return false;
		} else if ( Double.isNaN(pos.x()) || Double.isNaN(pos.y()) || Double.isNaN(pos.z()) ) {
			return false;
		} else {
			return true;
		}
	}

    private GenericObject getTriggerInfo(EventHeader event) {
        if(event.hasCollection(TriggerData.class, triggerDecisionCollectionName)) {
           
            List<TriggerData> triggerDataList = event.get(TriggerData.class, "TriggerBank");
            if(triggerDataList.isEmpty()) {
                if(_debug) 
                    System.out.println( "Event has trigger bank exists but is empty");
                return null;
            } else {
                if(_debug) 
                    System.out.println( "Event has trigger bank");
                return triggerDataList.get(0);
            }
        }
        else if (event.hasCollection(GenericObject.class, triggerDecisionCollectionName)) {
        	List<GenericObject> triggerList = event.get(GenericObject.class, triggerDecisionCollectionName);
            if (!triggerList.isEmpty()) {
            	if(_debug) 
                    System.out.println( "Event has trigger generic bank");
                GenericObject triggerData = triggerList.get(0);
                return triggerData;
            } 
            else {
            	if(_debug) 
                    System.out.println( "Event has trigger generic bank exists but is empty");
            	return null;
            }
        }
        else {
        	if(_debug) { 
        		System.out.printf( "%s: Event %d has NO trigger bank\n",this.getClass().getSimpleName(),event.getEventNumber());
        	}
        	return null;
        }
        
    }
                

    private HashMap<Integer,HelicalTrackHit> getHitMap(List<TrackerHit> hits,HelicalTrackFit helix) {
        HashMap<Integer,HelicalTrackHit> map = new HashMap<Integer,HelicalTrackHit>();
        for(TrackerHit hit : hits) {
            HelicalTrackHit hth = (HelicalTrackHit) hit;
            map.put(hth.Layer(), hth);
        }
        return map;
    }
    

    private HashMap<Integer,List<HelicalTrackStrip>> getStripHitsMap(List<TrackerHit> hits) {
        HashMap<Integer,List<HelicalTrackStrip>> map = new HashMap<Integer,List<HelicalTrackStrip>>();
        for(TrackerHit hit : hits) {
            HelicalTrackHit hth = (HelicalTrackHit) hit;
            HelicalTrackCross htc = (HelicalTrackCross) hth;
            HelicalTrackStrip s1 = htc.getStrips().get(0);
            HelicalTrackStrip s2 = htc.getStrips().get(1);
            if(!map.containsKey(s1.layer())) map.put(s1.layer(), new ArrayList<HelicalTrackStrip>());
            if(!map.containsKey(s2.layer())) map.put(s2.layer(), new ArrayList<HelicalTrackStrip>());
            map.get(s1.layer()).add(s1);
            map.get(s2.layer()).add(s2);
        }
        return map;
    }
    
    private HashMap<Integer,List<SiTrackerHitStrip1D>> getAllStripHitsMap(EventHeader event, boolean top) {
        HashMap<Integer,List<SiTrackerHitStrip1D>> map = new HashMap<Integer,List<SiTrackerHitStrip1D>>();
        if(!event.hasCollection(SiTrackerHitStrip1D.class, this._stripClusterCollectionName)) {
            return map;
        }
        List<SiTrackerHitStrip1D> strips = event.get(SiTrackerHitStrip1D.class, this._stripClusterCollectionName);
        if(this._debug) System.out.printf("%s: found %d strips in clollection, asking strips in the %s\n", this.getClass().getSimpleName(),strips.size(),(top?"top":"bottom"));
        for(SiTrackerHitStrip1D strip : strips) {
            IDetectorElement de = strip.getSensor();
            HpsSiSensor sensor = (HpsSiSensor) de;
            int lyr = _ID.getLayer(de);
            //===> if(!top && SvtUtils.getInstance().isTopLayer(sensor)) continue;
            if(!top && sensor.isTopLayer()) continue;
            //else if (top && !SvtUtils.getInstance().isTopLayer(sensor)) continue;
            else if (top && !sensor.isTopLayer()) continue;
            if(this._debug) System.out.printf("%s: strip \"%s\" at %s is selected\n", this.getClass().getSimpleName(),_ID.getName(de),strip.getPositionAsVector().toString());
            if(!map.containsKey(lyr)) {
                map.put(lyr, new ArrayList<SiTrackerHitStrip1D>());
            }
            map.get(lyr).add(strip);
        }
        
        return map;
    }
    
    private HashMap<Integer,List<HelicalTrackHit>> getAllStereoHitsMap(List<HelicalTrackHit> stereoHits, boolean top) {
        HashMap<Integer,List<HelicalTrackHit>> map = new HashMap<Integer,List<HelicalTrackHit>>();
        if(stereoHits==null) {
            return map;
        }
        if(this._debug) System.out.printf("%s: asking for stereo hits in the %s\n", this.getClass().getSimpleName(),(top?"top":"bottom"));
        for(HelicalTrackHit hit : stereoHits) {
            if(top && hit.z()<0) continue;
            if(!top && hit.z()>0) continue;
            if(this._debug) System.out.printf("%s: hit at xyz=%.3f,%.3f,%.3f is selected\n", this.getClass().getSimpleName(),hit.x(),hit.y(),hit.z());
            if(!map.containsKey(hit.Layer())) {
                map.put(hit.Layer(), new ArrayList<HelicalTrackHit>());
            }
            map.get(hit.Layer()).add(hit);
        }
        return map;
    }
    
    private int[] getNtracks(List<CmpTrack> tracks, double min_px) {
        //System.out.printf("%s: getNtracks for %d tracks with min_px=%.3f \n",this.getClass().getSimpleName(),tracks.size(),min_px);
        int n[] = {0,0};
        for(CmpTrack track : tracks) {
            if(track._track.getTrackStates().get(0).getMomentum()[0]<min_px) {
                continue;
            }
            //System.out.printf("%s: track had enough px=%f\n",this.getClass().getSimpleName(),track._track.getTrackStates().get(0).getMomentum()[0]);
            
            List<TrackerHit> hitsOnTrack = track._track.getTrackerHits();
            for(TrackerHit hit : hitsOnTrack) {
                double y = hit.getPosition()[1];
                if(y>0) {
                    //System.out.printf("%s: this track (chi2=%f) is a top track\n",this.getClass().getSimpleName(),track._track.getChi2());
                    n[0]++;
                    break;
                } else if(y<0) {
                    //System.out.printf("%s: this track (chi2=%f) is a bot track\n",this.getClass().getSimpleName(),track._track.getChi2());
                    n[1]++;
                    break;
                }
            }
        }
        //System.out.printf("%s: found %d top and %d bot tracks\n",this.getClass().getSimpleName(),n[0],n[1]);

        return n;
    }

    
    
      private void makePlots() {
        _vtxpos_x = aida.histogram1D("Vertex position X", 100, -800, -500);
        _vtxpos_y = aida.histogram1D("Vertex position Y", 100, 0, 40);
        _vtxpos_z = aida.histogram1D("Vertex position Z", 100, -4, 4);
        _vtxposfr_x = aida.histogram1D("Vertex fr position X", 100, -800, -500);
        _vtxposfr_y = aida.histogram1D("Vertex fr position Y", 100, 0, 40);
        _vtxposfr_z = aida.histogram1D("Vertex fr position Z", 100, -4, 4);
        _vtxposnonb_x = aida.histogram1D("Vertex position non-bend X", 100, -800, -500);
        _vtxposnonb_y = aida.histogram1D("Vertex position non-bend Y", 100, 0, 40);
        _vtxposnonb_dy = aida.histogram1D("Vertex position non-bend Ytrk1-Ytrk2", 100, -20, 20);
        _vtxposnonb_z = aida.histogram1D("Vertex position non-bend Z", 100, -4, 4);
        _partvtxpos_x = aida.histogram1D("Particle Vertex position X", 100, -1000, 0);
        _partvtxpos_y = aida.histogram1D("Particle Vertex position Y", 100, -20, 60);
        _partvtxpos_z = aida.histogram1D("Particle Vertex position Z", 100, -5, 5);
        _vtxposnonb_xAtZ0 = aida.histogram1D("SLT position non-bend X at Z=0", 100, -1000, 0);
        _vtxposnonb_zAtTarget = aida.histogram1D("SLT position non-bend Z at target", 100, -5, 5);
        _vtxposnonb_angle1 = aida.histogram1D("SLT thetay non-bend (trk1)", 100, -0.05, 0.05);
        _vtxposnonb_angle2 = aida.histogram1D("SLT thetay non-bend (trk2)", 100, -0.05, 0.05);

        _ntrks_px = aida.histogram2D("Track multiplicity vs px cut", 6, 0, 0.6,8,0,8);
        
        _trk_y_at_conv_top_pos = aida.histogram1D("Track y @ converter top +", 100, -30,60);
        _trk_z_at_conv_top_pos = aida.histogram1D("Track z @ converter top +", 100, -20,20);
        _trk_y_at_conv_top_pos_fr = aida.histogram1D("Track y @ converter top + (fr)", 100, -30, 60);
        _trk_z_at_conv_top_pos_fr = aida.histogram1D("Track z @ converter top + (fr)", 100, -20, 20);
        _trk_y_at_conv_bot_pos = aida.histogram1D("Track y @ converter bot +", 100, -30,60);
        _trk_z_at_conv_bot_pos = aida.histogram1D("Track z @ converter bot +", 100, -20,20);
        _trk_y_at_conv_bot_pos_fr = aida.histogram1D("Track y @ converter bot + (fr)", 100, -30, 60);
        _trk_z_at_conv_bot_pos_fr = aida.histogram1D("Track z @ converter bot + (fr)", 100, -20, 20);

        _trk_y_at_conv_top_neg = aida.histogram1D("Track y @ converter top -", 100, -30,60);
        _trk_z_at_conv_top_neg = aida.histogram1D("Track z @ converter top -", 100, -20,20);
        _trk_y_at_conv_top_neg_fr = aida.histogram1D("Track y @ converter top - (fr)", 100, -30, 60);
        _trk_z_at_conv_top_neg_fr = aida.histogram1D("Track z @ converter top - (fr)", 100, -20, 20);
        _trk_y_at_conv_bot_neg = aida.histogram1D("Track y @ converter bot -", 100, -30,60);
        _trk_z_at_conv_bot_neg = aida.histogram1D("Track z @ converter bot -", 100, -20,20);
        _trk_y_at_conv_bot_neg_fr = aida.histogram1D("Track y @ converter bot - (fr)", 100, -30, 60);
        _trk_z_at_conv_bot_neg_fr = aida.histogram1D("Track z @ converter bot - (fr)", 100, -20, 20);

        
        _trkmatch_top_dy = aida.histogram1D("dy(cl,track) top", 50, -60,60);
        _trkmatch_top_dx = aida.histogram1D("dx(cl,track) top", 50, -60,60);
        _trkmatch_bot_dy = aida.histogram1D("dy(cl,track) bot", 50, -60,60);
        _trkmatch_bot_dx = aida.histogram1D("dx(cl,track) bot", 50, -60,60);

        _trkmatch_top_plus_dx = aida.histogram1D("dx(cl,track) top plus", 50, -60,60);
        _trkmatch_bot_plus_dx = aida.histogram1D("dx(cl,track) bot plus", 50, -60,60);
        _trkmatch_top_minus_dx = aida.histogram1D("dx(cl,track) top minus", 50, -60,60);
        _trkmatch_bot_minus_dx = aida.histogram1D("dx(cl,track) bot minus", 50, -60,60);
       
        
        
        _plotterTrackVertex = aida.analysisFactory().createPlotterFactory().create();
        _plotterTrackVertex.createRegions(2,2);
        _plotterTrackVertex.region(0).plot(_vtxpos_x);
        _plotterTrackVertex.region(1).plot(_vtxpos_y);
        _plotterTrackVertex.region(2).plot(_vtxpos_z);
        _plotterTrackVertexFr = aida.analysisFactory().createPlotterFactory().create();
        _plotterTrackVertexFr.createRegions(2,2);
        _plotterTrackVertexFr.region(0).plot(_vtxposfr_x);
        _plotterTrackVertexFr.region(1).plot(_vtxposfr_y);
        _plotterTrackVertexFr.region(2).plot(_vtxposfr_z);
        _plotterTrackVertexNonBend = aida.analysisFactory().createPlotterFactory().create();
        _plotterTrackVertexNonBend.createRegions(2,4);
        _plotterTrackVertexNonBend.region(0).plot(_vtxposnonb_x);
        _plotterTrackVertexNonBend.region(1).plot(_vtxposnonb_y);
        _plotterTrackVertexNonBend.region(2).plot(_vtxposnonb_z);
        _plotterTrackVertexNonBend.region(3).plot(_vtxposnonb_dy);
        _plotterTrackVertexNonBend.region(4).plot(_vtxposnonb_xAtZ0);
        _plotterTrackVertexNonBend.region(5).plot(_vtxposnonb_zAtTarget);
        _plotterTrackVertexNonBend.region(6).plot(_vtxposnonb_angle1);
        _plotterTrackVertexNonBend.region(7).plot(_vtxposnonb_angle2);
        _plotterParticleVertex = aida.analysisFactory().createPlotterFactory().create();
        _plotterParticleVertex.createRegions(2,2);
        _plotterParticleVertex.region(0).plot(_partvtxpos_x);
        _plotterParticleVertex.region(1).plot(_partvtxpos_y);
        _plotterParticleVertex.region(2).plot(_partvtxpos_z);
        _plotterTrackMult = aida.analysisFactory().createPlotterFactory().create();
        _plotterTrackMult.createRegions(1,1);
        _plotterTrackMult.region(0).plot(_ntrks_px);
        _plotterTrackAtConv = aida.analysisFactory().createPlotterFactory().create();
        _plotterTrackAtConv.createRegions(4,4);
        _plotterTrackAtConv.region(0).plot(_trk_y_at_conv_top_pos);
        _plotterTrackAtConv.region(4).plot(_trk_z_at_conv_top_pos);
        _plotterTrackAtConv.region(8).plot(_trk_y_at_conv_top_pos_fr);
        _plotterTrackAtConv.region(12).plot(_trk_z_at_conv_top_pos_fr);
        _plotterTrackAtConv.region(2).plot(_trk_y_at_conv_bot_pos);
        _plotterTrackAtConv.region(6).plot(_trk_z_at_conv_bot_pos);
        _plotterTrackAtConv.region(10).plot(_trk_y_at_conv_bot_pos_fr);
        _plotterTrackAtConv.region(14).plot(_trk_z_at_conv_bot_pos_fr);
        _plotterTrackAtConv.region(1).plot(_trk_y_at_conv_top_neg);
        _plotterTrackAtConv.region(5).plot(_trk_z_at_conv_top_neg);
        _plotterTrackAtConv.region(9).plot(_trk_y_at_conv_top_neg_fr);
        _plotterTrackAtConv.region(13).plot(_trk_z_at_conv_top_neg_fr);
        _plotterTrackAtConv.region(3).plot(_trk_y_at_conv_bot_neg);
        _plotterTrackAtConv.region(7).plot(_trk_z_at_conv_bot_neg);
        _plotterTrackAtConv.region(11).plot(_trk_y_at_conv_bot_neg_fr);
        _plotterTrackAtConv.region(15).plot(_trk_z_at_conv_bot_neg_fr);
        _plotterTrackMatch = aida.analysisFactory().createPlotterFactory().create();
        _plotterTrackMatch.createRegions(2,3);
        _plotterTrackMatch.region(0).plot(_trkmatch_top_plus_dx);
        _plotterTrackMatch.region(1).plot(_trkmatch_top_minus_dx);
        _plotterTrackMatch.region(2).plot(_trkmatch_bot_plus_dx);
        _plotterTrackMatch.region(3).plot(_trkmatch_bot_minus_dx);
        _plotterTrackMatch.region(4).plot(_trkmatch_top_dy);
        _plotterTrackMatch.region(5).plot(_trkmatch_bot_dy);
        _plotterParticleVertex.setTitle("MC particle Vertex");
        _plotterTrackVertex.setTitle("Two Track Vertex");
        _plotterTrackVertexNonBend.setTitle("Two Track Vertex Non Bend");
        _plotterTrackMult.setTitle("Track multiplicity");
        _plotterTrackAtConv.setTitle("Track @ converter");
        _plotterTrackMatch.setTitle("Track-cluster matching");
        
        for(int i=0;i<3;++i) {
            ((PlotterRegion) _plotterParticleVertex.region(i)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) _plotterParticleVertex.region(i)).getPlot().setAllowPopupMenus(true);
            ((PlotterRegion) _plotterTrackVertex.region(i)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) _plotterTrackVertex.region(i)).getPlot().setAllowPopupMenus(true);
            ((PlotterRegion) _plotterTrackVertexNonBend.region(i)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) _plotterTrackVertexNonBend.region(i)).getPlot().setAllowPopupMenus(true);
			((PlotterRegion) _plotterTrackMatch.region(i)).getPlot().setAllowUserInteraction(true);
			((PlotterRegion) _plotterTrackMatch.region(i)).getPlot().setAllowPopupMenus(true);
            if(i<2) {
            	if(i==0) {
            		((PlotterRegion) _plotterTrackMult.region(i)).getPlot().setAllowUserInteraction(true);
            		((PlotterRegion) _plotterTrackMult.region(i)).getPlot().setAllowPopupMenus(true);
            	}
            }
        }
        
        if(!this.hideFrame) {
            this._plotterParticleVertex.show();
            this._plotterTrackVertex.show();
            this._plotterTrackVertexFr.show();
            this._plotterTrackVertexNonBend.show();
            //this._plotterTrackMult.show();
            _plotterTrackAtConv.show();
            _plotterTrackMatch.show();
        } 
    }

      
    private void createWriter() {
    	try {
            fileWriter = new FileWriter(outputNameTextTuple);
        } catch (IOException ex) {
            Logger.getLogger(ROOTFlatTupleDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        printWriter = new PrintWriter(fileWriter);
    }
    
    
    
    /*
     * Find the path length where the two helix cross in the non-bend plane
     */
    
    private double getCrossingS(Track trk1, Track trk2) {
        double slope_1 = ((SeedTrack)trk1).getSeedCandidate().getHelix().slope();
        double slope_2 = ((SeedTrack)trk2).getSeedCandidate().getHelix().slope();
        double z0_1 = ((SeedTrack)trk1).getSeedCandidate().getHelix().z0();
        double z0_2 = ((SeedTrack)trk2).getSeedCandidate().getHelix().z0();        
        double s = getPathLengthCrossingPoint(slope_1, z0_1, slope_2, z0_2);
        double zAtCross = z0_1 + s*slope_1;
        //System.out.printf("%s: s1=%f z1=%f s2=%f z2=%f => path=%f and zAtCross=%f\n",this.getClass().getSimpleName(),slope_1,z0_1,slope_2,z0_2,s,zAtCross);
        return zAtCross;
    }
    
    
    private StraightLineTrack[] getSLTs(Track trk1, Track trk2, boolean useFringe) {
        // find the point on the x- and y-axis by 
        // 1) go outside the fringe region 
        // 2) assume straight lines 
        // 3) solve for the position where the z-position is at the crossing point
        double zStart = useFringe==true ? -100. : 0.;
        StraightLineTrack slt1 = TrackUtils.findSLTAtZ(trk1,zStart,useFringe);
        StraightLineTrack slt2 = TrackUtils.findSLTAtZ(trk2,zStart,useFringe);
        StraightLineTrack[] vv = {slt1,slt2};
        return vv;
    }
    
    
     

    
    /*
     * Calculate the point where two 1-D lines cross
     * Use SZ coordinate system nomenclature
     * Parameters:
     * slope: slope of track in SZ plane
     * z0: z-coordinate at which S=0
     */
    private double getPathLengthCrossingPoint(double slope_1,double z0_1,double slope_2,double z0_2) {
        double s; //path length
        s = (z0_1-z0_2)/(slope_2-slope_1);
        return s;
    }

    /**
     * Print out informatoin about ecal geometry
     * @param detector
     */
    private void printEcalInfo(Detector detector) {
    Subdetector det = detector.getSubdetector("Ecal");
    if(det!=null) {
    	System.out.printf("found %s with %d children:\n",det.getName(),det.getDetectorElement().getChildren().size());
    	System.out.printf("%5s %5s %45s %45s\n","ix","iy","global pos"," translation");
    	for(IDetectorElement idet : det.getDetectorElement().getChildren()) {
        		//System.out.printf("%s\n",idet.getName());
        		IIdentifierHelper helper = idet.getIdentifierHelper();
        		IExpandedIdentifier expId = idet.getExpandedIdentifier();
        		int ix = expId.getValue(helper.getFieldIndex("ix"));
        		int iy = expId.getValue(helper.getFieldIndex("iy"));
        		System.out.printf("%5d %5d %45s %45s\n",ix,iy, idet.getGeometry().getPosition().toString(),idet.getGeometry().getPhysicalVolume().getTranslation().toString());
        }
    }
    else {
    	System.out.printf("found no ecal\n");
    }
    }
    
}
