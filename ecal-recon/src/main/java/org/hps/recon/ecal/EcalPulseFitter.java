package org.hps.recon.ecal;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;

// import java.io.FileWriter;
// import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;

/**
 * Fit ECal FADC Mode-1 waveform with a "3-pole" function for extraction of pulse integral and time. Called from
 * EcalRawConverter. Uses Ecal3PoleFunction. Limits are placed on pulse characteristics to avoid wasting time fitting
 * pulses near the edge of the window. Width parameter can be free or fixed. Pedestal parameter is initialized from
 * samples before threshold crossing.
 */
public class EcalPulseFitter {

    private EcalConditions ecalConditions = null;

    /**
     * If this is false, the width will be a free parameter in the fit:
     */
    public boolean fixShapeParameter = true;

    /**
     * don't bother fitting pulses with threshold crossing outside this sample range:
     */
    public int threshRange[] = {7, 20}; // (28 ns <--> 80 ns)

    /**
     * restrict fit's time0 parameter to this range: (units=samples)
     */
    public int t0limits[] = {1, 30};

    /**
     * fit sample range relative to threshold crossing
     */
    private final static int fitRange[] = {-10, 15};

    /**
     * sample range relative to threshold crossing used to initialize pedestal fit parameter:
     */
    private final static int pedRange[] = {-10, -5};

    /**
     * if this is positive, it will override individual widths and use one global width (units = samples)
     */
    public double globalThreePoleWidth = -999;

