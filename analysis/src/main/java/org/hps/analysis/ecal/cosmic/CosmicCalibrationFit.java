package org.hps.analysis.ecal.cosmic;

import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.EcalCosmicPulseFitter;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import hep.aida.IFitResult;

/**
 * This code looks at raw FADC spectra and applies geometric cuts to select
 * vertical cosmic events. Signals are then fitted with a "3-pole" function to
 * extract the integral. In order to cross-check the results, the integral is
 * evaluated also summing the FADC samples. The output is a .root file
 * containing the histogram spectra for each crystal, both for the fit and the
 * sum, the 2-D correlation between the two and the histograms of pedestal
 * values for each crystal.
 *
 * @author LucaMarsicano
 */

public class CosmicCalibrationFit extends Driver {

    // private ITree tree = null;
    // private IHistogramFactory histogramFactory = null;

    //

    private EcalCosmicPulseFitter pulseFitter = new EcalCosmicPulseFitter();
    private EcalConditions ecalConditions = null;

    // Tune-able parameters:
    // optimum signal window, 35-55 (in units of 4ns) (35-55 in 2016)
    private int MINS = 25;
    private int MAXS = 45;
    // pedestal window for calculating an average pedestal (10-30 in 2016)
    private int MINP = 0;
    private int MAXP = 20;
    // number of bins used to calculate the pedestal
    private int NWIN = MAXP - MINP;
    // threshold in mV (2.5mV in 2015, 3.5mV in 2016)
    private double THR = 3.0;// for 2016
    // 0 is strict requires 2 hits vertically, 1 is loose requiring 1 hit vertically
    // (fit available only with strict geo
    // cut)
    private int cutType = 0;

    // ///////////////////
    // Don't change these:
    // ///////////////////
    // puts the crystals in an array:
    private int NX = 46;
    private int NY = 10;
    // number of 4ns time samples in the measured spectrum
    private int NSAMP = 100;
    // conversion from FADC to mV
    private double ADC2V = 0.25;

    AIDA aida = AIDA.defaultInstance();

    public void setMinSampleSignal(int N) {
        this.MINS = N;
    }

    public void setMaxSampleSignal(int N) {
        this.MAXS = N;
    }

    public void setMinSamplePedestal(int N) {
        this.MINP = N;
        this.NWIN = (MAXP - MINP);
    }

    public void setMaxSamplePedestal(int N) {
        this.MAXP = N;
        this.NWIN = (MAXP - MINP);
    }

    public void setThreshold(double thr) {
        this.THR = thr;
    }

    protected void detectorChanged(Detector detector) {

        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        pulseFitter.setDetector(detector);

        // Instantiate the tree and histogram factory
        aida.tree().cd("/");
        // Create histograms

        String titleID_CorrTot = "CorrTot";
        aida.histogram2D(titleID_CorrTot, 150, 0, 150, 150, 0, 150);

        // String titleID_PedDiffTot = "PedDiffTot";
        // aida.histogram1D( titleID_PedDiffTot,150,0,150);

        for (int jx = 0; jx < NX; jx++) {
            for (int jy = 0; jy < NY; jy++) {
                if (!ishole(jx, jy)) {
                    String titleID = String.format("Cry_%d_%d", jx, jy);
                    aida.histogram1D(titleID, 150, 0, 150);

                    String titleID_Fit = String.format("Cry_%d_%d_Fit", jx, jy);
                    aida.histogram1D(titleID_Fit, 150, 0, 150);

                    String titleID_Corr = String.format("Cry_%d_%d_Corr", jx, jy);
                    aida.histogram2D(titleID_Corr, 150, 0, 150, 150, 0, 150);

                    String titleID_Ped = String.format("Cry_%d_%d_Ped", jx, jy);
                    aida.histogram1D(titleID_Ped, 400, 0, 60);

                    String titleID_PedF = String.format("Cry_%d_%d_PedF", jx, jy);
                    aida.histogram1D(titleID_PedF, 400, 0, 60);

                } // end !ishole
            } // end jy iteration
        } // end jx iteration
    }

