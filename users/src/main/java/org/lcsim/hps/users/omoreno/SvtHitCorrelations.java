package org.lcsim.hps.users.omoreno;

//--- java ---//
//--- aida ---//
import hep.aida.IPlotter;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.SvtUtils;
import org.hps.util.AIDAFrame;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
//--- org.lcsim ---//
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
//--- hps-java ---//

/**
 * 
 * @author Omar Moreno
 * @version $Id: SvtHitCorrelations.java,v 1.2 2013/10/25 19:45:01 jeremy Exp $
 *
 */
public class SvtHitCorrelations extends Driver {

	private AIDA aida;
	private List<AIDAFrame> frames = new ArrayList<AIDAFrame>();
	private List<IPlotter> plotters = new ArrayList<IPlotter>();
	
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    boolean taa = true;
    boolean tas = true;
    boolean baa = true;
    boolean bas = true;
    
    
	protected void detectorChanged(Detector detector){
		
		aida = AIDA.defaultInstance();
		aida.tree().cd("/");
		
		for(int index = 0; index < 4; index++) frames.add(new AIDAFrame());
	
		frames.get(0).setTitle("Top Correlation Plots: Axial vs Axial");
		frames.get(1).setTitle("Top Correlation Plots: Axial vs Stereo");
		frames.get(2).setTitle("Bottom Correlation Plots: Axial vs Axial");
		frames.get(3).setTitle("Bottom Correlation Plots: Axial vs Stereo");

		int nPlotters = 0;
		String plotName;
		
		// Create top volume axial plots
		if(taa){
			for(int layer1 = 1; layer1 <= 5; layer1++ ){
				for(int layer2 = 1; layer2 <= 5; layer2++){
					plotName = "Top Channel Correlation: Axial Layer " + layer1 + " vs Axial Layer " + layer2;
					plotters.add(aida.analysisFactory().createPlotterFactory().create(plotName));
					plotters.get(nPlotters).region(0).plot(aida.histogram2D(plotName, 320, 0, 639, 320, 0, 639));
					plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
					plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
					plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
					frames.get(0).addPlotter(plotters.get(nPlotters));
					nPlotters++;
				}
			}
		}
		
		if(tas){
			for(int layer1 = 1; layer1 <= 5; layer1++ ){
				for(int layer2 = 1; layer2 <= 5; layer2++){
					plotName = "Top Channel Correlation: Axial Layer " + layer1 + " vs Stereo Layer " + layer2;
					plotters.add(aida.analysisFactory().createPlotterFactory().create(plotName));
					plotters.get(nPlotters).region(0).plot(aida.histogram2D(plotName, 320, 0, 639, 320, 0, 639));
					plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
					plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
					plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
					frames.get(1).addPlotter(plotters.get(nPlotters));
					nPlotters++;
				}
			}
		}
	
		// Create bottom volume axial plots
		if(baa){
			for(int layer1 = 1; layer1 <= 5; layer1++ ){
				for(int layer2 = 1; layer2 <= 5; layer2++){
					plotName = "Bottom Channel Correlation: Axial Layer " + layer1 + " vs Axial Layer " + layer2;
					plotters.add(aida.analysisFactory().createPlotterFactory().create(plotName));
					plotters.get(nPlotters).region(0).plot(aida.histogram2D(plotName, 320, 0, 639, 320, 0, 639));
					plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
					plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
					plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
					frames.get(2).addPlotter(plotters.get(nPlotters));
					nPlotters++;
				}
			}
		}
		
		if(bas){
			for(int layer1 = 1; layer1 <= 5; layer1++ ){
				for(int layer2 = 1; layer2 <= 5; layer2++){
					plotName = "Bottom Channel Correlation: Axial Layer " + layer1 + " vs Stereo Layer " + layer2;
					plotters.add(aida.analysisFactory().createPlotterFactory().create(plotName));
					plotters.get(nPlotters).region(0).plot(aida.histogram2D(plotName, 320, 0, 639, 320, 0, 639));
					plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
					plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
					plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
					frames.get(3).addPlotter(plotters.get(nPlotters));
					nPlotters++;
				}
			}
		}
		
		for(AIDAFrame frame : frames){
        	frame.pack();
        	frame.setVisible(true);
        }
	}
	
	/**
	 * 
	 */
	public void setEnableTopAxialAxial(boolean flag){
		this.taa = flag;
	}
	
	/**
	 * 
	 */
	public void setEnableTopAxialStereo(boolean flag){
		this.tas = flag;
	}
	
	/**
	 * 
	 */
	public void setEnableBottomAxialAxial(boolean flag){
		this.baa = flag;
	}
	
	/**
	 * 
	 */
	public void setEnableBottomAxialStereo(boolean flag){
		this.bas = flag;
	}

	public void process(EventHeader event){
		
		if(!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)){
			System.out.println(this.getClass().getSimpleName() + ": Event does not have RawTrackerHits, skipping event ...");
			return;
		}
	
		List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
		
		for(RawTrackerHit rawHit1 : rawHits){
	
			SiSensor sensor1 = (SiSensor) rawHit1.getDetectorElement();
			int layer1 = (int) Math.ceil(((double) SvtUtils.getInstance().getLayerNumber(sensor1))/2);
			int channel1 = rawHit1.getIdentifierFieldValue("strip");
			
			for(RawTrackerHit rawHit2 : rawHits){
			
				SiSensor sensor2 = (SiSensor) rawHit2.getDetectorElement();
				int layer2 = (int) Math.ceil(((double) SvtUtils.getInstance().getLayerNumber(sensor2))/2);
				int channel2 = rawHit2.getIdentifierFieldValue("strip");
			
				if(SvtUtils.getInstance().isTopLayer(sensor1) && SvtUtils.getInstance().isTopLayer(sensor2)){
					if(SvtUtils.getInstance().isAxial(sensor1) && SvtUtils.getInstance().isAxial(sensor2) && taa){
						String plotName = "Top Channel Correlation: Axial Layer " + layer1 + " vs Axial Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					} else if(SvtUtils.getInstance().isAxial(sensor1) && !SvtUtils.getInstance().isAxial(sensor2) && tas){
						String plotName = "Top Channel Correlation: Axial Layer " + layer1 + " vs Stereo Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					}
				} else if(!SvtUtils.getInstance().isTopLayer(sensor1) && !SvtUtils.getInstance().isTopLayer(sensor2) && baa){
					if(SvtUtils.getInstance().isAxial(sensor1) && SvtUtils.getInstance().isAxial(sensor2)){
						String plotName = "Bottom Channel Correlation: Axial Layer " + layer1 + " vs Axial Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					} else if(SvtUtils.getInstance().isAxial(sensor1) && !SvtUtils.getInstance().isAxial(sensor2) && bas){
						String plotName = "Bottom Channel Correlation: Axial Layer " + layer1 + " vs Stereo Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					}
				}
			}
		}
	}
	
	

}
