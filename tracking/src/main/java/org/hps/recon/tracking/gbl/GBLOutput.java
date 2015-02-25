/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.gbl;
import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.DetectorPlane;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.MultipleScattering.ScatterPoint;
import org.hps.recon.tracking.MultipleScattering.ScatterPoints;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.lcsim.constants.Constants;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;


/**
 * Calculate the input needed for Millepede minimization.
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class GBLOutput {
    
    private int _debug = 0;
    private GBLFileIO textFile = null;
    private Hep3Vector _B;
	private TrackerHitUtils _trackerHitUtils = new TrackerHitUtils();
    private MaterialSupervisor _materialmanager;
    private MultipleScattering _scattering;
    private double _beamEnergy = 2.2; //GeV
	private boolean AprimeEvent = false; // do extra checks
	private boolean hasXPlanes = false;
    
    

    
    /**
     * Constructor
     * @param outputFileName is the filename given to the text-based output file. If empty no output file is written
     * @param bfield magnetic field in Tesla
     */
    GBLOutput(String outputFileName,Hep3Vector bfield) {
    	//System.out.printf("name \"%s\" \n", outputFileName);
    	if(!outputFileName.equalsIgnoreCase("")) {
    		textFile = new GBLFileIO(outputFileName);
    	}
        _materialmanager = new MaterialSupervisor();
        _scattering = new MultipleScattering(_materialmanager);
        _B = CoordinateTransformations.transformVectorToTracking(bfield);
        _scattering.setBField(Math.abs(_B.z())); // only absolute of B is needed as it's used for momentum calculation only
    }

    public void setDebug(int debug) {
        _debug = debug;
        _scattering.setDebug(_debug>0?true:false);
    }
    public void buildModel(Detector detector) {
        _materialmanager.buildModel(detector);
    }
    void printNewEvent(int eventNumber,double Bz) {
    	if(textFile != null) {
    		textFile.printEventInfo(eventNumber,Bz);
    	}
    }
    void printTrackID(int iTrack) {
    	if(textFile != null) {
    		textFile.printTrackID(iTrack);
    	}
    }
    void close() {
    	if(textFile != null) {
    		textFile.closeFile();
    	}
    }
    void setAPrimeEventFlag(boolean flag) {
    	this.AprimeEvent = flag;
    }
    void setXPlaneFlag(boolean flag) {
    	this.hasXPlanes = flag;
    }
    public Hep3Vector get_B() {
		return _B;
	}
	public void set_B(Hep3Vector _B) {
		this._B = _B;
	}


    
    void printGBL(Track trk, GBLTrackData gtd, List<GBLStripClusterData> stripClusterDataList, List<MCParticle> mcParticles, List<SimTrackerHit> simTrackerHits, boolean isMC) {

        SeedTrack st = (SeedTrack)trk;
        SeedCandidate seed = st.getSeedCandidate();
        HelicalTrackFit htf = seed.getHelix();          

        // Find scatter points along the path
        ScatterPoints scatters = _scattering.FindHPSScatterPoints(htf);
        
        // Hits on track
        List<HelicalTrackHit> hits = seed.getHits();

        // Find the truth particle of the track
        MCParticle mcp = null;
        
        if(isMC) {
        	mcp = getMatchedTruthParticle(trk);
        
        	if(mcp==null) {
        		System.out.printf("%s: no truth particle found in event!\n",this.getClass().getSimpleName());
        		this.printMCParticles(mcParticles);
        		System.exit(1);
        	} else {
        		if(_debug>0) System.out.printf("%s: truth particle (pdgif %d ) found in event!\n",this.getClass().getSimpleName(),mcp.getPDGID());
        	}
        
        	if(AprimeEvent ) {
        		checkAprimeTruth(mcp,mcParticles);
        	}
        }
        
        // Get track parameters from MC particle 
        HelicalTrackFit htfTruth = isMC ? TrackUtils.getHTF(mcp,-1.0*this._B.z()) : null;
        
        // Use the truth helix as the initial track for GBL?
        //htf = htfTruth;
                
        
        // Get perigee parameters to curvilinear frame
        PerigeeParams perPar = new PerigeeParams(htf);
        PerigeeParams perParTruth = new PerigeeParams(htfTruth);
        if(textFile != null) {
        	textFile.printPerTrackParam(perPar);
        	textFile.printPerTrackParamTruth(perParTruth);
        }
        
        //GBLDATA
        gtd.setPerigeeTrackParameters(perPar);

        // Get curvilinear parameters
        ClParams clPar = new ClParams(htf);
        ClParams clParTruth = new ClParams(htfTruth);
        if(textFile != null) {
        	textFile.printClTrackParam(clPar);
        	textFile.printClTrackParamTruth(clParTruth);
        
        	if(_debug>0) {
        		System.out.printf("%s\n",textFile.getClTrackParamStr(clPar));
        		System.out.printf("%s\n",textFile.getPerTrackParamStr(perPar));
        	}
        }
        
        
        // find the projection from the I,J,K to U,V,T curvilinear coordinates
        Hep3Matrix perToClPrj = getPerToClPrj(htf);
        
        if(textFile != null) {
        	textFile.printPerToClPrj(perToClPrj);
        }    
        
        //GBLDATA
        for(int row=0; row<perToClPrj.getNRows();++row) {
        	for(int col=0; col<perToClPrj.getNColumns();++col) {
        		gtd.setPrjPerToCl(row, col, perToClPrj.e(row, col));
        	}
        }
        
        
        
        //GBLDATA
        for(int row=0; row<perToClPrj.getNRows();++row) {
        	for(int col=0; col<perToClPrj.getNColumns();++col) {
        		gtd.setPrjPerToCl(row, col, perToClPrj.e(row, col));
        	}
        }
        
        
        // print chi2 of fit
        if(textFile != null) {
        	textFile.printChi2(htf.chisq(),htf.ndf());
        }
        
        // build map of layer to SimTrackerHits that belongs to the MC particle
        Map<Integer, SimTrackerHit> simHitsLayerMap = new HashMap<Integer, SimTrackerHit >(); 
        for (SimTrackerHit sh : simTrackerHits) {
            if(sh.getMCParticle()==mcp) {
            	int layer  = sh.getIdentifierFieldValue("layer");
                if(!simHitsLayerMap.containsKey(layer) || (sh.getPathLength() < simHitsLayerMap.get(layer).getPathLength()) ) {
                    simHitsLayerMap.put(layer, sh);
                }
            }
        }
        
        
        // covariance matrix from the fit
        if(textFile != null) {
        	textFile.printPerTrackCov(htf);
        }
        
        // dummy cov matrix for CL parameters
        BasicMatrix clCov = new BasicMatrix(5,5);
        initUnit(clCov);
        clCov = (BasicMatrix) MatrixOp.mult(0.1*0.1,clCov);
        
        if(textFile != null) {
        	textFile.printCLTrackCov(clCov);
        }
        
        if(_debug>0) {
            System.out.printf("%s: perPar covariance matrix\n%s\n",this.getClass().getSimpleName(),htf.covariance().toString());
            double chi2_truth = truthTrackFitChi2(perPar,perParTruth,htf.covariance());
            System.out.printf("%s: truth perPar chi2 %f\n",this.getClass().getSimpleName(),chi2_truth);
        }
        
        if(_debug>0) System.out.printf("%d hits\n",hits.size());
        

        int istrip = 0;
        for(int ihit=0;ihit!=hits.size();++ihit) {
            
            HelicalTrackHit hit = hits.get(ihit);
            HelicalTrackCross htc = (HelicalTrackCross) hit;
            List<HelicalTrackStrip> strips = htc.getStrips();
            
            for(HelicalTrackStrip stripOld : strips) {
                HelicalTrackStripGbl strip = new HelicalTrackStripGbl(stripOld, true);
                
                // find Millepede layer definition from DetectorElement
                IDetectorElement de = ((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement();
                HpsSiSensor sensor;
                if( de instanceof HpsSiSensor) {
                    sensor = (HpsSiSensor) de;
                } else {
                    throw new ClassCastException("Detector element " + de.getName() + " couldn't be casted to " + HpsSiSensor.class.getName());
                }

                int millepedeId = sensor.getMillepedeId();
               
                if(_debug>0) System.out.printf("%s: layer %d millepede %d (DE=\"%s\", origin %s) \n",this.getClass().getSimpleName(),strip.layer(), millepedeId, sensor.getName(), strip.origin().toString());
                
                if(textFile != null) {
                	textFile.printStrip(istrip,millepedeId);
                }
                
                //GBLDATA
                GBLStripClusterData stripData = new GBLStripClusterData(millepedeId);
                //Add to output list
                stripClusterDataList.add(stripData);
                
                
                
                //Center of the sensor
                Hep3Vector origin = strip.origin();                
                
                if(textFile != null) {
                	textFile.printOrigin(origin);
                }
                
                // associated 3D position of the crossing of this and it's stereo partner sensor
                if(textFile != null) {
                	textFile.printHitPos3D(hit.getCorrectedPosition());
                }
                
                //Find intercept point with sensor in tracking frame
                Hep3Vector trkpos = TrackUtils.getHelixPlaneIntercept(htf, strip, Math.abs(_B.z()));
                Hep3Vector trkposTruth = isMC ? TrackUtils.getHelixPlaneIntercept(htfTruth, strip, Math.abs(_B.z())) : new BasicHep3Vector(-999999.9,-999999.9,-999999.9);
                if(textFile != null) {
                	textFile.printStripTrackPos(trkpos);
                }
                if(_debug>0) {
                	System.out.printf("trkpos at intercept [%.10f %.10f %.10f]\n",trkpos.x(),trkpos.y(),trkpos.z());
                    System.out.printf("trkposTruth at intercept %s\n",trkposTruth.toString());
                }
                
                // cross-check intercept point
                if(hasXPlanes ) {
                	Hep3Vector trkposXPlaneIter = TrackUtils.getHelixPlanePositionIter(htf, strip.origin(), strip.w(), 1.0e-8);
	                Hep3Vector trkposDiff = VecOp.sub(trkposXPlaneIter, trkpos);
	                if(trkposDiff.magnitude() > 1.0e-7) {
	                	System.out.printf("WARNING trkposDiff mag = %.10f [%.10f %.10f %.10f]\n",trkposDiff.magnitude(),trkposDiff.x(),trkposDiff.y(),trkposDiff.z());
	                	System.exit(1);
	                }	
	                if(_debug>0) System.out.printf("trkposXPlaneIter at intercept [%.10f %.10f %.10f]\n",trkposXPlaneIter.x(),trkposXPlaneIter.y(),trkposXPlaneIter.z());
                }
                
                
                // Find the sim tracker hit for this layer
                SimTrackerHit simHit = simHitsLayerMap.get(strip.layer());

                if( isMC ) {
                	if(simHit==null) {
                		System.out.printf("%s: no sim hit for strip hit at layer %d\n",this.getClass().getSimpleName(),strip.layer());
                		System.out.printf("%s: it as %d mc particles associated with it:\n",this.getClass().getSimpleName(),hit.getMCParticles().size());
                		for (MCParticle particle : hit.getMCParticles())  System.out.printf("%s: %d p %s \n",this.getClass().getSimpleName(),particle.getPDGID(),particle.getMomentum().toString());
                		System.out.printf("%s: these are sim hits in the event:\n",this.getClass().getSimpleName());
                		for (SimTrackerHit simhit : simTrackerHits) System.out.printf("%s sim hit at %s with MC particle pdgid %d with p %s \n",this.getClass().getSimpleName(),simhit.getPositionVec().toString(),simhit.getMCParticle().getPDGID(),simhit.getMCParticle().getMomentum().toString());
                		//System.out.printf("%s: these are all the MC particles in the event:\n",this.getClass().getSimpleName());
                		//System.exit(1);
                	}

                	if(_debug>0) {
                		double s_truthSimHit = HelixUtils.PathToXPlane(htfTruth, simHit.getPositionVec().z(), 0, 0).get(0);
                		Hep3Vector trkposTruthSimHit = HelixUtils.PointOnHelix(htfTruth, s_truthSimHit);
                		Hep3Vector resTruthSimHit = VecOp.sub(CoordinateTransformations.transformVectorToTracking(simHit.getPositionVec()),trkposTruthSimHit);
                		System.out.printf("TruthSimHit residual %s for layer %d\n",resTruthSimHit.toString(),strip.layer());
                	}
                }
                
                //path length to intercept
                double s = HelixUtils.PathToXPlane(htf,trkpos.x(),0,0).get(0); 
                double s3D = s / Math.cos(Math.atan(htf.slope()));
                if(textFile != null) {
                	textFile.printStripPathLen(s);
                	textFile.printStripPathLen3D(s3D);
                }
                
                //GBLDATA
                stripData.setPath(s);
                stripData.setPath3D(s3D);
                
                
                
                //print stereo angle in YZ plane
                if(textFile != null) {
                	textFile.printMeasDir(strip.u());
                	textFile.printNonMeasDir(strip.v());
                	textFile.printNormalDir(strip.w());
                }
                
                //GBLDATA
                stripData.setU(strip.u());
                stripData.setV(strip.v());
                stripData.setW(strip.w());
                
                
                //Print track direction at intercept
                Hep3Vector tDir = HelixUtils.Direction(htf, s);
                double phi = htf.phi0() - s/htf.R();
                double lambda = Math.atan(htf.slope());
                if(textFile != null) {
                	textFile.printStripTrackDir(Math.sin(phi),Math.sin(lambda));
                	textFile.printStripTrackDirFull(tDir);
                }
                
                //GBLDATA
                stripData.setTrackDir(tDir);
                stripData.setTrackPhi(phi);
                stripData.setTrackLambda(lambda);
                
                
                
                
                //Print residual in measurement system
                
                // start by find the distance vector between the center and the track position
                Hep3Vector vdiffTrk = VecOp.sub(trkpos, origin);
                Hep3Vector vdiffTrkTruth = VecOp.sub(trkposTruth, origin);
                
                // then find the rotation from tracking to measurement frame
                Hep3Matrix trkToStripRot = _trackerHitUtils.getTrackToStripRotation(strip.getStrip());
                
                // then rotate that vector into the measurement frame to get the predicted measurement position
                Hep3Vector trkpos_meas = VecOp.mult(trkToStripRot, vdiffTrk);
                Hep3Vector trkposTruth_meas = VecOp.mult(trkToStripRot, vdiffTrkTruth);
                
                
                // hit measurement and uncertainty in measurement frame
                Hep3Vector m_meas = new BasicHep3Vector(strip.umeas(),0.,0.);
                Hep3Vector res_err_meas = new BasicHep3Vector(strip.du(),(strip.vmax() - strip.vmin()) / Math.sqrt(12),10.0/Math.sqrt(12));
                
                if(textFile != null) {
                	textFile.printStripMeas(m_meas.x());
                }
                
                //GBLDATA
                stripData.setMeas(strip.umeas());
                stripData.setTrackPos(trkpos_meas);
                
                if(_debug>1) { 
                System.out.printf("%s: rotation matrix to meas frame\n%s\n", getClass().getSimpleName(), VecOp.toString(trkToStripRot));
                System.out.printf("%s: tPosGlobal %s origin %s\n", getClass().getSimpleName(), trkpos.toString(), origin.toString());
                System.out.printf("%s: tDiff %s\n", getClass().getSimpleName(), vdiffTrk.toString());
                System.out.printf("%s: tPosMeas %s\n", getClass().getSimpleName(), trkpos_meas.toString());
                }
                
                
                
                // residual in measurement frame
                Hep3Vector res_meas = VecOp.sub(m_meas, trkpos_meas);
                Hep3Vector resTruth_meas = VecOp.sub(m_meas, trkposTruth_meas);
                if(textFile != null) {
                	textFile.printStripMeasRes(res_meas.x(),res_err_meas.x());
                	textFile.printStripMeasResTruth(resTruth_meas.x(),res_err_meas.x());
                }
                
                //GBLDATA
                stripData.setMeasErr(res_err_meas.x());
                
                
                
                if(_debug>0) System.out.printf("layer %d millePedeId %d uRes %.10f\n",strip.layer(), millepedeId ,res_meas.x());
                
                // sim hit residual
                
                if(simHit!=null) { 
                    Hep3Vector simHitPos = CoordinateTransformations.transformVectorToTracking(simHit.getPositionVec());
                    if(_debug>0) System.out.printf("simHitPos  %s\n",simHitPos.toString());
                    Hep3Vector vdiffSimHit = VecOp.sub(simHitPos, trkpos);
                    Hep3Vector simHitPos_meas = VecOp.mult(trkToStripRot, vdiffSimHit);
                    if(textFile != null) {
                    	textFile.printStripMeasResSimHit(simHitPos_meas.x(),res_err_meas.x());
                    }
                } else {
                	if(textFile != null) {
                		textFile.printStripMeasResSimHit(-999999.9,-999999.9);
                	}
                }	
                
                // find scattering angle
                ScatterPoint scatter = scatters.getScatterPoint(((RawTrackerHit)strip.getStrip().rawhits().get(0)).getDetectorElement());
                double scatAngle;
                
                if(scatter != null) {
                    scatAngle = scatter.getScatterAngle().Angle();
                }
                else {
                    System.out.printf("%s: WARNING cannot find scatter for detector %s with strip cluster at %s\n",this.getClass(),((RawTrackerHit)strip.getStrip().rawhits().get(0)).getDetectorElement().getName(),strip.origin().toString());
                    //can be edge case where helix is outside, but close to sensor, so use hack with the sensor origin closest to hit -> FIX THIS!
                    DetectorPlane closest = null;
                    double dx = 999999.9;
                    if(MaterialSupervisor.class.isInstance(_scattering.getMaterialManager())) {
                        MaterialSupervisor matSup = (MaterialSupervisor)_scattering.getMaterialManager();
                        for(ScatteringDetectorVolume vol : matSup.getMaterialVolumes()) {
                            DetectorPlane plane = (DetectorPlane)vol;
                            double dx_loop = Math.abs(strip.origin().x() - plane.origin().x());
                            if(dx_loop<dx) {
                                dx = dx_loop;
                                closest = plane;
                            }
                        }
                        if(closest==null) {
                            throw new RuntimeException("cannot find any plane that is close to strip!");
                        } else {
                            // find scatterlength
                            double s_closest = HelixUtils.PathToXPlane(htf,closest.origin().x(), 0., 0).get(0);
                            double X0 = closest.getMaterialTraversedInRL(HelixUtils.Direction(htf, s_closest));
                            ScatterAngle scatterAngle = new ScatterAngle(s_closest, _scattering.msangle(htf.p(this._B.magnitude()),X0));
                            scatAngle = scatterAngle.Angle();
                        }
                    } 
                    else {
                        throw new UnsupportedOperationException("Should not happen. This problem is only solved with the MaterialSupervisor.");
                    }
                } 
                
                
                //print scatterer to file
                if(textFile != null) {
                	textFile.printStripScat(scatAngle);
                }
                
                //GBLDATA
                stripData.setScatterAngle(scatAngle);
                
                ++istrip;
                
                
              
            }
            
        }
        
        
        
    }
    
    
    private void checkAprimeTruth(MCParticle mcp, List<MCParticle> mcParticles) {
    	List<MCParticle> mcp_pair = getAprimeDecayProducts(mcParticles);
        
    	if(_debug>0) {
    		double invMassTruth = Math.sqrt( Math.pow(mcp_pair.get(0).getEnergy()+mcp_pair.get(1).getEnergy(),2) - VecOp.add(mcp_pair.get(0).getMomentum(), mcp_pair.get(1).getMomentum()).magnitudeSquared() );
    		double invMassTruthTrks = getInvMassTracks(TrackUtils.getHTF(mcp_pair.get(0),-1.0*this._B.z()),TrackUtils.getHTF(mcp_pair.get(1),-1.0*this._B.z()));
    		System.out.printf("%s: invM = %f\n",this.getClass().getSimpleName(),invMassTruth);
    		System.out.printf("%s: invMTracks = %f\n",this.getClass().getSimpleName(),invMassTruthTrks);
    	}
    
	    // cross-check
	    if(!mcp_pair.contains(mcp)) {
	        boolean hasBeamElectronParent = false;
	        for(MCParticle parent : mcp.getParents()) {
	            if(parent.getGeneratorStatus()!=MCParticle.FINAL_STATE && parent.getPDGID()==11 && parent.getMomentum().y()==0.0 && Math.abs(parent.getMomentum().magnitude() - _beamEnergy)<0.01) {
	                hasBeamElectronParent = true;
	            }
	        }
	        if(!hasBeamElectronParent) {
	            System.out.printf("%s: the matched MC particle is not an A' daughter and not a the recoil electrons!?\n",this.getClass().getSimpleName());
	            System.out.printf("%s: %s %d p %s org %s\n",this.getClass().getSimpleName(),mcp.getGeneratorStatus()==MCParticle.FINAL_STATE?"F":"I",mcp.getPDGID(),mcp.getMomentum().toString(),mcp.getOrigin().toString());
	            printMCParticles(mcParticles);
	            System.exit(1);
	        } else {
	            if(_debug>0) System.out.printf("%s: the matched MC particle is the recoil electron\n",this.getClass().getSimpleName());
	        }
	    }
		
	}
    

    MCParticle getMatchedTruthParticle(Track track) {
        boolean debug = false;
        
        Map<MCParticle,Integer> particlesOnTrack = new HashMap<MCParticle,Integer>();
        
        if(debug) System.out.printf("getmatched mc particle from %d tracker hits on the track \n",track.getTrackerHits().size());
        
        
        for(TrackerHit hit : track.getTrackerHits()) {
            List<MCParticle> mcps = ((HelicalTrackHit)hit).getMCParticles();
            if(mcps==null) {
                System.out.printf("%s: warning, this hit (layer %d pos=%s) has no mc particles.\n",this.getClass().getSimpleName(),((HelicalTrackHit)hit).Layer(),((HelicalTrackHit)hit).getCorrectedPosition().toString());
            } 
            else {
            	if( debug ) System.out.printf("%s: this hit (layer %d pos=%s) has %d mc particles.\n",this.getClass().getSimpleName(),((HelicalTrackHit)hit).Layer(),((HelicalTrackHit)hit).getCorrectedPosition().toString(),mcps.size());
                for(MCParticle mcp : mcps) {
                    if( !particlesOnTrack.containsKey(mcp) ) {
                        particlesOnTrack.put(mcp, 0);
                    }
                    int c = particlesOnTrack.get(mcp);
                    particlesOnTrack.put(mcp, c+1);
                }
            }
        }
        if(debug) {
            System.out.printf("Track p=[ %f, %f, %f] \n",track.getTrackStates().get(0).getMomentum()[0],track.getTrackStates().get(0).getMomentum()[1],track.getTrackStates().get(0).getMomentum()[1]);
            System.out.printf("FOund %d particles\n",particlesOnTrack.size());
            for(Map.Entry<MCParticle, Integer> entry : particlesOnTrack.entrySet()) {
                System.out.printf("%d hits assigned to %d p=%s \n",entry.getValue(),entry.getKey().getPDGID(),entry.getKey().getMomentum().toString());
            }
        }
        Map.Entry<MCParticle,Integer> maxEntry = null;
        for(Map.Entry<MCParticle,Integer> entry : particlesOnTrack.entrySet()) {
            if ( maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0 ) maxEntry = entry;
            //if ( maxEntry != null ) {
            //    if(entry.getValue().compareTo(maxEntry.getValue()) < 0) continue;
            //}
            //maxEntry = entry;
        }
        if(debug) {
        	if (maxEntry != null ) {
        		System.out.printf("Matched particle with pdgId=%d and mom %s to track with charge %d and momentum [%f %f %f]\n",
        				maxEntry.getKey().getPDGID(),maxEntry.getKey().getMomentum().toString(),
        				track.getCharge(),track.getTrackStates().get(0).getMomentum()[0],track.getTrackStates().get(0).getMomentum()[1],track.getTrackStates().get(0).getMomentum()[2]);
        	} else {
        		System.out.printf("No truth particle found on this track\n");
        	}
        }
        return maxEntry == null ? null : maxEntry.getKey();
    }
    
    
    private BasicMatrix getPerParVector(HelicalTrackFit htf) {
        BasicMatrix perPar = new BasicMatrix(1,5);
        if( htf != null) {
        	double kappa = -1.0*Math.signum(htf.R())*Constants.fieldConversion*this._B.z()/htf.pT(Math.abs(_B.z()));
        	double theta = Math.PI/2.0 - Math.atan(htf.slope());
        	perPar.setElement(0,0,kappa);
        	perPar.setElement(0,1,theta);
        	perPar.setElement(0,2,htf.phi0());
        	perPar.setElement(0,3,htf.dca());
        	perPar.setElement(0,4,htf.z0());
        }
        return perPar;
        
    }


    
    
    private BasicMatrix getJacPerToCl(HelicalTrackFit htf) {
        System.out.printf("%s: getJacPerToCl\n",this.getClass().getSimpleName());        
        //use propoerly normalized B-field
        Hep3Vector Bnorm = VecOp.mult(Constants.fieldConversion, _B);
        //init jacobian to zero
        BasicMatrix j = new BasicMatrix(5,5);
        initZero(j);
        double lambda = Math.atan(htf.slope());
        double q = Math.signum(htf.R());
        double theta = Math.PI/2.0 - lambda;
        Hep3Vector T = HelixUtils.Direction(htf, 0.);
        Hep3Vector p = VecOp.mult(htf.p(Math.abs(_B.z())), T);
        double pT = htf.pT(Math.abs(_B.z()));
        Hep3Vector H = VecOp.mult(1./(Bnorm.magnitude()), Bnorm); 
        Hep3Vector Z = new BasicHep3Vector(0,0,1);
        Hep3Vector J = VecOp.mult(1./VecOp.cross(T,Z).magnitude(), VecOp.cross(T, Z));
        Hep3Vector U = VecOp.mult(-1, J);
        Hep3Vector V = VecOp.cross(T, U);
        double alpha = VecOp.cross(H,T).magnitude();
        Hep3Vector N = VecOp.mult(1./alpha,VecOp.cross(H, T));
        Hep3Vector K = Z;
        double Q = -Bnorm.magnitude()*q/p.magnitude();
        double kappa = -1.0*q*Bnorm.z()/pT;
        
        if (this._debug!=0) {
            System.out.printf("%s: Bnorm=%s mag(Bnorm)=%f\n",this.getClass().getSimpleName(),Bnorm.toString(),Bnorm.magnitude());
            System.out.printf("%s: p=%s |p|=%f pT=%f\n",this.getClass().getSimpleName(),p.toString(),p.magnitude(),pT);
            System.out.printf("%s: q=%f\n",this.getClass().getSimpleName(),q);
            System.out.printf("%s: q/p=%f\n",this.getClass().getSimpleName(),q/p.magnitude());
            System.out.printf("%s: T=%s\n",this.getClass().getSimpleName(),T.toString());
            System.out.printf("%s: H=%s\n",this.getClass().getSimpleName(),H.toString());
            System.out.printf("%s: kappa=%f\n",this.getClass().getSimpleName(),kappa);
            System.out.printf("%s: alpha=%f Q=%f \n",this.getClass().getSimpleName(),alpha,Q);
            System.out.printf("%s: J=%s \n",this.getClass().getSimpleName(),J.toString());
            System.out.printf("%s: V=%s \n",this.getClass().getSimpleName(),V.toString());
            System.out.printf("%s: N=%s \n",this.getClass().getSimpleName(),N.toString());
            System.out.printf("%s: TdotJ=%f \n",this.getClass().getSimpleName(),VecOp.dot(T, J));
            System.out.printf("%s: VdotN=%f \n",this.getClass().getSimpleName(),VecOp.dot(V, N));
            System.out.printf("%s: TdotK=%f \n",this.getClass().getSimpleName(),VecOp.dot(T, K));
            System.out.printf("%s: UdotN=%f \n",this.getClass().getSimpleName(),VecOp.dot(U, N));
        }
        

        
        
        
        j.setElement(0,0,-1.0*Math.sin(theta)/Bnorm.z());
        
        j.setElement(0,1,q/(p.magnitude()*Math.tan(theta)));
        
        j.setElement(1,1,-1);
        
        j.setElement(1,3,-alpha*Q*VecOp.dot(T, J)*VecOp.dot(V, N));
                
        j.setElement(1,4,-alpha*Q*VecOp.dot(T, K)*VecOp.dot(V, N));

        j.setElement(2,2,1);
        
        j.setElement(2,3,-alpha*Q*VecOp.dot(T,J)*VecOp.dot(U, N)/Math.cos(lambda));

        j.setElement(2,4,-alpha*Q*VecOp.dot(T,K)*VecOp.dot(U, N)/Math.cos(lambda));

        j.setElement(3,3,-1);
        
        j.setElement(4,4,VecOp.dot(V, K));

        
        if(_debug>0) {
                System.out.printf("%s: lambda= J(1,1)=%f  * theta + J(1,3)=%f * eps + J(1,4)=%f * z0 \n",
                this.getClass().getSimpleName(),
                j.e(1, 1),j.e(1,3),j.e(1,4));

        }
        
        
        return j;
        
    }
    
    
   

    
    
    private void initUnit(BasicMatrix mat) {
        for(int row=0;row!=mat.getNRows();row++) {
            for(int col=0;col!=mat.getNColumns();col++) {
                if(row!=col) mat.setElement(row, col, 0);
                else mat.setElement(row, col, 1);
            }
        }
    }

    private void initZero(BasicMatrix mat) {
        for(int row=0;row!=mat.getNRows();row++) {
            for(int col=0;col!=mat.getNColumns();col++) {
                mat.setElement(row, col, 0);
            }
        }
    }


    
    /**
     * Transform MCParticle into a Helix object.
     * Note that it produces the helix parameters at nominal x=0 and assumes that there is no field at x<0
     * 
     * @param mcp MC particle to be transformed
     * @return helix object based on the MC particle
     */
//    private HelicalTrackFit getHTF(MCParticle mcp) {
//        Hep3Vector org = this._hpstrans.transformVectorToTracking(mcp.getOrigin());
//        Hep3Vector p = this._hpstrans.transformVectorToTracking(mcp.getMomentum());
//        // Move to x=0 if needed
//        if(org.x() < 0.) { 
//        	double dydx = p.y()/p.x();
//        	double dzdx = p.z()/p.x();
//        	double delta_x = -1. * org.x(); 
//        	double y = delta_x * dydx;
//        	double z = delta_x * dzdx;
//        	double x = org.x() + delta_x;
//        	if( Math.abs(x) > 1e-8) throw new RuntimeException("Error: origin is not zero!");
//        	Hep3Vector old = org;
//        	org = new BasicHep3Vector(x,y,z);
//        	System.out.printf("org %s p %s -> org %s\n", old.toString(),p.toString(),org.toString());
//        } else {
//        	org = this._hpstrans.transformVectorToTracking(mcp.getOrigin());
//        }
//        
//        
//        
//        HelixParamCalculator helixParamCalculator = new HelixParamCalculator(p, org, -1*((int)mcp.getCharge()), -1.0*this._B.z());
//        double par[] = new double[5];
//        par[HelicalTrackFit.dcaIndex] = helixParamCalculator.getDCA();
//        par[HelicalTrackFit.slopeIndex] = helixParamCalculator.getSlopeSZPlane();
//        par[HelicalTrackFit.phi0Index] = helixParamCalculator.getPhi0();
//        par[HelicalTrackFit.curvatureIndex] = 1.0/helixParamCalculator.getRadius();
//        par[HelicalTrackFit.z0Index] = helixParamCalculator.getZ0();
//        SymmetricMatrix cov = new SymmetricMatrix(5);
//        for(int i=0;i<cov.getNRows();++i) cov.setElement(i, i, 1.);
//        HelicalTrackFit htf = new HelicalTrackFit(par, cov, new double[2], new int[2], null, null);
//        return htf;
//    }

    private double truthTrackFitChi2(PerigeeParams perPar, PerigeeParams perParTruth, SymmetricMatrix covariance) {
        //re-shuffle the param vector to match the covariance order of parameters
        BasicMatrix p = new BasicMatrix(1,5);
        p.setElement(0, 0, perPar.getD0());
        p.setElement(0, 1, perPar.getPhi());
        p.setElement(0, 2, perPar.getKappa());
        p.setElement(0, 0, perPar.getZ0());
        p.setElement(0, 4, Math.tan(Math.PI/2.0-perPar.getTheta()));
        BasicMatrix pt = new BasicMatrix(1,5);
        pt.setElement(0, 0, perParTruth.getD0());
        pt.setElement(0, 1, perParTruth.getPhi());
        pt.setElement(0, 2, perParTruth.getKappa());
        pt.setElement(0, 0, perParTruth.getZ0());
        pt.setElement(0, 4, Math.tan(Math.PI/2.0-perParTruth.getTheta()));
        Matrix error_matrix = MatrixOp.inverse(covariance);
        BasicMatrix res = (BasicMatrix) MatrixOp.sub(p, pt);
        BasicMatrix chi2 = (BasicMatrix) MatrixOp.mult(res,MatrixOp.mult(error_matrix, MatrixOp.transposed(res)));
        if(chi2.getNColumns()!=1 ||chi2.getNRows()!=1) {
            throw new RuntimeException("matrix dim is screwed up!");
        }
        return chi2.e(0, 0);
    }

    
    
    private List<MCParticle> getAprimeDecayProducts(List<MCParticle> mcParticles) {
        List<MCParticle> pair = new ArrayList<MCParticle>();
        for(MCParticle mcp : mcParticles) {
            if(mcp.getGeneratorStatus()!=MCParticle.FINAL_STATE) continue;
            boolean hasAprimeParent = false;
            for(MCParticle parent : mcp.getParents()) {
                if(Math.abs(parent.getPDGID())==622) hasAprimeParent = true;
            }
            if(hasAprimeParent)  pair.add(mcp);
        }
        if(pair.size()!=2) {
            System.out.printf("%s: ERROR this event has %d mcp with 622 as parent!!??  \n",this.getClass().getSimpleName(),pair.size());
            this.printMCParticles(mcParticles);
            System.exit(1);
        }
        if( Math.abs(pair.get(0).getPDGID()) != 11 || Math.abs(pair.get(1).getPDGID()) != 11 ) {
            System.out.printf("%s: ERROR decay products are not e+e-? \n",this.getClass().getSimpleName());
            this.printMCParticles(mcParticles);
            System.exit(1);
        }
        if(pair.get(0).getPDGID()*pair.get(1).getPDGID() > 0) {
            System.out.printf("%s: ERROR decay products have the same sign? \n",this.getClass().getSimpleName());
            this.printMCParticles(mcParticles);
            System.exit(1);
        }
        return pair;
        
    }

    private void printMCParticles(List<MCParticle> mcParticles) {      
        System.out.printf("%s: printMCParticles \n",this.getClass().getSimpleName());
        System.out.printf("%s: %d mc particles \n",this.getClass().getSimpleName(),mcParticles.size());
        for(MCParticle mcp : mcParticles) {
            if(mcp.getGeneratorStatus()!=MCParticle.FINAL_STATE) continue;
            System.out.printf("\n%s: (%s) %d  p %s org %s  %s \n",this.getClass().getSimpleName(),
                                mcp.getGeneratorStatus()==MCParticle.FINAL_STATE?"F":"I",mcp.getPDGID(),mcp.getMomentum().toString(),mcp.getOrigin().toString(),
                                mcp.getParents().size()>0?"parents:":"");
            for(MCParticle parent : mcp.getParents()) {
                System.out.printf("%s:       (%s) %d  p %s org %s %s \n",this.getClass().getSimpleName(),
                                parent.getGeneratorStatus()==MCParticle.FINAL_STATE?"F":"I",parent.getPDGID(),parent.getMomentum().toString(),parent.getOrigin().toString(),
                                parent.getParents().size()>0?"parents:":"");
                for(MCParticle grparent : parent.getParents()) {
                    System.out.printf("%s:            (%s) %d  p %s org %s  %s \n",this.getClass().getSimpleName(),
                                    grparent.getGeneratorStatus()==MCParticle.FINAL_STATE?"F":"I",grparent.getPDGID(),grparent.getMomentum().toString(),grparent.getOrigin().toString(),
                                    grparent.getParents().size()>0?"parents:":"");
                }
            
            }
        }
        return;
     }

    private double getInvMassTracks(HelicalTrackFit htf1, HelicalTrackFit htf2) {
        double p1 = htf1.p(this._B.magnitude());
        double p2 = htf2.p(this._B.magnitude());
        Hep3Vector p1vec = VecOp.mult(p1, HelixUtils.Direction(htf1, 0));
        Hep3Vector p2vec = VecOp.mult(p2, HelixUtils.Direction(htf2, 0));
        double me = 0.000510998910;
        double E1 = Math.sqrt(p1*p1 + me*me);
        double E2 = Math.sqrt(p2*p2 + me*me);
        //System.out.printf("p1 %f %s E1 %f\n",p1,p1vec.toString(),E1);
        //System.out.printf("p2 %f %s E2 %f\n",p2,p2vec.toString(),E2);
        return Math.sqrt( Math.pow(E1+E2,2) - VecOp.add(p1vec, p2vec).magnitudeSquared() );
    }

    
    public class PerigeeParams {
        private BasicMatrix _params;

        private PerigeeParams(HelicalTrackFit htf) {
            _params = GBLOutput.this.getPerParVector(htf);
        }
        public BasicMatrix getParams() {
            return _params;
        }
        public double getKappa() {
            return _params.e(0, 0);
        }
        public double getTheta() {
            return _params.e(0, 1);
        }
        public double getPhi() {
            return _params.e(0, 2);
        }
        public double getD0() {
            return _params.e(0, 3);
        }
        public double getZ0() {
            return _params.e(0, 4);
        }
    }

    /**
     * Computes the projection matrix from the perigee XY plane variables 
     * dca and z0 into the curvilinear xT,yT,zT frame (U,V,T)
     * @param htf input helix to find the track direction
     * @return 3x3 projection matrix
     */
    Hep3Matrix getPerToClPrj(HelicalTrackFit htf) {
        Hep3Vector Z = new BasicHep3Vector(0,0,1);
        Hep3Vector T = HelixUtils.Direction(htf, 0.);
        Hep3Vector J = VecOp.mult(1./VecOp.cross(T,Z).magnitude(), VecOp.cross(T, Z));
        Hep3Vector K = Z;
        Hep3Vector U = VecOp.mult(-1, J);
        Hep3Vector V = VecOp.cross(T, U);
        Hep3Vector I = VecOp.cross(J, K);
           
        BasicHep3Matrix trans = new BasicHep3Matrix();
        trans.setElement(0, 0, VecOp.dot(I, U));
        trans.setElement(0, 1, VecOp.dot(J, U));
        trans.setElement(0, 2, VecOp.dot(K, U));
        trans.setElement(1, 0, VecOp.dot(I, V));
        trans.setElement(1, 1, VecOp.dot(J, V));
        trans.setElement(1, 2, VecOp.dot(K, V));
        trans.setElement(2, 0, VecOp.dot(I, T));
        trans.setElement(2, 1, VecOp.dot(J, T));
        trans.setElement(2, 2, VecOp.dot(K, T));
        return trans;
        
    }
    

    

	public class ClParams {
        private BasicMatrix _params = new BasicMatrix(1,5);
        private ClParams(HelicalTrackFit htf) {
            
        	if (htf == null) return;

            Hep3Matrix perToClPrj = GBLOutput.this.getPerToClPrj(htf);
            
            double d0 = -1 * htf.dca(); //sign convention for curvilinear frame
            double z0 = htf.z0();
            Hep3Vector vecPer = new BasicHep3Vector(0.,d0,z0);
            //System.out.printf("%s: vecPer=%s\n",this.getClass().getSimpleName(),vecPer.toString());
            
            Hep3Vector vecCl = VecOp.mult(perToClPrj, vecPer);
            //System.out.printf("%s: vecCl=%s\n",this.getClass().getSimpleName(),vecCl.toString());
            double xT = vecCl.x();
            double yT = vecCl.y();
            //double zT = vecCl.z();
            
            Hep3Vector T = HelixUtils.Direction(htf, 0.);
            Hep3Vector p = VecOp.mult(htf.p(Math.abs(_B.z())), T);
            double lambda = Math.atan(htf.slope());
            double q = Math.signum(htf.R());
            double qOverP = q/p.magnitude();
            double phi = htf.phi0();
            
            _params.setElement(0, 0, qOverP);
            _params.setElement(0, 1, lambda);
            _params.setElement(0, 2, phi);
            _params.setElement(0, 3, xT);
            _params.setElement(0, 4, yT);
            
        }

        public BasicMatrix getParams() {
            return _params;
        }
        
        double getLambda() {
            return _params.e(0,1);
        }
        double getQoverP() {
            return _params.e(0,0);
        }
        double getPhi() {
            return _params.e(0,2);
        }
        double getXt() {
            return _params.e(0,3);
        }
        double getYt() {
            return _params.e(0,4);
        }
        
    }
	

}