    // read in channels, store in arrays
    public void process(EventHeader event) {
        if (!event.hasCollection(RawTrackerHit.class, "EcalReadoutHits"))
            throw new Driver.NextEventException();
        List<RawTrackerHit> hitList = event.get(RawTrackerHit.class, "EcalReadoutHits");

        // Put all raw hits into a hash map with ix,iy array position
        Map<Point, RawTrackerHit> hitMap = new LinkedHashMap<Point, RawTrackerHit>();

        // Loop over the raw hits in the event
        for (RawTrackerHit hit : hitList) {
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            // crystals are stored in array of 0-45,0-9 with numbering starting at bottom
            // left of Ecal (-23,-5)
            int xx = ix + 23;
            if (xx > 23)
                xx -= 1;
            int yy = iy + 5;
            if (yy > 5)
                yy -= 1;

            Point hitIndex = new Point(xx, yy);
            hitMap.put(hitIndex, hit);
        }

        int pedestal[][] = new int[NX][NY];
        int pulse[][] = new int[NX][NY];
        float signal[][] = new float[NX][NY];
        int time0 = 0;

        // loop over crystal y
        for (int iy = 0; iy < NY; iy++) {
            // loop over crystal x
            for (int ix = 0; ix < NX; ix++) {
                int trigger = 0;

                if (!ishole(ix, iy)) {

                    // loop over time samples, integrate adc values, use for pedestal
                    pedestal[ix][iy] = 0;

                    Point cid = new Point(ix, iy);
                    RawTrackerHit ihit = hitMap.get(cid);

                    // This may happen if - for any reason - a FADC channel is missing data.
                    // In principle, cosmics data should be taken with no 0-suppression.
                    if (ihit == null) {
                        pulse[ix][iy] = 0;
                        signal[ix][iy] = 0;
                        continue;
                    }

                    final short samples[] = ihit.getADCValues();

                    for (int nTime = MINP; nTime < MAXP; nTime++) {
                        int adc = samples[nTime];
                        pedestal[ix][iy] += adc;
                    } // end loop over time samples

                    // loop over time samples, integrate adc values, use for signal and trigger
                    pulse[ix][iy] = 0;
                    for (int nTime = MINS; nTime < MAXS; nTime++) {
                        int adc = samples[nTime];
                        double peak = (adc - pedestal[ix][iy] / NWIN) * ADC2V;

                        if (peak > THR) {
                            trigger = trigger + 1;
                            time0 = nTime;
                            // System.out.println("\t greater than thresh:\t"+peak+"\t"+THR);
                        }
                        pulse[ix][iy] += adc;
                    } // end loop over time samples

                    // subtract pedestal from pulse and plot
                    if (trigger >= 1) {
                        signal[ix][iy] = (float) ((pulse[ix][iy] - pedestal[ix][iy]) * ADC2V);

                        // Crystal has passed threshold trigger cut, now must pass geometry cuts
                        int geomCut0 = 0;// 0 passes, ix+1
                        int geomCut1 = 0;// 0 passes, ix-1
                        int geomCut2 = 1;// 0 passes, iy+1
                        int geomCut3 = 1;// 0 passes, iy-1
                        int geomCut4 = 1;// 0 passes, if iy is 9,4, iy-2
                        int geomCut5 = 1;// 0 passes, if iy is 0,5, iy+2

                        // define geometry cuts-no other hit on left and right passing raw thresh
                        // loop over time samples, integrate adc values
                        if (!ishole(ix + 1, iy) && (ix + 1) < 46) {
                            Point cidxp1 = new Point(ix + 1, iy);
                            RawTrackerHit ihitxp1 = hitMap.get(cidxp1);

                            // This may happen if - for any reason - a FADC channel is missing data.
                            // In principle, cosmics data should be taken with no 0-suppression.
                            if (ihitxp1 == null) {
                                pulse[ix + 1][iy] = 0;
                                signal[ix + 1][iy] = 0;
                                continue;
                            }

                            final short samplesxp1[] = ihitxp1.getADCValues();
                            pedestal[ix + 1][iy] = 0;
                            // calculate the pedestal for the crystal ix+1
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesxp1[nTime];
                                pedestal[ix + 1][iy] += ped;
                            } // end loop over time samples
                              // check if hit in adj crystal passes threshold
                            pulse[ix + 1][iy] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesxp1[nTime];
                                double peak = (adc - pedestal[ix + 1][iy] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut0 = 1;
                                    break;
                                }
                            } // end loop over time samples
                        }

                        if (!ishole(ix - 1, iy) && (ix - 1) > -1) {
                            Point cidxm1 = new Point(ix - 1, iy);
                            RawTrackerHit ihitxm1 = hitMap.get(cidxm1);
                            // This may happen if - for any reason - a FADC channel is missing data.
                            // In principle, cosmics data should be taken with no 0-suppression.
                            if (ihitxm1 == null) {
                                pulse[ix - 1][iy] = 0;
                                signal[ix - 1][iy] = 0;
                                continue;
                            }
                            final short samplesxm1[] = ihitxm1.getADCValues();

                            pedestal[ix - 1][iy] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesxm1[nTime];
                                pedestal[ix - 1][iy] += ped;
                            } // end loop over time samples
                              // check if hit in adj crystal passes threshold
                            pulse[ix - 1][iy] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesxm1[nTime];
                                double peak = (adc - pedestal[ix - 1][iy] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut1 = 1;
                                    break;
                                }
                            } // end loop over time samples
                        }

                        if (!ishole(ix, iy + 1) && (iy + 1) < 10 && iy != 4) {
                            geomCut2 = 0;
                            Point cidyp1 = new Point(ix, iy + 1);
                            RawTrackerHit ihityp1 = hitMap.get(cidyp1);
                            // This may happen if - for any reason - a FADC channel is missing data.
                            // In principle, cosmics data should be taken with no 0-suppression.
                            if (ihityp1 == null) {
                                pulse[ix][iy + 1] = 0;
                                signal[ix][iy + 1] = 0;
                                continue;
                            }
                            final short samplesyp1[] = ihityp1.getADCValues();

                            pedestal[ix][iy + 1] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesyp1[nTime];
                                pedestal[ix][iy + 1] += ped;
                            } // end loop over time samples
                              // check if hit in adj crystal passes threshold
                            pulse[ix][iy + 1] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesyp1[nTime];
                                double peak = (adc - pedestal[ix][iy + 1] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut2 = geomCut2 + 1;
                                    // break;
                                }
                            } // end loop over time samples
                        }

                        if (!ishole(ix, iy - 1) && (iy - 1) > -1 && iy != 5) {
                            geomCut3 = 0;
                            Point cidym1 = new Point(ix, iy - 1);
                            RawTrackerHit ihitym1 = hitMap.get(cidym1);
                            if (ihitym1 == null) {
                                pulse[ix][iy - 1] = 0;
                                signal[ix][iy - 1] = 0;
                                continue;
                            }
                            final short samplesym1[] = ihitym1.getADCValues();

                            pedestal[ix][iy - 1] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesym1[nTime];
                                pedestal[ix][iy - 1] += ped;
                            } // end loop over time samples
                              // check if hit in adj crystal passes threshold
                            pulse[ix][iy - 1] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesym1[nTime];
                                double peak = (adc - pedestal[ix][iy - 1] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut3 = geomCut3 + 1;
                                    // break;
                                }

                            } // end loop over time samples
                        }

                        // ///////////////////Add in additional vert geom constraint for edges/////////
                        // if the crystal is along an edge, it must have a hit two above or two below
                        // since it does not have 1 above and 1 below
                        if (iy == 9 || iy == 4 || ishole(ix, iy + 1)) // look at iy-2
                        {
                            geomCut4 = 0;
                            Point cidym2 = new Point(ix, iy - 2);
                            RawTrackerHit ihitym2 = hitMap.get(cidym2);
                            if (ihitym2 == null) {
                                pulse[ix][iy - 2] = 0;
                                signal[ix][iy - 2] = 0;
                                // continue;
                            }
                            final short samplesym2[] = ihitym2.getADCValues();

                            pedestal[ix][iy - 2] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesym2[nTime];
                                pedestal[ix][iy - 2] += ped;
                            } // end loop over time samples
                            pulse[ix][iy - 2] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesym2[nTime];
                                double peak = (adc - pedestal[ix][iy - 2] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut4 = geomCut4 + 1;
                                    // break;
                                }

                            } // end loop over time samples
                        } // end for iy-2
                        if (iy == 0 || iy == 5 || ishole(ix, iy - 1)) // look at iy+2
                        {
                            geomCut5 = 0;
                            pedestal[ix][iy + 2] = 0;
                            Point cidyp2 = new Point(ix, iy + 2);
                            RawTrackerHit ihityp2 = hitMap.get(cidyp2);
                            if (ihityp2 == null) {
                                pulse[ix][iy + 2] = 0;
                                signal[ix][iy + 2] = 0;
                                // continue;
                            }
                            final short samplesyp2[] = ihityp2.getADCValues();
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesyp2[nTime];
                                pedestal[ix][iy + 2] += ped;
                            } // end loop over time samples
                            pulse[ix][iy + 2] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesyp2[nTime];
                                double peak = (adc - pedestal[ix][iy + 2] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut5 = geomCut5 + 1;
                                    // break;
                                }
                            } // end loop over time samples
                        } // end for iy+2

                        // ///////////////////////////////////////////////////////////////////////////
                        // System.out.println("print of
                        // geoCuts:\t"+geomCut0+","+geomCut1+","+geomCut2+","+geomCut3+","+geomCut4+","+geomCut5);
                        // System.out.println("cut type:\t"+cutType);

                        if (cutType == 0) // strict geometry cut
                        {
                            if (geomCut0 == 0 && geomCut1 == 0 && geomCut2 >= 1 && geomCut3 >= 1 && geomCut4 >= 1
                                    && geomCut5 >= 1) {
                                // double rangeMin = MINS-5;
                                // double rangeMax = MAXS+5;
                                double sumADC = 0;
                                double minADC = 0;
                                double maxADC = 10000;
                                double fitQuality = -1;

                                // Fill histograms
                                String titleID = String.format("Cry_%d_%d", ix, iy);
                                aida.histogram1D(titleID).fill(signal[ix][iy]);

                                String titleID_Ped = String.format("Cry_%d_%d_Ped", ix, iy);
                                aida.histogram1D(titleID_Ped).fill(pedestal[ix][iy] / NWIN * ADC2V);

                                // System.out.println("Fitting:\t"+ix+"\t"+iy);
                                // System.out.println(findChannel(ihit.getCellID()).getCalibration().getNoise()
                                // );

                                // Fit the pulse
                                IFitResult fitResult = pulseFitter.fitCosmicPulse(ihit, time0, maxADC);

                                // System.out.println("Fitting:\t"+ix+"\t"+iy);

                                if (fitResult != null) {
                                    fitQuality = fitResult.quality();

                                    if (fitQuality > 0) {
                                        // pulseTime = fitResult.fittedParameter("time0")*nsPerSample;
                                        // Getting fit results
                                        sumADC = fitResult.fittedParameter("integral");
                                        minADC = fitResult.fittedParameter("pedestal");

                                        String titleID_CorrTot = "CorrTot";
                                        aida.histogram2D(titleID_CorrTot).fill(signal[ix][iy], sumADC * ADC2V);

                                        // String titleID_PedDiffTot = "PedDiffTot";
                                        // aida.histogram1D(titleID_PedDiffTot).fill(minADC*ADC2V-pedestal[ix][iy]/NWIN*ADC2V);

                                        String titleID_Fit = String.format("Cry_%d_%d_Fit", ix, iy);
                                        aida.histogram1D(titleID_Fit).fill(sumADC * ADC2V);

                                        String titleID_Corr = String.format("Cry_%d_%d_Corr", ix, iy);
                                        aida.histogram2D(titleID_Corr).fill(signal[ix][iy], sumADC * ADC2V);

                                        String titleID_PedF = String.format("Cry_%d_%d_PedF", ix, iy);
                                        aida.histogram1D(titleID_PedF).fill(minADC * ADC2V);

                                    }
                                }

                            }
                        } else if (cutType == 1) // loose geometry cut
                        {
                            if (geomCut0 == 0 && geomCut1 == 0) {
                                if (geomCut2 == 0 || geomCut3 == 0) {
                                    String titleID = String.format("Cry_%d_%d", ix, iy);
                                    aida.histogram1D(titleID).fill(signal[ix][iy]);
                                }
                            }
                        }
                    } // end of trigger==1

                } // end !ishole

            } // end loop over x

        } // end loop over y

    }// end process event

    boolean ishole(int x, int y) {
        return (x > 12 && x < 22 && y > 3 && y < 6);
    }
}