    /**
     * 442 channels' widths, measured from 2015 data (units = samples)
     */
    private final static double threePoleWidths[] = {2.44057, 2.40057, 2.53389, 2.32095, 2.44267, 2.43631, 2.40292,
            2.46911, 2.36032, 2.42132, 2.41473, 2.43864, 2.37710, 2.43278, 2.49126, 2.41739, 2.40560, 2.42042, 2.51278,
            2.46731, 2.42647, 2.35636, 2.55308, 2.47517, 2.38748, 2.48324, 2.53726, 2.54780, 2.50771, 2.45188, 2.35011,
            2.35110, 2.37575, 2.37649, 2.41037, 2.38817, 2.52422, 2.47389, 2.40672, 2.59415, 2.35626, 2.44359, 2.54118,
            2.42893, 2.43100, 2.32748, 2.34831, 2.30641, 2.40003, 2.38997, 2.41643, 2.45717, 2.44830, 2.48149, 2.44339,
            2.33572, 2.42276, 2.43731, 2.44205, 2.37290, 2.54975, 2.44517, 2.39132, 2.48821, 2.45687, 2.39768, 2.61674,
            2.52142, 2.46620, 2.46802, 2.54764, 2.45238, 2.48421, 2.44521, 2.47204, 2.50415, 2.47346, 2.51053, 2.44811,
            2.54895, 2.44839, 2.51419, 2.45706, 2.38596, 2.46743, 2.49971, 2.37904, 2.46478, 2.43015, 2.52088, 2.55846,
            2.50522, 2.30973, 2.41450, 2.40907, 2.31343, 2.48668, 2.46800, 2.43559, 2.42113, 2.47494, 2.36971, 2.44022,
            2.44193, 2.42661, 2.41918, 2.41102, 2.51565, 2.34875, 2.36750, 2.37355, 2.48763, 2.36659, 2.42424, 2.44321,
            2.46651, 2.52053, 2.46172, 2.41316, 2.41670, 2.41661, 2.53472, 2.49345, 2.46496, 2.40006, 2.41084, 2.44442,
            2.49113, 2.42821, 2.56839, 2.45630, 2.46344, 2.42585, 2.43730, 2.44339, 2.44315, 2.48069, 2.39892, 2.36987,
            2.40482, 2.40103, 2.36307, 2.46118, 2.47136, 2.39327, 2.46267, 2.30668, 2.45917, 2.46903, 2.51727, 2.27056,
            2.46365, 2.35228, 2.52188, 2.44052, 2.41457, 2.25591, 2.51210, 2.43585, 2.57410, 2.46877, 2.37902, 2.46975,
            2.45628, 2.44264, 2.36588, 2.48233, 2.46351, 2.37512, 2.44228, 2.52664, 2.51927, 2.37348, 2.52284, 2.52505,
            2.55755, 2.51522, 2.44848, 2.51430, 2.41387, 2.42311, 2.42880, 2.46546, 2.40000, 2.45992, 2.46841, 2.43101,
            2.22369, 2.43262, 2.41324, 2.45465, 2.40752, 2.50491, 2.45191, 2.40109, 2.49099, 2.37011, 2.70111, 2.46843,
            2.44336, 2.44675, 2.42308, 2.51800, 2.45660, 2.43406, 2.43297, 2.37133, 2.50718, 2.43035, 2.42818, 2.47709,
            2.40578, 2.39228, 2.47350, 2.51876, 2.39584, 2.44114, 2.49120, 2.48185, 2.59246, 2.41612, 2.42914, 2.51496,
            2.38785, 2.53339, 2.30056, 2.25807, 2.40500, 2.53300, 2.37439, 2.48418, 2.33764, 2.47934, 2.33080, 2.48834,
            2.48318, 2.42860, 2.28253, 2.44013, 2.38272, 2.23632, 2.37630, 2.34564, 2.34538, 2.44281, 2.33952, 2.33574,
            2.40939, 2.44992, 2.43986, 2.45022, 2.39336, 2.43253, 2.38136, 2.41941, 2.56685, 2.54684, 2.43060, 2.33258,
            2.43024, 2.48895, 2.51698, 2.38837, 2.45276, 2.38518, 2.35826, 2.38321, 2.31616, 2.46480, 2.53753, 2.28027,
            2.50727, 2.35369, 2.29179, 2.06776, 2.49429, 2.35639, 2.42628, 2.55657, 2.51859, 2.49579, 2.45617, 2.41293,
            2.47083, 2.51653, 2.47478, 2.46354, 2.48828, 2.34846, 2.34419, 2.43172, 2.46589, 2.45462, 2.52146, 2.35946,
            2.38810, 2.46027, 2.53848, 2.48102, 2.42701, 2.51750, 2.57911, 2.45136, 2.48329, 2.39329, 2.48209, 2.35948,
            2.49431, 2.36117, 2.26121, 2.40899, 2.51383, 2.41642, 2.52102, 2.39702, 2.43949, 2.40867, 2.38560, 2.46874,
            2.22937, 2.40741, 2.36007, 2.38784, 2.41052, 2.42673, 2.39476, 2.40239, 2.46902, 2.55278, 2.44661, 2.54734,
            2.41863, 2.42451, 2.40056, 2.44307, 2.35066, 2.46254, 2.43270, 2.50439, 2.49733, 2.55563, 2.48589, 2.45219,
            2.44774, 2.41116, 2.49081, 2.43893, 2.43731, 2.55774, 2.42404, 2.37911, 2.45140, 2.47196, 2.50533, 2.38292,
            2.47868, 2.48262, 2.39845, 2.42290, 2.54415, 2.43109, 2.54306, 2.49385, 2.35113, 2.43233, 2.50552, 2.49532,
            2.42667, 2.53772, 2.41398, 2.41968, 2.59536, 2.52077, 2.42026, 2.51269, 2.42584, 2.54930, 2.63547, 2.42869,
            2.43348, 2.42402, 2.38768, 2.41903, 2.50153, 2.45315, 2.45472, 2.41113, 2.44795, 2.50849, 2.41817, 2.44166,
            2.61143, 2.38567, 2.40737, 2.39843, 2.43711, 2.35324, 2.48315, 2.38388, 2.45665, 2.30220, 2.41293, 2.36640,
            2.38185, 2.40913, 2.49233, 2.46818, 2.51527, 2.54177, 2.37051, 2.37318, 2.38405, 2.53258, 2.58176, 2.53786,
            2.52385, 2.40308, 2.45279, 2.39370, 2.50056, 2.41076, 2.40789, 2.47444, 2.52857, 2.40741, 2.38524, 2.39403,
            2.42442, 2.56829, 2.56063, 2.58340, 2.42897, 2.58796, 2.56262, 2.53480, 2.43707, 2.38258, 2.46584, 2.40596,
            2.42590, 2.48281, 2.47800};

    // Stuff for debugging:
    // public int debug = 0;
    // public String fitFileName = null;
    // private FileWriter fitFileWriter = null;

    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(null);
    IFitFactory fitFactory = analysisFactory.createFitFactory();
    IFitter fitter = fitFactory.createFitter();
    IFunction fitFcn3Pole = new Ecal3PoleFunction();
    IDataPointSet fitData = aida.analysisFactory().createDataPointSetFactory(null).create("ADC DataPointSet", 2);

