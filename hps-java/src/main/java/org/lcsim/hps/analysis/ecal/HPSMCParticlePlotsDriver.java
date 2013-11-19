package org.lcsim.hps.analysis.ecal;

import hep.aida.IAnalysisFactory;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.ref.plotter.PlotterRegion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.ParticleTypeClassifier;
import org.lcsim.hps.monitoring.deprecated.AIDAFrame;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Diagnostic plots for HPS ECal.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: HPSMCParticlePlotsDriver.java,v 1.8 2013/10/25 20:11:50 jeremy Exp $
 */
public class HPSMCParticlePlotsDriver extends Driver {

	AIDA aida = AIDA.defaultInstance();
        private AIDAFrame pFrame;
        IAnalysisFactory af = aida.analysisFactory();
        public boolean _hideFrame = false;
        // MCParticle plots.
	ICloud1D primaryEPlot;
	ICloud1D fsCountPlot;
	IHistogram1D fsCountVsEventPlot;
	ICloud1D fsCountTypePlot;
	ICloud1D fsCountEventTypePlot;
	ICloud1D fsCountEventTypePlot2;
	ICloud1D fsCountTypePlot500;
	IHistogram1D fsEPlot;
	IHistogram1D fsGammaEPlot;
	IHistogram1D fsElectronEPlot;
	IHistogram1D fsPositronEPlot;
	IHistogram1D fsThetayPlot;
	ICloud1D fsGammaThetaPlot;
	IHistogram1D fsGammaThetayPlot;
	IHistogram1D fsGammaThetayTrigPlot;
	ICloud2D fsGammaThetayEPlot;
	ICloud1D fsElectronThetaPlot;
	IHistogram1D fsElectronThetayPlot;
	IHistogram1D fsElectronThetayTrigPlot;
	ICloud2D fsElectronThetayEPlot;
	ICloud1D fsPositronThetaPlot;
	IHistogram1D fsPositronThetayPlot;
	IHistogram1D fsPositronThetayTrigPlot;
	ICloud2D fsPositronThetayEPlot;
        ICloud1D eventEPlot;

	class MCParticleEComparator implements Comparator<MCParticle> {

		public int compare(MCParticle p1, MCParticle p2) {
			double e1 = p1.getEnergy();
			double e2 = p2.getEnergy();
			if (e1 < e2) {
				return -1;
			} else if (e1 == e2) {
				return 0;
			} else {
				return 1;
			}
		}
	}

