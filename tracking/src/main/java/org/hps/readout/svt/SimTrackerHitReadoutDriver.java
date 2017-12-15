package org.hps.readout.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.SLICDataReadoutDriver;
import org.hps.readout.util.LcsimCollection;
import org.hps.readout.util.LcsimSingleEventCollectionData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;

public class SimTrackerHitReadoutDriver extends SLICDataReadoutDriver<SimTrackerHit> {
	private Subdetector detector = null;
	private LcsimCollection<FpgaData> fpgaDataParams = null;
	
	public SimTrackerHitReadoutDriver() {
		super(SimTrackerHit.class, 0xc0000000);
	}
	
	@Override
	public void detectorChanged(Detector detector) {
		this.detector = detector.getSubdetector("Tracker");
	}
	
	@Override
	public void startOfData() {
		// Run the superclass method.
		super.startOfData();
		
		// Create the LCSim collection parameters for the FPGA data.
		// String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement
		fpgaDataParams = new LcsimCollection<FpgaData>("FPGAData", this, FpgaData.class, 0.0);
	}
	
	protected Collection<LcsimSingleEventCollectionData<?>> getOnTriggerData(double triggerTime) {
		// Get the FPGA data.
		List<FpgaData> fpgaData = new ArrayList<FpgaData>(makeFPGAData(detector).values());
		
		// Create the FPGA data collection.
		LcsimSingleEventCollectionData<FpgaData> fpgaCollection = new LcsimSingleEventCollectionData<FpgaData>(fpgaDataParams);
		fpgaCollection.getData().addAll(fpgaData);
		
		// Create a general list for the collection.
		List<LcsimSingleEventCollectionData<?>> collectionsList = new ArrayList<LcsimSingleEventCollectionData<?>>(1);
		collectionsList.add(fpgaCollection);
		
		// Return the collections list result.
		return collectionsList;
	}
	
	private Map<Integer, FpgaData> makeFPGAData(Subdetector subdetector) {
		double[] temps = new double[HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID];
		for(int i = 0; i < HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID; i++) {
			temps[i] = 23.0;
		}
		
		Map<Integer, FpgaData> fpgaData = new HashMap<Integer, FpgaData>();
		List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
		
		List<Integer> fpgaNumbers = new ArrayList<Integer>();
		for(HpsSiSensor sensor : sensors) {
			if(sensor instanceof HpsTestRunSiSensor && !fpgaNumbers.contains(((HpsTestRunSiSensor) sensor).getFpgaID())) {
				fpgaNumbers.add(((HpsTestRunSiSensor) sensor).getFpgaID());
			}
		}
		//===> for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
		for(Integer fpgaNumber : fpgaNumbers) {
			fpgaData.put(fpgaNumber, new FpgaData(fpgaNumber, temps, 0));
		}
		
		return fpgaData;
	}
	
	@Override
	protected void writeData(List<SimTrackerHit> data) {
		writer.write("Event Time: " + ReadoutDataManager.getCurrentTime());
		for(SimTrackerHit hit : data) {
			String output = String.format("\tCell ID: %d;    Energy: %f;   (x, y, z): (%f, %f, %f);   p: (%f, %f, %f);   l: %f;   t: %f",
					hit.getCellID(), hit.getdEdx(), hit.getPosition()[0], hit.getPosition()[1], hit.getPosition()[2],
					hit.getMomentum()[0], hit.getMomentum()[1], hit.getMomentum()[2], hit.getPathLength(), hit.getTime());
			writer.write(output);
		}
	}
}