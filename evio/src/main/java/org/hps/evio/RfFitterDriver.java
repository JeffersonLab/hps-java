package org.hps.evio;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.ecal.FADCGenericHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Extract RF time from waveform and put into lcsim event.
 */
public class RfFitterDriver extends Driver {

    private static final Logger LOGGER = Logger.getLogger(RfFitterDriver.class.getPackage().getName());
   
    // FIXME:  move crate/slot/channel to conditions database
    private static final double NOISE = 2.0; // units = FADC
    private static final int CRATE = 46;
    private static final int SLOT = 13;
    private static final int CHANNELS[] = {0, 1};
    private static final int CRATE2019 = 39;
    private static final int SLOT2019 = 13;
    private static final int CHANNELS2019[] = {0, 2};
    private static final double NSPERSAMPLE = 4;

    // boilerplate:
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory analysisFactory = aida.analysisFactory();
    //private IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(null);
    private IFitFactory fitFactory = analysisFactory.createFitFactory();
    private IFitter fitter = fitFactory.createFitter();
    private IDataPointSet fitData = aida.analysisFactory().createDataPointSetFactory(null).create("RF ADC DataPointSet", 2);

    // the function used to fit the RF pulse:
    private IFunction fitFunction = new RfFitFunction();

    /**
     * Check the event for an RF pulse, and, if found, fit it to get RF time.
     */
    public void process(EventHeader event) {

        List<RfHit> rfHits = new ArrayList<RfHit>();

        boolean is2016 = false;
        boolean is2019 = false;
        boolean foundRf = false;
        double times[] = {-9999, -9999};

        if (event.hasCollection(GenericObject.class, "FADCGenericHits")) {

            for (GenericObject gob : event.get(GenericObject.class, "FADCGenericHits")) {

                FADCGenericHit hit = null;

                /* Added conversion from GenericObject in case loading back from an LCIO file. --JM */
                if (gob instanceof FADCGenericHit) {
                    hit = (FADCGenericHit) gob;
                } else {
                    hit = new FADCGenericHit(gob);
                }

                // ignore hits not from proper RF signals based on crate/slot/channel:
                if (hit.getCrate()==CRATE && hit.getSlot()==SLOT) {
                    for (int ii = 0; ii < CHANNELS.length; ii++) {
                        if (hit.getChannel() == CHANNELS[ii]) {
                            if (is2019) {
                                LOGGER.log(Level.SEVERE, "Mixed 2019/2016 RF signals.");
                                return;
                            }
                            else {
                                // we found a RF readout, fit it:
                                is2016 = true;
                                foundRf = true;
                                times[ii] = fitPulse(hit);
                                break;
                            }
                        }
                    }
                }
                else if (hit.getCrate()==CRATE2019 && hit.getSlot()==SLOT2019) {
                    for (int ii = 0; ii < CHANNELS2019.length; ii++) {
                        if (hit.getChannel() == CHANNELS2019[ii]) {
                            if (is2016) {
                                LOGGER.log(Level.SEVERE, "Mixed 2019/2016 RF signals.");
                                return;
                            }
                            else {
                                // we found a RF readout, fit it:
                                is2019 = true;
                                foundRf = true;
                                times[ii] = fitPulse(hit);
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (foundRf) {
            rfHits.add(new RfHit(times));
        }
        event.put("RFHits", rfHits, RfHit.class, 1);
    }

    /**
     * Perform the fit to the RF pulse:
     */
    private double fitPulse(FADCGenericHit hit) {
        fitData.clear();
        final int adcSamples[] = hit.getData();
        // stores the number of peaks
        int iz = 0;
        int peakBin[] = {-999, -999};
        final int threshold = 300;
        double fitThresh[] = {-999, -999};
        double pedVal[] = {-999, -999};

        // Look for bins containing the peaks (2-3 peaks)
        for (int ii = 4; ii < adcSamples.length; ii++) {
            // After 2 peaks, stop looking for more
            if (iz == 2) {
                break;
            }
            if ((adcSamples[ii + 1] > 0) && (adcSamples[ii - 1] > 0) && (adcSamples[ii] > threshold) && ii > 8) {
                if ((adcSamples[ii] > adcSamples[ii + 1]) && (adcSamples[ii] >= adcSamples[ii - 1])) {

                    peakBin[iz] = ii;
                    iz++;
                }
            }
        }

        int jj = 0;
        // Choose peak closest to center of window (second peak, ik=1)
        final int ik = 1;
        pedVal[ik] = (adcSamples[peakBin[ik] - 6] + adcSamples[peakBin[ik] - 7] + adcSamples[peakBin[ik] - 8] + adcSamples[peakBin[ik] - 9]) / 4.0;
        fitThresh[ik] = (adcSamples[peakBin[ik]] + pedVal[ik]) / 3.0;

        // Initial values: we find/fit 3 points:
        double itime[] = {-999, -999, -999};
        double ifadc[] = {-999, -999, -999};

        // Find the points of the peak bin to peak bin-5
        for (int ll = 0; ll < 5; ll++) {
            if ((adcSamples[peakBin[ik] - 5 + ll]) > fitThresh[ik]) {
                // One point is below fit threshold and two points are above
                if (jj == 0 && (adcSamples[peakBin[ik] - 6 + ll] > pedVal[ik])) {
                    final int zz = fitData.size();
                    fitData.addPoint();
                    itime[zz] = peakBin[ik] - 6 + ll;
                    ifadc[zz] = adcSamples[peakBin[ik] - 6 + ll];
                    fitData.point(zz).coordinate(0).setValue(peakBin[ik] - 6 + ll);
                    fitData.point(zz).coordinate(1).setValue(adcSamples[peakBin[ik] - 6 + ll]);
                    fitData.point(zz).coordinate(1).setErrorMinus(NOISE);
                    fitData.point(zz).coordinate(1).setErrorPlus(NOISE);
                    jj++;
                }
                final int zz = fitData.size();
                fitData.addPoint();
                itime[zz] = peakBin[ik] - 5 + ll;
                ifadc[zz] = adcSamples[peakBin[ik] - 5 + ll];
                fitData.point(zz).coordinate(0).setValue(peakBin[ik] - 5 + ll);
                fitData.point(zz).coordinate(1).setValue(adcSamples[peakBin[ik] - 5 + ll]);
                fitData.point(zz).coordinate(1).setErrorMinus(NOISE);
                fitData.point(zz).coordinate(1).setErrorPlus(NOISE);

                jj++;
                if (jj == 3) {
                    break;
                }
            }
        }

        double islope = ((double) (ifadc[2] - ifadc[0])) / (itime[2] - itime[0]);
        double icept = ifadc[1] - islope * itime[1];
        // Initialize fit parameters:
        fitFunction.setParameter("intercept", icept);
        fitFunction.setParameter("slope", islope);

        // this used to be turned on somewhere else on every event, dunno if it still is:
        // Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);

        IFitResult fitResults = fitter.fit(fitData, fitFunction);

        // Read the time value at this location on the fit:
        double halfVal = (adcSamples[peakBin[1]] + pedVal[1]) / 2.0;

        return NSPERSAMPLE * (halfVal - fitResults.fittedParameter("intercept")) / fitResults.fittedParameter("slope");
    }
}