        public void setHideFrame(boolean hideFrame) {
            this._hideFrame = hideFrame;
        }
        
        
	@Override
	public void startOfData() {
		fsCountPlot = aida.cloud1D("MCParticle: Number of Final State Particles");
		fsCountPlot.annotation().addItem("xAxisLabel", "Number of FS Particles");
                
                fsCountVsEventPlot = aida.histogram1D("MCParticle: Number of Final State Particles vs Event Nr", 501, -0.5, 500.5);
		fsCountVsEventPlot.annotation().addItem("xAxisLabel", "Event Number");

                fsCountTypePlot = aida.cloud1D("MCParticle: Number of Final State Particles Type");
		fsCountTypePlot.annotation().addItem("xAxisLabel", "Number of FS Particles of Type");

                fsCountTypePlot500 = aida.cloud1D("MCParticle: Number of Final State Particles Type E>0.5GeV");
		fsCountTypePlot500.annotation().addItem("xAxisLabel", "Number of FS Particles of Type E>0.5GeV");

                fsCountEventTypePlot = aida.cloud1D("MCParticle: Number of Final State Types");
		fsCountEventTypePlot.annotation().addItem("xAxisLabel", "Number of FS Types");

                fsCountEventTypePlot2 = aida.cloud1D("MCParticle: Number of Final State Types Gamma E>500");
		fsCountEventTypePlot2.annotation().addItem("xAxisLabel", "Number of FS Types Gamma E>500");                
                
		fsEPlot = aida.histogram1D("MCParticle: FS Particle E",100,0,3);
		fsEPlot.annotation().addItem("xAxisLabel", "Particle E [GeV]");

		fsGammaEPlot = aida.histogram1D("MCParticle: FS Gamma E",100,0,3);
		fsGammaEPlot.annotation().addItem("xAxisLabel", "Particle E [GeV]");

		fsElectronEPlot = aida.histogram1D("MCParticle: FS Electron E",100,0,3);
		fsElectronEPlot.annotation().addItem("xAxisLabel", "Particle E [GeV]");

		fsPositronEPlot = aida.histogram1D("MCParticle: FS Positron E",100,0,3);
		fsPositronEPlot.annotation().addItem("xAxisLabel", "Particle E [GeV]");

		fsGammaThetaPlot = aida.cloud1D("MCParticle: FS Gamma Theta");
		fsGammaThetaPlot.annotation().addItem("xAxisLabel", "Particle angle [rad]");

                fsThetayPlot = aida.histogram1D("MCParticle: FS Particle Thetay",100,0,0.1);
		fsThetayPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");

                
                fsGammaThetayPlot = aida.histogram1D("MCParticle: FS Gamma Thetay",100,0,0.1);
		fsGammaThetayPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");

                fsGammaThetayTrigPlot = aida.histogram1D("MCParticle: FS Gamma Thetay Trig",100,0,0.1);
		fsGammaThetayTrigPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");
             
                fsGammaThetayEPlot = aida.cloud2D("MCParticle: FS Gamma Thetay vs E");
		fsGammaThetayEPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");
		fsGammaThetayEPlot.annotation().addItem("yAxisLabel", "Particle Energy [GeV]");
                
		fsElectronThetaPlot = aida.cloud1D("MCParticle: FS Electron Theta");
		fsElectronThetaPlot.annotation().addItem("xAxisLabel", "Particle angle [rad]");

                fsElectronThetayPlot = aida.histogram1D("MCParticle: FS Electron Thetay",100,0,0.1);
		fsElectronThetayPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");

                fsElectronThetayTrigPlot = aida.histogram1D("MCParticle: FS Electron Thetay Trig",100,0,0.1);
		fsElectronThetayTrigPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");

                fsElectronThetayEPlot = aida.cloud2D("MCParticle: FS Electron Thetay vs E");
		fsElectronThetayEPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");
		fsElectronThetayEPlot.annotation().addItem("yAxisLabel", "Particle Energy [GeV]");

		fsPositronThetaPlot = aida.cloud1D("MCParticle: FS Positron Theta");
		fsPositronThetaPlot.annotation().addItem("xAxisLabel", "Particle angle [rad]");

                fsPositronThetayPlot = aida.histogram1D("MCParticle: FS Positron Thetay",100,0,0.1);
		fsPositronThetayPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");

                fsPositronThetayTrigPlot = aida.histogram1D("MCParticle: FS Positron Thetay Trig",100,0,0.1);
		fsPositronThetayTrigPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");

                fsPositronThetayEPlot = aida.cloud2D("MCParticle: FS Positron Thetay vs E");
		fsPositronThetayEPlot.annotation().addItem("xAxisLabel", "Particle Thetay angle [rad]");
		fsPositronThetayEPlot.annotation().addItem("yAxisLabel", "Particle Energy [GeV]");

                
		primaryEPlot = aida.cloud1D("MCParticle: Highest Primary E in Event");
		primaryEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

		eventEPlot = aida.cloud1D("MCParticle: Total Gen FS Electron E in Event");
		eventEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

                
                pFrame = new AIDAFrame();
                pFrame.setTitle("Truth MC Particle Plots");
                IPlotter pPlotter = af.createPlotterFactory().create();
                pPlotter.setTitle("Truth MC Types");
                pPlotter.createRegions(2,2);
                pPlotter.region(0).plot(fsCountPlot);
                pPlotter.region(1).plot(fsCountTypePlot);
                pPlotter.region(2).plot(fsCountEventTypePlot);
                pPlotter.region(3).plot(fsCountVsEventPlot);
                for(int i=0;i<4;++i) {
                    ((PlotterRegion) pPlotter.region(i)).getPlot().setAllowPopupMenus(true);
                    ((PlotterRegion) pPlotter.region(i)).getPlot().setAllowUserInteraction(true);
                }
                IPlotter pPlotter2 = af.createPlotterFactory().create();
                pPlotter2.setTitle("Truth MC Kinematics 1");
                pPlotter2.createRegions(2,2);
                pPlotter2.region(0).plot(fsEPlot);
                pPlotter2.region(0).plot(fsElectronEPlot,"mode=overlay");
                pPlotter2.region(0).plot(fsPositronEPlot,"mode=overlay");
                pPlotter2.region(0).plot(fsGammaEPlot,"mode=overlay");
                pPlotter2.region(0).style().dataStyle().fillStyle().setVisible(false);
                pPlotter2.region(1).plot(fsThetayPlot);
                pPlotter2.region(1).plot(fsElectronThetayPlot,"mode=overlay");
                pPlotter2.region(1).plot(fsPositronThetayPlot,"mode=overlay");
                pPlotter2.region(1).plot(fsGammaThetayPlot,"mode=overlay");
                pPlotter2.region(1).style().dataStyle().fillStyle().setVisible(false);
                        //.dataStyle().fillStyle()
                pPlotter2.region(2).plot(fsElectronThetayEPlot);
                pPlotter2.region(3).plot(fsPositronThetayEPlot);
                pPlotter2.region(2).style().setParameter("hist2DStyle", "colorMap");
                pPlotter2.region(2).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                pPlotter2.region(3).style().setParameter("hist2DStyle", "colorMap");
                pPlotter2.region(3).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                for(int i=0;i<4;++i) {
                    ((PlotterRegion) pPlotter2.region(i)).getPlot().setAllowPopupMenus(true);
                    ((PlotterRegion) pPlotter2.region(i)).getPlot().setAllowUserInteraction(true);
                }
                
                
                pFrame.addPlotter(pPlotter2);
                pFrame.addPlotter(pPlotter);
                pFrame.pack();
                pFrame.setVisible(!_hideFrame);
                
                
                
	}

