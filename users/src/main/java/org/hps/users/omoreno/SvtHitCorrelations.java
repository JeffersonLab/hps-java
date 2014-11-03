package org.hps.users.omoreno;

import hep.aida.IPlotter;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Driver to find the correlations between stereo hits.
 *  
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class SvtHitCorrelations extends Driver {

	private AIDA aida;
	private List<IPlotter> plotters = new ArrayList<IPlotter>();
	
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    boolean taa = true;
    boolean tas = true;
    boolean baa = true;
    boolean bas = true;
    
    
	protected void detectorChanged(Detector detector){
		
		aida = AIDA.defaultInstance();
		aida.tree().cd("/");
		
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
					nPlotters++;
				}
			}
		}
	
		for(IPlotter plotter : plotters) plotter.show();
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
	
			HpsSiSensor sensor1 = (HpsSiSensor) rawHit1.getDetectorElement();
			int layer1 = (int) Math.ceil(((double) sensor1.getLayerNumber())/2);
			int channel1 = rawHit1.getIdentifierFieldValue("strip");
			
			for(RawTrackerHit rawHit2 : rawHits){
			
				HpsSiSensor sensor2 = (HpsSiSensor) rawHit2.getDetectorElement();
				int layer2 = (int) Math.ceil(((double) sensor2.getLayerNumber())/2);
				int channel2 = rawHit2.getIdentifierFieldValue("strip");
			
				if(sensor1.isTopLayer() && sensor2.isTopLayer()){
					if(sensor1.isAxial() && sensor2.isAxial() && taa){
						String plotName = "Top Channel Correlation: Axial Layer " + layer1 + " vs Axial Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					} else if(sensor1.isAxial() && !sensor2.isAxial() && tas){
						String plotName = "Top Channel Correlation: Axial Layer " + layer1 + " vs Stereo Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					}
				} else if(!sensor1.isTopLayer() && !sensor2.isTopLayer() && baa){
					if(sensor1.isAxial() && sensor2.isAxial()){
						String plotName = "Bottom Channel Correlation: Axial Layer " + layer1 + " vs Axial Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					} else if(sensor1.isAxial() && !sensor2.isAxial() && bas){
						String plotName = "Bottom Channel Correlation: Axial Layer " + layer1 + " vs Stereo Layer " + layer2;
						aida.histogram2D(plotName).fill(channel1, channel2);
					}
				}
			}
		}
	}
	
	

}