    private boolean debug;
    private IPlotter plotter1;
    private IPlotter plotter2;
    private IHistogram1D chi2Hist;

    private IHistogram1D pedestal_fit;
    private IHistogram1D pedestal_prefit;

    private IHistogram1D widthOffsetHist;
    private IHistogram1D widthOffsets[];
    private int skipHits = 100000;
    private int displayHits = 100;
    private int hitCount = 0;

    public void setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            this.plotter1 = aida.analysisFactory().createPlotterFactory().create();
            plotter1.show();
            this.plotter2 = aida.analysisFactory().createPlotterFactory().create();
            plotter2.createRegions(2, 2);
            chi2Hist = aida.histogram1D("chi2", 100, 0, 200);
            widthOffsetHist = aida.histogram1D("width offset", 200, -1, 1);
            widthOffsets = new IHistogram1D[442];
            for (int i = 0; i < 442; i++) {
                widthOffsets[i] = aida.histogram1D("width offset " + (i + 1), 200, -1, 1);
            }
            pedestal_prefit = aida.histogram1D("pedestal_prefit", 200, 0, 200);
            pedestal_fit = aida.histogram1D("pedestal_fit", 200, 0, 200);

            plotter2.region(0).plot(chi2Hist);
            plotter2.region(1).plot(widthOffsetHist);
            plotter2.region(2).plot(pedestal_prefit);
            plotter2.region(3).plot(pedestal_fit);
            plotter2.show();
        } else {
            if (this.plotter1 != null) {
                this.plotter1.destroyRegions();
                this.plotter1.hide();
                this.plotter1 = null;
            }
            if (this.plotter2 != null) {
                this.plotter2.destroyRegions();
                this.plotter2.hide();
                this.plotter2 = null;
            }
        }
    }

    /**
     * Perform and return "3-pole" fit of ECAL raw waveform.
     * 
     * @param hit the RawTrackerHit (Mode-1 FADC ECal readout) to be fit
     * @param threshCross the sample of threshold crossing, used to initialize fit parameters
     * @param maxADC the ADC at pulse maximum, used to initialize fit parameters
     * @return fit result, which can be null if fit fails or certain conditions are not met.
     */
    public IFitResult fitPulse(RawTrackerHit hit, int threshCross, double maxADC) {

        // if (debug>0) System.err.println("FITTING.....................................................");

        final short samples[] = hit.getADCValues();
        final double noise = findChannel(hit.getCellID()).getCalibration().getNoise();
        final int cid = ecalConditions.getChannelCollection().findGeometric(hit.getCellID()).getChannelId();

        // don't bother with pulses far from trigger:
        if (threshCross < threshRange[0] || threshCross > threshRange[1])
            return null;

        // calculate pedestal for initializing fit parameters:
        int nped = 0;
        double ped = 0;
        for (int ii = threshCross + pedRange[0]; ii < threshCross + pedRange[1]; ii++) {
            if (ii < 0)
                continue;
            if (ii >= samples.length)
                break;
            ped += samples[ii];
            nped++;
        }

        // don't bother trying to fit:
        if (nped == 0)
            return null;
        ped /= nped;

        // choose points to fit and get starting value for pulse integral:
        fitData.clear();
        int nFitPoints = 0;
        int iPeakADC = -1;
        int sumADC = 0;
        // int maxADC=0;
        for (int ii = threshCross + fitRange[0]; ii < threshCross + fitRange[1]; ii++) {
            if (ii < 0)
                continue;
            if (ii >= samples.length)
                break;
            // if (debug>0) System.err.print(ii+":"+samples[ii]+" ");
            // if (samples[ii] > maxADC) maxADC=samples[ii];
            if (iPeakADC < 0 && ii < samples.length - 1 && samples[ii + 1] < samples[ii]) {
                iPeakADC = ii;
            }
            sumADC += samples[ii];
            fitData.addPoint();
            fitData.point(nFitPoints).coordinate(0).setValue(ii);
            fitData.point(nFitPoints).coordinate(1).setValue(samples[ii]);
            fitData.point(nFitPoints).coordinate(1).setErrorMinus(noise);
            fitData.point(nFitPoints).coordinate(1).setErrorPlus(noise);
            nFitPoints++;
        }
        // if (debug>0) System.err.print("\n");

        // don't bother trying to fit:
        if (nFitPoints < 10)
            return null;
        if (maxADC < ped)
            return null;

        // if (debug>0) System.err.println("------- "+ped+" "+threshCross+" "+maxADC);

        final double pulseIntegral = sumADC - ped * nFitPoints;

        // initialize parameters:
        fitFcn3Pole.setParameter("pedestal", ped);
        fitFcn3Pole.setParameter("time0", (double) threshCross - 2);
        fitFcn3Pole.setParameter("integral", pulseIntegral > 0 ? pulseIntegral : 2);
        if (globalThreePoleWidth > 0)
            fitFcn3Pole.setParameter("width", globalThreePoleWidth);
        else
            fitFcn3Pole.setParameter("width", threePoleWidths[cid - 1]);

        // constrain parameters:
        // fitter.fitParameterSettings("time0").setBounds(1,30);
        fitter.fitParameterSettings("time0").setBounds(t0limits[0], t0limits[1]);
        fitter.fitParameterSettings("width").setBounds(0.1, 5);
        fitter.fitParameterSettings("integral").setBounds(0, 999999);
        if (fixShapeParameter)
            fitter.fitParameterSettings("width").setFixed(true);

        /*
         * if (debug>0) { System.err.println(String.format("A= %8.2f",fitFcn3Pole.parameter("integral")));
         * System.err.println(String.format("T= %8.2f",fitFcn3Pole.parameter("time0")*4));
         * System.err.println(String.format("P= %8.2f",fitFcn3Pole.parameter("pedestal")));
         * System.err.println(String.format("S= %8.2f",fitFcn3Pole.parameter("width"))); } else
         */

        // did this because something else was turning it back on every event
        // once that thing stops doing that, this can be removed.
        Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);
        if (debug && hitCount > skipHits) {
            System.out.println("pre fit");
            for (int i = 0; i < fitFcn3Pole.parameters().length; i++) {
                System.out.println(fitFcn3Pole.parameterNames()[i] + "\t" + fitFcn3Pole.parameters()[i]);
            }
        }
        IFitResult fitResult = fitter.fit(fitData, fitFcn3Pole);

        /*
         * if (debug>0) { writeFit(samples,fitResult,cid); final double P = fitResult.fittedParameter("pedestal"); final
         * double I = fitResult.fittedParameter("integral"); final double T = fitResult.fittedParameter("time0")*4; // 4
         * ns per sample final double S = fitResult.fittedParameter("width"); final double Q = fitResult.quality();
         * final double E[] = fitResult.errors(); System.err.println(";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;");
         * System.err.println(String.format("I = %8.2f %8.2f",I,sumADC-P*30));
         * System.err.println(String.format("T = %8.2f %8.2f",T,threshCross));
         * System.err.println(String.format("P = %8.2f %8.2f",P,ped));
         * System.err.println(String.format("S = %8.2f %8.2f",S,threePoleWidths[cid-1]));
         * System.err.println(String.format("M = %8.2f",maxADC-P)); System.err.println(String.format("Q = %8.2f",Q)); }
         */

        /*
         * // now fit just leading edge: IFitResult timeResult = null; if (doTimeFit) { for (int ii=fitData.size()-1;
         * ii>=0; ii--) { if (fitData.point(ii).coordinate(0).value() > iPeakADC+1) fitData.removePoint(ii); else break;
         * } fitter.fit(fitData,fitFcn3Pole); } IFitResult fitResults[] = {fitResult,timeResult};
         */
        if (debug && hitCount++ > skipHits) {
            double max = 0;
            double min = 99999;
            for (int i = 0; i < fitData.size(); i++) {
                double v = fitData.point(i).coordinate(1).value();
                if (v > max)
                    max = v;
                if (v < min)
                    min = v;
            }
            if (max - min < 100)
                return fitResult; // skip this hit. too small.
            if (hitCount % displayHits == 0) {
                plotter1.region(0).clear();
                plotter1.region(0).plot(fitData);
                plotter1.region(0).plot(fitResult.fittedFunction());

                System.out.println(hit.getCellID());
                System.out.println("after fit");
                for (int i = 0; i < fitFcn3Pole.parameters().length; i++) {

                    System.out.printf("%s\t%.3f\t(+-%.3f)\n", fitResult.fittedFunction().parameterNames()[i], fitResult
                            .fittedFunction().parameters()[i], Math.sqrt(fitResult.covMatrixElement(i, i)));
                }
            }
            double chi2 = getChi2(fitResult, fitData);
            int dof = fitResult.ndf();
            if (hitCount % displayHits == 0) {
                System.out.println("chi2\t" + chi2);
                System.out.println("dof\t" + dof);
                System.out.println("chi2 per dof\t" + chi2 / dof);
                System.out.println("fit quality: " + fitResult.quality());
            }

            chi2Hist.fill(chi2);
            widthOffsetHist.fill(fitResult.fittedParameter("width") - threePoleWidths[cid - 1]);
            widthOffsets[cid - 1].fill(fitResult.fittedParameter("width") - threePoleWidths[cid - 1]);
            pedestal_prefit.fill(ped);
            pedestal_fit.fill(fitResult.fittedFunction().parameters()[0]);

            if (hitCount % displayHits == 0) {
                System.out.println("Press enter to continue");
                try {
                    System.in.read();
                } catch (Exception e) {

                }
                plotter1.region(0).clear();
            }
        }
        return fitResult;
    }

    /**
     * calculate chi2 for debug
     * 
     * @param fitResult
     * @param fitData2
     * @return
     */
    private double getChi2(IFitResult fitResult, IDataPointSet fitData) {
        double chi2 = 0;
        for (int i = 0; i < fitData.size(); i++) {
            double yp = fitResult.fittedFunction().value(new double[] {fitData.point(i).coordinate(0).value()});
            double y = fitData.point(i).coordinate(1).value();
            double ey = fitData.point(i).coordinate(1).errorMinus();
            chi2 += Math.pow((y - yp) / ey, 2);
        }
        return chi2;
    }

    public void setDetector(Detector detector) {
        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }

    /*
     * public void writeFit(short samples[],IFitResult fit,final int cid) { if (fitFileName == null) return; if
     * (fitFileWriter == null) { try { fitFileWriter=new FileWriter(fitFileName); } catch (IOException ee) { throw new
     * RuntimeException("Error opening file "+fitFileName,ee); } } try { for (final short ss : samples)
     * fitFileWriter.write(String.format("%6d ",ss));
     * fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("integral")));
     * fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("time0")));
     * fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("pedestal")));
     * fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("width")));
     * fitFileWriter.write(String.format("%8.3f",fit.quality())); fitFileWriter.write(String.format("%4d",cid));
     * fitFileWriter.write("\n"); } catch (IOException ee) { throw new
     * RuntimeException("Error writing file "+fitFileName,ee); } } public double getSmartPedestal(short samples[]) {
     * double ped = 99999; final int nSamples = 6; for (int ii=0; ii<samples.length-nSamples; ii++) { double aped = 0;
     * for (int jj=ii; jj<ii+nSamples; jj++) aped += samples[jj]; aped /= nSamples; if (aped < ped) ped = aped; } return
     * ped; }
     */

    /*
     * public IFitResult fitLeadingEdge(RawTrackerHit hit,int threshCross) { IFunction
     * fitFcnLinear=functionFactory.createFunctionFromScript("fitFcnLinear",1,"p0+x[0]*p1","p0,p1","",null); final short
     * samples[] = hit.getADCValues(); final double noise = findChannel(hit.getCellID()).getCalibration().getNoise();
     * int imaxADC=0; for (int ii=threshCross; ii<samples.length-1; ii++) { if (ii<0) continue; if (ii>=samples.length)
     * break; if (samples[ii+1] < samples[ii]) { imaxADC=ii; break; } } fitData.clear(); int nFitPoints=0; for (int
     * ii=threshCross-1; ii<imaxADC-1; ii++) { fitData.addPoint(); fitData.point(nFitPoints).coordinate(0).setValue(ii);
     * fitData.point(nFitPoints).coordinate(1).setValue(samples[ii]);
     * fitData.point(nFitPoints).coordinate(1).setErrorMinus(noise);
     * fitData.point(nFitPoints).coordinate(1).setErrorPlus(noise); nFitPoints++; } if (nFitPoints<2) return null;
     * return fitter.fit(fitData,fitFcnLinear); }
     */

}