	@Override
	public void process(EventHeader event) {

		// MCParticles
		List<MCParticle> mcparticles = event.get(MCParticle.class).get(0);

		// Final State particles.
		List<MCParticle> fsParticles = makeGenFSParticleList(mcparticles);

		//System.out.println("fsParticles="+fsParticles.size());
		fsCountPlot.fill(fsParticles.size());
                
                for (int i=0;i<fsParticles.size();++i) fsCountVsEventPlot.fill(event.getEventNumber());
                
                
                double fsGammaEmax = this.getHighestPhotonE(fsParticles);
                //if (fsGammaEmax>0.5) System.out.println("Emax large " + fsGammaEmax);
                int[] nelectrons = {0,0};
                int[] npositrons = {0,0};
                int[] ngammas = {0,0};
                int count = 0;
                double trigThr = 0.2;
		for (MCParticle fs : fsParticles) {
                        //System.out.println("Index " + count);

			double fsE = fs.getEnergy();
			double theta = Math.atan2(Math.sqrt(fs.getPX() * fs.getPX() + fs.getPY() * fs.getPY()), fs.getPZ());
			double thetay = Math.atan2(fs.getPY(), fs.getPZ());
			int fsPdg = fs.getPDGID();
			fsEPlot.fill(fsE);
                        this.fsThetayPlot.fill(Math.abs(thetay));
                        fsCountTypePlot.fill(fsPdg);
                        if(fsE>0.5) fsCountTypePlot500.fill(fsPdg);
			if (ParticleTypeClassifier.isElectron(fsPdg)) {
				fsElectronEPlot.fill(fsE);
				fsElectronThetaPlot.fill(theta);
				fsElectronThetayPlot.fill(Math.abs(thetay));
				if(fsE>trigThr) fsElectronThetayTrigPlot.fill(Math.abs(thetay));
				fsElectronThetayEPlot.fill(Math.abs(thetay),fsE);
                                nelectrons[0]++;
                                if(fsGammaEmax>0.5) nelectrons[1]++; 
			} else if (ParticleTypeClassifier.isPositron(fsPdg)) {
				fsPositronEPlot.fill(fsE);
				fsPositronThetaPlot.fill(theta);
				fsPositronThetayPlot.fill(Math.abs(thetay));
				if(fsE>trigThr) fsPositronThetayTrigPlot.fill(Math.abs(thetay));
				fsPositronThetayEPlot.fill(Math.abs(thetay),fsE);
                                npositrons[0]++;
                                if(fsGammaEmax>0.5) npositrons[1]++; 
			} else if (ParticleTypeClassifier.isPhoton(fsPdg)) {
				fsGammaEPlot.fill(fsE);
				fsGammaThetaPlot.fill(theta);
				fsGammaThetayPlot.fill(Math.abs(thetay));
				if(fsE>trigThr) fsGammaThetayTrigPlot.fill(Math.abs(thetay));
				fsGammaThetayEPlot.fill(Math.abs(thetay),fsE);
                                ngammas[0]++;
                                if(fsGammaEmax>0.5) {
                                    ngammas[1]++;
                                    //System.out.println("Counting high E gamma at count "+ count);
                                } 
			}
		}

                fsCountEventTypePlot.fill(getEventTypeId(nelectrons[0],npositrons[0],ngammas[0]));
                fsCountEventTypePlot2.fill(getEventTypeId(nelectrons[1],npositrons[1],ngammas[1]));

		// Sort MCParticles on energy.
		//Collections.sort(fsParticles, new MCParticleEComparator());

		// Energy of top two FS particles.
		//double e2 = fsParticles.get(0).getEnergy() + fsParticles.get(1).getEnergy();

		// Energy of top three FS particles.
		//double e3 = e2 + fsParticles.get(2).getEnergy();

		if (!fsParticles.isEmpty()) {
			// primary particle with most E
			double primaryE = getPrimary(fsParticles).getEnergy();
			primaryEPlot.fill(primaryE);
		}

		// event electron energy
		double eventE = getPrimaryElectronE(fsParticles);
		eventEPlot.fill(eventE);
	}

        
        public int getEventTypeId(int ne, int np, int ng) {
            //get a unique ID depending on final state particle count
            if(ne==0 && np==0 & ng==0) return -1;
            if(ne==1 && np==0 & ng==0) return 1;
            if(ne==0 && np==1 & ng==0) return 2;
            if(ne==0 && np==0 & ng==1) return 3;
            if(ne==1 && np==1 & ng==0) return 4;
            if(ne==1 && np==0 & ng==1) return 5;
            if(ne==0 && np==1 & ng==1) return 6;
            if(ne==1 && np==1 & ng==1) return 7;
            if(ne==2 && np==1) return 8;
            if(ne==2 && np==1 & ng ==0 ) return 9;
            if(ne==2 && np==1 & ng ==1 ) return 10;
            if(ne>2) return 11;
            if(np>1) return 12;
            if(ng>1) return 13;
            
            return 0;
        }
        
