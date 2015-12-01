package org.hps.users.spaul.feecc;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hps.conditions.ConditionsDriver;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.lcio.LCIOReader;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.physics.vec.Hep3Vector;
import org.hps.users.spaul.StyleUtil;

public class MakeHistograms {
	static boolean display = false;
	static CustomBinning cb;
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		if(arg.length == 1){
			File file = new File(arg[0]);
			String path = arg[0];
			if(file.isDirectory()){
				org.hps.users.spaul.SumEverything.main(new String[]{path, "temp.aida"});
				path = "temp.aida";
			}
			IAnalysisFactory af = IAnalysisFactory.create();
			 ITreeFactory tf = af.createTreeFactory();
			ITree tree0 = tf.create(path, "xml");
			extractHistograms(tree0);
			setupPlotter(af);
			
		} else{

			String input = arg[0];
			String output = arg[1];
			cb = new CustomBinning(new File(arg[2]));
			if(arg.length == 5)
				display = true;
			IAnalysisFactory af = IAnalysisFactory.create();
			ITree tree = af.createTreeFactory().create(output,"xml",false,true);
			IHistogramFactory hf = af.createHistogramFactory(tree);
			setupHistograms(hf);
			if(display){
				setupPlotter(af);
			}
			ConditionsDriver hack = new ConditionsDriver();
			//hack.setXmlConfigResource("/u/group/hps/hps_soft/detector-data/detectors/HPS-EngRun2015-Nominal-v3");
			hack.setDetectorName("HPS-EngRun2015-Nominal-v3");
			hack.setFreeze(true);
			hack.setRunNumber(Integer.parseInt(arg[3]));
			hack.initialize();
			LCIOReader reader = new LCIOReader(new File(input));
			//reader.open(input);
			//reader.
			EventHeader event = reader.read();
			int nEvents = 0;
			try{
				outer : while(event != null){
					processEvent(event);

					//System.out.println(Q2);

					event = reader.read();
				}
			} catch (Exception e){
				e.printStackTrace();
			}
			tree.commit();
			tree.close();
		}

	}
	
	static IHistogram2D h1, h2, h2a, h2b, h2c;
	static IHistogram2D h4,h4a;
	static IHistogram1D h3, h3a;
	static IHistogram1D h5, h6;
	
	private static void extractHistograms(ITree tree0) {
		h2 = (IHistogram2D) tree0.find("theta vs phi");
		h2a = (IHistogram2D) tree0.find("theta vs phi cut");
		h2b = (IHistogram2D) tree0.find("theta vs phi cut alt");
		h2c = (IHistogram2D) tree0.find("theta vs phi alt");
		
		h3 = (IHistogram1D) tree0.find("theta");
		h4 = (IHistogram2D) tree0.find("px\\/pz vs py\\/pz");
		h4a = (IHistogram2D) tree0.find("px\\/pz vs py\\/pz cut");
		h5 = (IHistogram1D) tree0.find("energy");
		
	}
	static void setupHistograms(IHistogramFactory hf){
		//h1 = hf.createHistogram2D("px\\/pz vs py\\/pz", 160, -.16, .24, 160, -.2, .2);
		
		
		
		
		h2 = hf.createHistogram2D("theta vs phi", 300, 0, .3, 314, -3.14, 3.14);
		
		h2a = hf.createHistogram2D("theta vs phi cut", 300, 0, .3, 314, -3.14, 3.14);

		double thetaBins[] = new double[cb.nTheta+1];
		for(int i = 0; i<cb.nTheta; i++){
			thetaBins[i] = cb.thetaMin[i];
		}
		
		thetaBins[thetaBins.length-1] = cb.thetaMax[cb.nTheta-1];

		double phiBins[] = new double[315];
		for(int i = 0; i<315; i++){
			phiBins[i] = i/50.-3.14;  //every 10 mrad;
		}
		
		//identical to h2a, except different binning
		h2b = hf.createHistogram2D("theta vs phi cut alt", "theta vs phi cut alt", thetaBins, phiBins);
		h2c = hf.createHistogram2D("theta vs phi alt", "theta vs phi alt", thetaBins, phiBins);
		
		h3 = hf.createHistogram1D("theta", "theta", thetaBins);

		h4 = hf.createHistogram2D("px\\/pz vs py\\/pz", 160, -.16, .24, 160, -.2, .2);
		h4a = hf.createHistogram2D("px\\/pz vs py\\/pz cut", 160, -.16, .24, 160, -.2, .2);
		
		h5 = hf.createHistogram1D("energy", 75, 0, 1.5);
	}
	static void setupPlotter(IAnalysisFactory af){
		IPlotterFactory pf = af.createPlotterFactory();
		IPlotter p = pf.create();
		p.createRegions(2,2);
		p.region(0).plot(h2);

		StyleUtil.stylize(p.region(0), "theta", "phi");
		p.region(1).plot(h2a);
		StyleUtil.stylize(p.region(1), "theta", "phi");
		p.region(2).plot(h3);
		StyleUtil.stylize(p.region(2), "theta", "# of particles");
		p.region(3).plot(h5);
		StyleUtil.stylize(p.region(3), "energy", "# of particles");
		
		p.show();
		//new window for the next plot
		IPlotter p2 = pf.create();
		p2.region(0).plot(h2b);
		StyleUtil.stylize(p2.region(0), "theta", "phi");
		
		p2.show();
		
		//new window for the next plot
		IPlotter p3 = pf.create();
		p3.region(0).plot(h2c);
		StyleUtil.stylize(p3.region(0), "theta", "phi");
		
		p3.show();
		
		//new window for the next plot
		IPlotter p4 = pf.create();
		p4.region(0).plot(h4);
		StyleUtil.stylize(p4.region(0), "px/pz", "py/pz");
		
		p4.show();
		
		//new window for the next plot
		IPlotter p5 = pf.create();
		p5.region(0).plot(h4a);
		StyleUtil.stylize(p5.region(0), "px/pz", "py/pz");
		
		p5.show();
	}
	private static void processEvent(EventHeader event) {
		if(event.getEventNumber() %1000 == 0)
			System.out.println("event number " + event.getEventNumber());
		
		for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
		{
			if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
			TIData tid = new TIData(gob);
			if (!tid.isSingle1Trigger())
			{
				return;
			}
		}
		List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, "FinalStateParticles");

		for(ReconstructedParticle p : particles){

			boolean isGood = addParticle(p);


		}
	}

	static double eMin = .8;
	static double eMax = 1.2;
	static double beamEnergy = 1.057;

	static double beamTilt = .03057;
	static double maxChi2 = 50;
	static boolean addParticle(ReconstructedParticle part){
		
		if(part.getTracks().size() == 0)
			return false;
		if(part.getTracks().get(0).getChi2()>maxChi2){
			return false;
		}
		if(part.getClusters().size() == 0)
			return false;
		Cluster c = part.getClusters().get(0);
		double time = c.getCalorimeterHits().get(0).getTime();
		if(EcalUtil.fid_ECal(c)){
			if(c.getCalorimeterHits().size() < 3)
				return false;
			if(time>40 && time <48)
				h5.fill(c.getEnergy());
			if(c.getEnergy() > eMin && c.getEnergy() < eMax && (time >40 && time < 48)) {

				Hep3Vector p = part.getMomentum();
				
				double px = p.x(), pz = p.z();
				double pxtilt = px*Math.cos(beamTilt)-pz*Math.sin(beamTilt);
				double py = p.y();
				double pztilt = pz*Math.cos(beamTilt)+px*Math.sin(beamTilt);

				double theta = Math.atan(Math.hypot(pxtilt, py)/pztilt);
				double phi =Math.atan2(py, pxtilt);

				h2.fill(theta, phi);
				h2c.fill(theta, phi);
				
				h4.fill(px/pz, py/pz);
				
				if(cb.inRange(theta, phi)){
					h2a.fill(theta, phi);
					h2b.fill(theta, phi);
					h3.fill(theta);
					h4a.fill(px/pz, py/pz);
				}


				return true;
			}

		}
		return false;
	}

}