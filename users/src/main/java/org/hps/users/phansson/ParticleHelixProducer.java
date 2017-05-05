package org.hps.users.phansson;

import hep.aida.ICloud1D;
import hep.aida.IPlotter;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hps.analysis.ecal.HPSMCParticlePlotsDriver;
import org.hps.recon.tracking.HpsHelicalTrackFit;
import org.hps.recon.tracking.TrackerHitUtils;
import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.ParticleTypeClassifier;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.MultipleScatter;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType.CoordinateSystem;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class ParticleHelixProducer extends Driver {

    private boolean debug = false;
    private boolean hideFrame = true;
    private boolean saveSingleTrack = false;
    private boolean noTopTracks = true;
    private boolean noBottomTracks = false;
    private int _totalTracks = 0;
    TrackerHitUtils trackerhitutils = new TrackerHitUtils();
    Hep3Matrix detToTrk;
    Hep3Vector _bfield;
    TrackerHitType trackerType = new TrackerHitType(TrackerHitType.CoordinateSystem.GLOBAL, TrackerHitType.MeasurementType.STRIP_1D);
    CoordinateSystem coordinate_system = trackerType.getCoordinateSystem();
    // Name of StripHit1D output collection.
    private String trackOutputCollectionName = "MCParticle_HelicalTrackFit";
    private AIDA aida = AIDA.defaultInstance();
    IPlotter plotter_trkparams;
    ICloud1D h_pt;
    ICloud1D h_phi0;
    ICloud1D h_d0;
    ICloud1D h_R;
    ICloud1D h_slope;
    ICloud1D h_z0;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setHideFrame(boolean hide) {
        this.hideFrame = hide;
    }

    public void setNoTopTracks(boolean doTop) {
        this.noTopTracks = doTop;
    }

    public void setNoBottomTracks(boolean doBot) {
        this.noBottomTracks = doBot;
    }

    public void setSaveSingleTrack(boolean single) {
        this.saveSingleTrack = single;
    }

    public void setTrackOutputCollectionName(String trackOutputCollectionName) {
        this.trackOutputCollectionName = trackOutputCollectionName;
    }

    /**
     * Creates a new instance of TrackerHitDriver.
     */
    public ParticleHelixProducer() {
    }

    /**
     * Do initialization once we get a Detector.
     */
    @Override
    public void detectorChanged(Detector detector) {

        // Call sub-Driver's detectorChanged methods.
        super.detectorChanged(detector);

        if (this.noTopTracks && this.noBottomTracks) {
            System.out.println(this.getClass().getSimpleName() + ": ERROR you need to produce top or bottom tracks!");
            System.exit(1);
        }

        Hep3Vector IP = new BasicHep3Vector(0., 0., 1.);
        _bfield = new BasicHep3Vector(0, 0, detector.getFieldMap().getField(IP).y());
        detToTrk = trackerhitutils.detToTrackRotationMatrix();


        plotter_trkparams = aida.analysisFactory().createPlotterFactory().create();
        plotter_trkparams.createRegions(3, 2);
        h_pt = aida.cloud1D("Track pT");
        h_R = aida.cloud1D("Track R");
        h_phi0 = aida.cloud1D("Track phi0");
        h_d0 = aida.cloud1D("Track d0");
        h_slope = aida.cloud1D("Track slope");
        h_z0 = aida.cloud1D("Track z0");
        plotter_trkparams.region(0).plot(h_pt);
        plotter_trkparams.region(1).plot(h_R);
        plotter_trkparams.region(2).plot(h_phi0);
        plotter_trkparams.region(3).plot(h_d0);
        plotter_trkparams.region(4).plot(h_slope);
        plotter_trkparams.region(5).plot(h_z0);
        if (!this.hideFrame) {
            plotter_trkparams.show();
        }


    }

    /**
     * Perform the digitization.
     */
    @Override
    public void process(EventHeader event) {


        //Make new tracks based on the MC particles
        //List<HelicalTrackFit> tracks = new ArrayList<HelicalTrackFit>();
        List<HpsHelicalTrackFit> tracks = new ArrayList<HpsHelicalTrackFit>();

        if (event.hasCollection(MCParticle.class)) {
            List<MCParticle> mcparticles = event.get(MCParticle.class).get(0);
            List<MCParticle> fsParticles = HPSMCParticlePlotsDriver.makeGenFSParticleList(mcparticles);

            //-----> DEBUG
            if (debug) {
                String particleList = "[ ";
                for (MCParticle mcParticle : mcparticles) {
                    particleList += mcParticle.getPDGID() + ", ";
                }
                particleList += "]";
                this.printDebug("MC Particles: " + particleList);
            }
            if (debug) {
                String particleList = "[ ";
                for (MCParticle fsParticle : fsParticles) {
                    particleList += fsParticle.getPDGID() + ", ";
                }
                particleList += "]";
                this.printDebug("Final State MC Particles: " + particleList);
            }
            //------> DEBUG

            double bfield = Math.abs(_bfield.z()); //remove sign from B-field, assumed to be along z-direction
            if (debug) {
                System.out.println(this.getClass().getSimpleName() + ": bfield " + bfield);
            }

            for (MCParticle part : fsParticles) {
                if (Math.random() > 0.5) {
                    this.printDebug("Random value below threshold. Skipping the final state MC particle " + part.getPDGID());
                    continue;
                }
                if (ParticleTypeClassifier.isElectron(part.getPDGID()) || ParticleTypeClassifier.isPositron(part.getPDGID())) {


                    Hep3Vector p = part.getMomentum();
                    Hep3Vector org = part.getOrigin();
                    double q = -1 * part.getCharge(); //since I flipped the B-field I need to flip the charge


                    if (p.magnitude() < 0.3) {
                        if (debug) {
                            System.out.println(this.getClass().getSimpleName() + ": this MC particle had too small momentum p=" + p.toString());
                        }
                        continue;
                    }

                    double thetay = Math.atan(p.y() / p.z());

                    if (Math.abs(thetay) < 0.01) {
                        if (debug) {
                            System.out.println(this.getClass().getSimpleName() + ": this MC particle had too small thetay =" + Math.abs(Math.atan(p.y() / p.z())));
                        }
                        continue;
                    }

                    if (this.noTopTracks && thetay > 0.0) {
                        if (debug) {
                            System.out.println(this.getClass().getSimpleName() + ": this MC particle had negative thetay (" + thetay + ") and we only want top tracks");
                        }
                        continue;
                    }
                    if (this.noBottomTracks && thetay < 0.0) {
                        if (debug) {
                            System.out.println(this.getClass().getSimpleName() + ": this MC particle had positive thetay (" + thetay + ") and we only want bottom tracks");
                        }
                        continue;
                    }


                    //propagate to start of field region if needed
                    double dz = 0 - org.z();
                    if (dz > 0) {
                        System.out.print(this.getClass().getSimpleName() + ": Propagate MC particle to field region from org=" + org.toString());
                        double tanPxPz = p.x() / p.z();
                        double tanPyPz = p.y() / p.z();
                        double dx = dz * tanPxPz;
                        double dy = dz * tanPyPz;

                        org = new BasicHep3Vector(org.x() + dx, org.y() + dy, org.z() + dz);

                        System.out.println(" to   org=" + org.toString() + " (p=" + p.toString() + ")");

                    }

                    //if(debug) System.out.println(this.getClass().getSimpleName() + ": MC particle p=" + p.toString() +" org=" + org.toString() +" q = " + q);
                    p = VecOp.mult(detToTrk, p);
                    org = VecOp.mult(detToTrk, org);
                    if (debug) {
                        System.out.println(this.getClass().getSimpleName() + ": MC particle p=" + p.toString() + " org=" + org.toString() + " q = " + q + " (rotated)");
                    }

                    if (debug) {
                        double pt = Math.sqrt(p.x() * p.x() + p.y() * p.y());
                        double Rman = q * pt / (Constants.fieldConversion * bfield);
                        double phi = Math.atan2(p.y(), p.x());
                        double xc = org.x() + Rman * Math.sin(phi);
                        double yc = org.y() - Rman * Math.cos(phi);
                        double Rc = Math.sqrt(xc * xc + yc * yc);
                        double dca = q > 0 ? (Rman - Rc) : (Rman + Rc);
                        System.out.println(this.getClass().getSimpleName() + ": manual calcualtion gives pt " + pt + " R " + Rman + " phi " + phi + " xc " + xc + " yc " + yc + " Rc " + Rc + " DCA " + dca);
                    }
                    //HelixParamCalculator hpc = new HelixParamCalculator(part,bfield);
                    HelixParamCalculator hpc = new HelixParamCalculator(p, org, (int) q, bfield); //remove sign from b-field
                    double[] pars = new double[5];
                    pars[0] = hpc.getDCA();
                    pars[1] = hpc.getPhi0();
                    pars[2] = 1 / hpc.getRadius();
                    pars[3] = hpc.getZ0();
                    pars[4] = hpc.getSlopeSZPlane();
                    //HelicalTrackFit htf = this.trackUtils.makeHelicalTrackFit(pars);
                    HpsHelicalTrackFit htf = this.makeHPSTrack(pars, part);
                    tracks.add(htf);
                    if (debug) {
                        System.out.println(this.getClass().getSimpleName() + ": MC particle created HelicalTrackFit " + htf.toString());

                    }

                    h_pt.fill(htf.pT(bfield));
                    h_R.fill(htf.R());
                    h_d0.fill(htf.dca());
                    h_phi0.fill(htf.phi0());
                    h_slope.fill(htf.slope());
                    h_z0.fill(htf.z0());


                    if (saveSingleTrack && tracks.size() == 1) {
                        break;
                    }

                }
            }
        }

        // If there were not HpsHelicalTrackFits created, skip the event
        if (tracks.isEmpty()) {
            this.printDebug("No tracks were created. Skipping the event ...");
            return;
        }

        this.printDebug("created " + tracks.size() + " MC particle helix tracks");
        event.put(this.trackOutputCollectionName, tracks, HpsHelicalTrackFit.class, 0);
        _totalTracks += tracks.size();
    }

    /**
     * Create an HpsHelicalTrackFit
     *
     * @param helixParameters : an array of helix parameters
     * @param mcParticle : MC particle associated to this HelicalTrackFit
     * @return HpsHelicalTrackFit :
     */
    public HpsHelicalTrackFit makeHPSTrack(double[] helixParameters, MCParticle mcParticle) {
        HpsHelicalTrackFit helicalTrackFit = new HpsHelicalTrackFit(helixParameters, new SymmetricMatrix(5), new double[2], new int[2],
                new HashMap<HelicalTrackHit, Double>(), new HashMap<HelicalTrackHit, MultipleScatter>());
        helicalTrackFit.setMcParticle(mcParticle);
        return  helicalTrackFit;
        
    }

    /**
     * print debug statement
     *
     * @param debugStatement : debug statement
     */
    public void printDebug(String debugStatement) {
        if (!debug) {
            return;
        }
        System.out.println(this.getClass().getSimpleName() + ": " + debugStatement);
    }

    @Override
    public void endOfData() {

        System.out.println(this.getClass().getSimpleName() + ": produced " + _totalTracks);
    }
}