	public double getHighestPhotonE(List<MCParticle> particles) {
		double Emax = -1;
                double E=0;
                int count = 0;
		for (MCParticle particle : particles) {
			if (ParticleTypeClassifier.isPhoton(particle.getPDGID())) {
				E = particle.getEnergy();
                                if(E>Emax) {
                                    Emax = E;
                                    //System.out.println("Emax from photon with index " + count);
                                }
			count++;
                        }
		}
		return Emax;
	}

        private double getPrimaryElectronE(List<MCParticle> particles) {
		double totalE = 0;
		for (MCParticle particle : particles) {
			if (Math.abs(particle.getPDGID()) == 11) {
				totalE += particle.getEnergy();
			}
		}
		return totalE;
	}

	private MCParticle getPrimary(List<MCParticle> particles) {
		double maxE = 0;
		MCParticle primary = null;
		for (MCParticle particle : particles) {
			if (particle.getEnergy() > maxE) {
				maxE = particle.getEnergy();
				primary = particle;
			}
		}
		return primary;
	}

	public static List<MCParticle> makeGenFSParticleList(List<MCParticle> mcparticles) {
		List<MCParticle> fsParticles = new ArrayList<MCParticle>();
		for (MCParticle mcparticle : mcparticles) {
			if (mcparticle.getGeneratorStatus() == MCParticle.FINAL_STATE) {
				double theta = Math.atan2(Math.sqrt(mcparticle.getPX() * mcparticle.getPX() + mcparticle.getPY() * mcparticle.getPY()), mcparticle.getPZ());
				if (theta > 1e-3) {
					fsParticles.add(mcparticle);
				}
			}
		}
		return fsParticles;
	}
        
        
        public void endOfData() {
            if(this._hideFrame) pFrame.dispose();
                
        }
}
