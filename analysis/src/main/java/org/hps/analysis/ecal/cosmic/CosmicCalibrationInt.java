package org.hps.analysis.ecal.cosmic;

import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This code looks at raw FADC spectra, integrates cosmic signals, and outputs
 * a .root file containing the histogram spectra for each crystal.
 * 
 * @author holly
 *
 */
public class CosmicCalibrationInt extends Driver {

    // private ITree tree = null;
    // private IHistogramFactory histogramFactory = null;

    // Tune-able parameters:
    // optimum signal window, originally 35-55 (in units of 4ns)
    private int MINS = 35;// 50
    private int MAXS = 55;// 70
    // pedestal window for calculating an average pedestal
    private int MINP = 10;
    private int MAXP = 30;
    // number of bins used to calculate the pedestal
    private int NWIN = MAXP - MINP;
    // threshold in mV (2.5mV in 2015)
    private double THR = 3.5;// for 2016
    // 0 is strict requires 2 hits vertically, 1 is loose requiring 1 hit vertically
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

    protected void detectorChanged(Detector detector) {

        // Instantiate the tree and histogram factory
        aida.tree().cd("/");
        // Create histograms
        // IHistogram1D mipSigCut[][];
        // mipSigCut = new IHistogram1D[NX][NY];

        for (int jx = 0; jx < NX; jx++) {
            for (int jy = 0; jy < NY; jy++) {
                if (!ishole(jx, jy)) {
                    String titleID = String.format("Cry_%d_%d", jx, jy);
                    aida.histogram1D(titleID, 80, 5, 70);
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
            // crystals are stored in array of 0-45,0-9 with numbering starting at bottom left of Ecal (-23,-5)
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
                    final short samples[] = ihit.getADCValues();

                    for (int nTime = MINP; nTime < MAXP; nTime++) {
                        int adc = samples[nTime];
                        pedestal[ix][iy] += adc;
                    }// end loop over time samples

                    // loop over time samples, integrate adc values, use for signal and trigger
                    pulse[ix][iy] = 0;
                    for (int nTime = MINS; nTime < MAXS; nTime++) {
                        int adc = samples[nTime];
                        double peak = (adc - pedestal[ix][iy] / NWIN) * ADC2V;

                        if (peak > THR) {
                            trigger = 1;

                            // System.out.println("\t greater than thresh:\t"+peak+"\t"+THR);
                        }
                        pulse[ix][iy] += adc;
                    }// end loop over time samples

                    // subtract pedestal from pulse and plot
                    if (trigger == 1) {
                        signal[ix][iy] = (float) ((pulse[ix][iy] - pedestal[ix][iy]) * ADC2V);

                        // Crystal has passed threshold trigger cut, now must pass geometry cuts
                        int geomCut0 = 0;// 0 passes, ix+1
                        int geomCut1 = 0;// 0 passes, ix-1
                        int geomCut2 = 0;// 0 passes, iy+1
                        int geomCut3 = 0;// 0 passes, iy-1
                        int geomCut4 = 0;// 0 passes, if iy is 9,4, iy-2
                        int geomCut5 = 0;// 0 passes, if iy is 0,5, iy+2

                        // define geometry cuts-no other hit on left and right passing raw thresh
                        // loop over time samples, integrate adc values
                        if (!ishole(ix + 1, iy) && (ix + 1) < 46) {
                            Point cidxp1 = new Point(ix + 1, iy);
                            RawTrackerHit ihitxp1 = hitMap.get(cidxp1);
                            final short samplesxp1[] = ihitxp1.getADCValues();
                            pedestal[ix + 1][iy] = 0;
                            // calculate the pedestal for the crystal ix+1
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesxp1[nTime];
                                pedestal[ix + 1][iy] += ped;
                            }// end loop over time samples
                             // check if hit in adj crystal passes threshold
                            pulse[ix + 1][iy] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesxp1[nTime];
                                double peak = (adc - pedestal[ix + 1][iy] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut0 = 1;
                                    break;
                                }
                            }// end loop over time samples
                        }

                        if (!ishole(ix - 1, iy) && (ix - 1) > -1) {
                            Point cidxm1 = new Point(ix - 1, iy);
                            RawTrackerHit ihitxm1 = hitMap.get(cidxm1);
                            final short samplesxm1[] = ihitxm1.getADCValues();

                            pedestal[ix - 1][iy] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesxm1[nTime];
                                pedestal[ix - 1][iy] += ped;
                            }// end loop over time samples
                             // check if hit in adj crystal passes threshold
                            pulse[ix - 1][iy] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesxm1[nTime];
                                double peak = (adc - pedestal[ix - 1][iy] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut1 = 1;
                                    break;
                                }
                            }// end loop over time samples
                        }

                        if (!ishole(ix, iy + 1) && (iy + 1) < 10 && iy != 4) {
                            geomCut2 = 1;
                            Point cidyp1 = new Point(ix, iy + 1);
                            RawTrackerHit ihityp1 = hitMap.get(cidyp1);
                            final short samplesyp1[] = ihityp1.getADCValues();

                            pedestal[ix][iy + 1] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesyp1[nTime];
                                pedestal[ix][iy + 1] += ped;
                            }// end loop over time samples
                             // check if hit in adj crystal passes threshold
                            pulse[ix][iy + 1] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesyp1[nTime];
                                double peak = (adc - pedestal[ix][iy + 1] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut2 = 0;
                                    break;
                                }
                            }// end loop over time samples
                        }

                        if (!ishole(ix, iy - 1) && (iy - 1) > -1 && iy != 5) {
                            geomCut3 = 1;
                            Point cidym1 = new Point(ix, iy - 1);
                            RawTrackerHit ihitym1 = hitMap.get(cidym1);
                            final short samplesym1[] = ihitym1.getADCValues();

                            pedestal[ix][iy - 1] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesym1[nTime];
                                pedestal[ix][iy - 1] += ped;
                            }// end loop over time samples
                             // check if hit in adj crystal passes threshold
                            pulse[ix][iy - 1] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesym1[nTime];
                                double peak = (adc - pedestal[ix][iy - 1] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut3 = 0;
                                    break;
                                }

                            }// end loop over time samples
                        }

                        // ///////////////////Add in additional vert geom constraint for edges/////////
                        // if the crystal is along an edge, it must have a hit two above or two below
                        // since it does not have 1 above and 1 below
                        if (iy == 9 || iy == 4 || ishole(ix, iy + 1)) // look at iy-2
                        {
                            geomCut4 = 1;
                            Point cidym2 = new Point(ix, iy - 2);
                            RawTrackerHit ihitym2 = hitMap.get(cidym2);
                            final short samplesym2[] = ihitym2.getADCValues();

                            pedestal[ix][iy - 2] = 0;
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesym2[nTime];
                                pedestal[ix][iy - 2] += ped;
                            }// end loop over time samples
                            pulse[ix][iy - 2] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesym2[nTime];
                                double peak = (adc - pedestal[ix][iy - 2] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut4 = 0;
                                    break;
                                }

                            }// end loop over time samples
                        } // end for iy-2
                        if (iy == 0 || iy == 5 || ishole(ix, iy - 1)) // look at iy+2
                        {
                            geomCut5 = 1;
                            pedestal[ix][iy + 2] = 0;
                            Point cidyp2 = new Point(ix, iy + 2);
                            RawTrackerHit ihityp2 = hitMap.get(cidyp2);
                            final short samplesyp2[] = ihityp2.getADCValues();
                            for (int nTime = MINP; nTime < MAXP; nTime++) {
                                int ped = samplesyp2[nTime];
                                pedestal[ix][iy + 2] += ped;
                            }// end loop over time samples
                            pulse[ix][iy + 2] = 0;
                            for (int nTime = MINS; nTime < MAXS; nTime++) {
                                int adc = samplesyp2[nTime];
                                double peak = (adc - pedestal[ix][iy + 2] / NWIN) * ADC2V;
                                if (peak > THR) {
                                    geomCut5 = 0;
                                    break;
                                }
                            }// end loop over time samples
                        } // end for iy+2

                        // ///////////////////////////////////////////////////////////////////////////
                        // System.out.println("print of geoCuts:\t"+geomCut0+","+geomCut1+","+geomCut2+","+geomCut3+","+geomCut4+","+geomCut5);
                        // System.out.println("cut type:\t"+cutType);

                        if (cutType == 0) // strict geometry cut
                        {
                            if (geomCut0 == 0 && geomCut1 == 0 && geomCut2 == 0 && geomCut3 == 0 && geomCut4 == 0
                                    && geomCut5 == 0) {
                                String titleID = String.format("Cry_%d_%d", ix, iy);
                                aida.histogram1D(titleID).fill(signal[ix][iy]);
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

            }// end loop over x

        }// end loop over y

    }// end process event

    boolean ishole(int x, int y) {
        return (x > 12 && x < 22 && y > 3 && y < 6);
    }
}
