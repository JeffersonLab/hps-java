package org.hps.users.spaul.feecc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hps.conditions.ConditionsDriver;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.lcio.LCIOReader;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSetFactory;
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
			if(arg[arg.length -1].equals("display"))
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
			beamTiltY = Double.parseDouble(arg[4]);
			beamTiltX = Double.parseDouble(arg[5]);
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
	static IHistogram1D h3, /*h3a,*/ h3_t, h3_b;
	static IHistogram1D h5, h5a; 
	//static IHistogram2D h6, h6a;
	static IHistogram1D h7, h7a;
	static IHistogram1D h8;
	static IHistogram1D h9_t, h9_b;
	static IHistogram1D h10_t, h10_b;
	private static IHistogram1D h4y;

	private static void extractHistograms(ITree tree0) {
		h1 = (IHistogram2D) tree0.find("theta vs energy");

		h2 = (IHistogram2D) tree0.find("theta vs phi");
		h2a = (IHistogram2D) tree0.find("theta vs phi cut");
		h2b = (IHistogram2D) tree0.find("theta vs phi cut alt");
		h2c = (IHistogram2D) tree0.find("theta vs phi alt");

		h3 = (IHistogram1D) tree0.find("theta");
		//h3a = (IHistogram1D) tree0.find("theta isolated ");
		h3_t = (IHistogram1D) tree0.find("theta top");
		h3_b = (IHistogram1D) tree0.find("theta bottom");
		
		h4 = (IHistogram2D) tree0.find("px\\/pz vs py\\/pz");
		h4a = (IHistogram2D) tree0.find("px\\/pz vs py\\/pz cut");
		System.out.println(h4a.xAxis().bins());
		h5 = (IHistogram1D) tree0.find("energy top");
		h5 = (IHistogram1D) tree0.find("energy bottom");

//		h6 = (IHistogram2D) tree0.find("cluster");
//		h6a = (IHistogram2D) tree0.find("cluster matched");
		h7 = (IHistogram1D) tree0.find("y top");
		h7a = (IHistogram1D) tree0.find("y bottom");
		h8 = (IHistogram1D) tree0.find("seed energy");
		

		h9_t = (IHistogram1D) tree0.find("pz top");
		h9_b = (IHistogram1D) tree0.find("pz bottom");
		

		h10_t = (IHistogram1D) tree0.find("clustsize top");
		h10_b = (IHistogram1D) tree0.find("clustsize bottom");

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

		double eBins[] = new double[66];
		for(int i = 0; i<66; i++){
			eBins[i] = i/50.;  //every 20 MeV up to 1300 MeV
		}


		h1 = hf.createHistogram2D("theta vs energy", "theta vs energy", thetaBins, eBins);


		//identical to h2a, except different binning
		h2b = hf.createHistogram2D("theta vs phi cut alt", "theta vs phi cut alt", thetaBins, phiBins);
		h2c = hf.createHistogram2D("theta vs phi alt", "theta vs phi alt", thetaBins, phiBins);

		h3 = hf.createHistogram1D("theta", "theta", thetaBins);
//		h3a = hf.createHistogram1D("theta isolated ", "theta isolated", thetaBins);

		h3_t = hf.createHistogram1D("theta top", "theta top", thetaBins);
		h3_b = hf.createHistogram1D("theta bottom", "theta bottom", thetaBins);

		
		h4 = hf.createHistogram2D("px\\/pz vs py\\/pz", 300, -.16, .24, 300, -.2, .2);
		h4a = hf.createHistogram2D("px\\/pz vs py\\/pz cut", 300, -.16, .24, 300, -.2, .2);
		h4y = hf.createHistogram1D("py\\pz", 1200, -.06, .06);
		
		h5 = hf.createHistogram1D("energy top", 75, 0, 1.5);
		h5a = hf.createHistogram1D("energy bottom", 75, 0, 1.5);
		

		h9_t = hf.createHistogram1D("pz top", 75, 0, 1.5);
		h9_b = hf.createHistogram1D("pz bottom", 75, 0, 1.5);
		
//		h6 = hf.createHistogram2D("cluster", 47, -23.5, 23.5, 11, -5.5, 5.5);
//		h6a = hf.createHistogram2D("cluster matched", 47, -23.5, 23.5, 11, -5.5, 5.5);

		h7 = hf.createHistogram1D("y top", 500, 0, 100);

		h7a = hf.createHistogram1D("y bottom", 500, 0, 100);

		h8 = hf.createHistogram1D("seed energy", 120, 0, 1.2);
		
		h10_t = hf.createHistogram1D("clustsize top", 10,0, 10);
		h10_b = hf.createHistogram1D("clustsize bottom", 10,0, 10);
	}
	static void setupPlotter(IAnalysisFactory af){
		IPlotterFactory pf = af.createPlotterFactory();
		IPlotter p = pf.create();
		p.createRegions(2,2);
		p.region(0).plot(h2);
		StyleUtil.stylize(p.region(0), "theta", "phi");
//		p.region(1).plot(h3a);
		StyleUtil.stylize(p.region(1), "theta", "# of particles");
		p.region(2).plot(h9_t);
		p.region(2).plot(h9_b);
		StyleUtil.noFillHistogramBars(p.region(2));
		StyleUtil.stylize(p.region(2), "pztilt" ,"pztilt", "# of particles");
		p.region(3).plot(h5);
		p.region(3).plot(h5a);
		StyleUtil.noFillHistogramBars(p.region(3));
		StyleUtil.stylize(p.region(3), "energy", "# of particles");

		p.show();
		
		//new window for the next plot
		IPlotter p2 = pf.create();
		p2.region(0).plot(h2b);
		//IDataPointSetFactory dpsf = af.createDataPointSetFactory(af.createTreeFactory().create());

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

		IPlotter p6 = pf.create("efficiency");
		p6.createRegions(1,2);
//		p6.region(0).plot(h6);
		StyleUtil.stylize(p6.region(0), "ix", "iy");
//		p6.region(1).plot(h6a);
		StyleUtil.stylize(p6.region(1), "ix", "iy");
		p6.show();

		IPlotter p7 = pf.create("theta vs energy");
		//p6.createRegions(1,2);
		p7.region(0).plot(h1);
		StyleUtil.stylize(p7.region(0), "theta", "energy");
		//		StyleUtil.stylize(p6.region(1), "ix", "iy");
		p7.show();

		IPlotter p8 = pf.create("y");
		//p6.createRegions(1,2);
		p8.region(0).plot(h7);
		p8.region(0).plot(h7a);
		StyleUtil.stylize(p8.region(0), "y", "# of particles");
		//		StyleUtil.stylize(p6.region(1), "ix", "iy");
		p8.show();
		
		IPlotter p9 = pf.create("theta: top vs. bottom");
		//p6.createRegions(1,2);
		p9.region(0).plot(h3_t);
		p9.region(0).plot(h3_b);
		StyleUtil.stylize(p9.region(0), "theta", "theta", "# of particles");
		StyleUtil.noFillHistogramBars(p9.region(0));
				//StyleUtil.stylize(p6.region(1), "ix", "iy");
		p9.show();
		
		IPlotter p10 = pf.create("seed energy");
		//p6.createRegions(1,2);
		p10.createRegions(2,1);
		p10.region(0).plot(h8);
		StyleUtil.stylize(p10.region(0), "seed energy", "seed energy (GeV)", "# of particles");
		
		p10.region(1).plot(h10_t);
		p10.region(1).plot(h10_b);
		StyleUtil.noFillHistogramBars(p10.region(1));
		StyleUtil.stylize(p10.region(1), "clust size", "n ecal hits", "# of particles");
		
		//StyleUtil.noFillHistogramBars(p10.region(0));
				//StyleUtil.stylize(p6.region(1), "ix", "iy");
		p10.show();

	}
	private static void processEvent(EventHeader event) {
		if(event.getEventNumber() %10000 == 0)
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
		particles = RemoveDuplicateParticles.removeDuplicateParticles(particles);
		outer : for(ReconstructedParticle p : particles){
			

			boolean isGood = addParticle(p);
			

		}
		
	}

	

	static double eMin = .8;
	static double eMax = 1.2;
	static double beamEnergy = 1.057;

	static double beamTiltX = .03057;
	static double beamTiltY;
	static double maxChi2 = 50;
	//maximum difference between the reconstructed energy and momentum
	static double maxdE = .5;

	static double seedEnergyCut = .4;


	static boolean addParticle(ReconstructedParticle part){
		if(part.getTracks().size() != 0){
			if(part.getMomentum().magnitudeSquared() > .8
					&& part.getTracks().get(0).getChi2() > maxChi2){
				h4y.fill(part.getMomentum().y()/part.getMomentum().z());
			}
		}
		if(part.getCharge() != -1)
			return false;
		if(part.getClusters().size() == 0)
			return false;
		Cluster c = part.getClusters().get(0);
		double time = c.getCalorimeterHits().get(0).getTime();

		if(!(time>40 && time <50))
			return false;
		double seedEnergy = 0;
		for(CalorimeterHit hit : c.getCalorimeterHits()){
			if(hit.getCorrectedEnergy() > seedEnergy)
				seedEnergy = hit.getCorrectedEnergy();
		}
		h8.fill(seedEnergy);
		
		
		if(seedEnergy < seedEnergyCut)
			return false;
		
		if(c.getPosition()[1] > 0){
			h10_t.fill(c.getSize());
		}
		else{ 
			h10_b.fill(c.getSize());
		}
		
		
		if(c.getCalorimeterHits().size() < 3)
			return false;
		

		if(c.getEnergy() > eMin && c.getEnergy() < eMax){
			if(c.getPosition()[1] > 0)
				h7.fill(c.getPosition()[1]);
			else if(c.getPosition()[1] < 0)
				h7a.fill(-c.getPosition()[1]);
		}
		
		if(EcalUtil.fid_ECal(c)){
			
			if(c.getPosition()[1] > 0){
				h5.fill(c.getEnergy());
			}
			else{ 
				h5a.fill(c.getEnergy());
			}
			if(part.getTracks().size() == 0)
				return false;
			Track t = part.getTracks().get(0);
			if(t.getChi2()>maxChi2){
				return false;
			}
			if(!TrackType.isGBL(t.getType()))
				return false;
			
			
			
			Hep3Vector p = part.getMomentum();



			double px = p.x(), pz = p.z();
			double pxtilt = px*Math.cos(beamTiltX)-pz*Math.sin(beamTiltX);
			double py = p.y();
			double pztilt = pz*Math.cos(beamTiltX)+px*Math.sin(beamTiltX);
			
			double pytilt = py*Math.cos(beamTiltY)-pztilt*Math.sin(beamTiltY);
			pztilt = pz*Math.cos(beamTiltY) + pytilt*Math.sin(beamTiltY);

			if(Math.abs(pztilt - c.getEnergy()) > maxdE)
				return false;
			if(c.getPosition()[1] > 0)
				h9_t.fill(pztilt);
			else
				h9_b.fill(pztilt);

			double theta = Math.atan(Math.hypot(pxtilt, pytilt)/pztilt);
			double phi =Math.atan2(pytilt, pxtilt);
			boolean inRange = cb.inRange(theta, phi);
			if(inRange)
				h1.fill(theta, c.getEnergy());



			if(c.getEnergy() > eMin && c.getEnergy() < eMax) {



				h2.fill(theta, phi);
				h2c.fill(theta, phi);

				h4.fill(px/pz, py/pz);
				//h4y.fill(py/pz);

				if(inRange){

					//System.out.println(c.getEnergy() + " " + t.getType());
					/*for(TrackState ts : t.getTrackStates()){
						if(ts.getLocation() == TrackState.AtIP)
							System.out.println(Arrays.toString( 
									ts.getReferencePoint()));
					}*/
					h2a.fill(theta, phi);
					h2b.fill(theta, phi);
					
					h3.fill(theta);
					if(py > 0)
						h3_t.fill(theta);
					else 
						h3_b.fill(theta);
					//if(h3_t.sumBinHeights()+h3_b.sumBinHeights() != h3.sumBinHeights())
						//System.out.println("NABO ERROR");
					
					
					h4a.fill(px/pz, py/pz);
				}


				return true;
			}

		}
		return false;
	}
	
}