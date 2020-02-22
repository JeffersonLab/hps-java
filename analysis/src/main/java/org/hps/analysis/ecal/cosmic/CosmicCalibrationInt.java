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
 * This code looks at raw FADC spectra, integrates cosmic signals, and outputs a
 * .root file containing the histogram spectra for each crystal.
 * 
 * @author holly
 *
 */
public class CosmicCalibrationInt extends Driver {

    // private ITree tree = null;
    // private IHistogramFactory histogramFactory = null;

    // Tune-able parameters:
    // optimum signal window, originally 35-55 (in units of 4ns)
    private int MINS = 25;// 50
    private int MAXS = 45;// 70
    // pedestal window for calculating an average pedestal (originally 10-30)
    private int MINP = 0;
    private int MAXP = 20;
    // number of bins used to calculate the pedestal
    private int NWIN = MAXP - MINP;
    // threshold in mV (2.5mV in 2015)
    private double THR = 3.00;// for 2016
    private double THRLow = 2.00;// for 2016
    // 0 is strict requires 2 hits vertically, 1 is loose requiring 1 hit vertically
    private int cutType = 0;

    private int pedestal[][];
    private int pulse[][];
    private int trigger[][];
    private int triggerLow[][];
    private float signal[][];
    private Map<Point, RawTrackerHit> hitMap;
    // ///////////////////
    // Don't change these:
    // ///////////////////
    // puts the crystals in an array:
    private int NX = 46;
    private int NY = 10;
    // number of 4ns time samples in the measured spectrum
    private int NSAMP = 100;
    // conversion from FADC to mV
    private double ADC2V = 0.244;

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

        pedestal = new int[NX][NY];
        pulse = new int[NX][NY];
        trigger = new int[NX][NY];
        triggerLow = new int[NX][NY];
        signal = new float[NX][NY];
        hitMap = new LinkedHashMap<Point, RawTrackerHit>();

        // Instantiate the tree and histogram factory
        aida.tree().cd("/");
        // Create histograms
        // IHistogram1D mipSigCut[][];
        // mipSigCut = new IHistogram1D[NX][NY];

        for (int jx = 0; jx < NX; jx++) {
            for (int jy = 0; jy < NY; jy++) {
                if (!ishole(jx, jy)) {
                    String titleID;
                    //create different histograms for different selections

                    titleID = String.format("Cry_sel1_%d_%d", jx, jy);
                    aida.histogram1D(titleID, 80, 5, 70);

                    titleID = String.format("Cry_sel2_%d_%d", jx, jy);
                    aida.histogram1D(titleID, 80, 5, 70);

                    titleID = String.format("Cry_sel3_%d_%d", jx, jy);
                    aida.histogram1D(titleID, 80, 5, 70);

                    titleID = String.format("Cry_sel4_%d_%d", jx, jy);
                    aida.histogram1D(titleID, 80, 5, 70);

                    titleID = String.format("Cry_sel5_%d_%d", jx, jy);
                    aida.histogram1D(titleID, 80, 5, 70);

                } // end !ishole
            } // end jy iteration
        } // end jx iteration
    }

    // read in channels, store in arrays
    public void process(EventHeader event) {
        if (!event.hasCollection(RawTrackerHit.class, "EcalReadoutHits")) throw new Driver.NextEventException();
        List<RawTrackerHit> hitList = event.get(RawTrackerHit.class, "EcalReadoutHits");

        // Put all raw hits into a hash map with ix,iy array position
        hitMap.clear();

        // Loop over the raw hits in the event
        for (RawTrackerHit hit : hitList) {
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            // crystals are stored in array of 0-45,0-9 with numbering starting at bottom
            // left of Ecal (-23,-5)
            int xx = ix + 23;
            if (xx > 23) xx -= 1;
            int yy = iy + 5;
            if (yy > 5) yy -= 1;

            Point hitIndex = new Point(xx, yy);
            hitMap.put(hitIndex, hit);

        }

        // Compute pedestal,pulse,signal and trigger for all channels
        // loop over crystal y
        for (int iy = 0; iy < NY; iy++) {
            // loop over crystal x
            for (int ix = 0; ix < NX; ix++) {
                trigger[ix][iy] = 0;
                triggerLow[ix][iy] = 0;
                pedestal[ix][iy] = 0;
                pulse[ix][iy] = 0;
                signal[ix][iy] = 0;
                if (!ishole(ix, iy)) {
                    Point cid = new Point(ix, iy);
                    RawTrackerHit ihit = hitMap.get(cid);

                    /*
                     * This may happen if - for any reason - a FADC channel is missing data. In
                     * principle, cosmics data should be taken with no 0-suppression.
                     */
                    if (ihit == null) {
                        continue;
                    }
                    final short samples[] = ihit.getADCValues();

                    // loop over time samples, integrate adc values, use for pedestal
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
                            trigger[ix][iy] = trigger[ix][iy] + 1;
                        }
                        if (peak > THRLow) {
                            triggerLow[ix][iy] = triggerLow[ix][iy] + 1;
                        }
                        pulse[ix][iy] += adc;
                    } // end loop over time samples
                    signal[ix][iy] = (float) ((pulse[ix][iy] - pedestal[ix][iy]) * ADC2V);
                }
            }
        }

        // loop over crystal y
        for (int iy = 0; iy < NY; iy++) {
            // loop over crystal x
            for (int ix = 0; ix < NX; ix++) {
                if (!ishole(ix, iy)) {

                    if (selectCrystal1(ix, iy) && (signal[ix][iy] > 0)) {
                        String titleID = String.format("Cry_sel1_%d_%d", ix, iy);
                        aida.histogram1D(titleID).fill(signal[ix][iy]);
                    }

                    if (selectCrystal2(ix, iy) && (signal[ix][iy] > 0)) {
                        String titleID = String.format("Cry_sel2_%d_%d", ix, iy);
                        aida.histogram1D(titleID).fill(signal[ix][iy]);
                    }

                    if (selectCrystal3(ix, iy) && (signal[ix][iy] > 0)) {
                        String titleID = String.format("Cry_sel3_%d_%d", ix, iy);
                        aida.histogram1D(titleID).fill(signal[ix][iy]);
                    }

                    if (selectCrystal4(ix, iy) && (signal[ix][iy] > 0)) {
                        String titleID = String.format("Cry_sel4_%d_%d", ix, iy);
                        aida.histogram1D(titleID).fill(signal[ix][iy]);
                    } /*
                      
                      if (selectCrystal5(ix, iy) && (signal[ix][iy] > 0)) {
                         String titleID = String.format("Cry_sel5_%d_%d", ix, iy);
                         aida.histogram1D(titleID).fill(signal[ix][iy]);
                      }*/
                }
            }
        }
    }

    // Original selection:
    // no x+1, x-1
    // select y+1, y-1
    // select iy + 2 if y==0 or y==5 or isHole(ix,iy-1)
    // select iy -2 if y==4 or y==9 or isHole(ix,iy+1)

    boolean selectCrystal1(int x, int y) {
        boolean ret = true;

        //dead (-15,-2)--->(8,3)
        if ((x == 8) && (y == 3)) {
            return false;
        }
        //the one below the dead: require y-1 and y+2, exclude (x-1,y) and (x+1,y)
        else if ((x == 8) && (y == 2)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][1]==0) return false; //require the one below
            if (trigger[x][4]==0) return false; //require the one at y+2
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y);
            return ret;
        }
        //the one on top of the dead: require y-2 and y-3, exclude (x-1,y-1) and (x+1,y-1) and (x-1,y) and (x+1,y)
        else if ((x == 8) && (y == 4)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-2]==0) return false; //require the one at y-2
            if (trigger[x][y-3]==0) return false; //require the one at y-3
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y);
            if (trigger[x+1][y-1]>0) return false; //exclude (x+1,y-1)
            if (trigger[x-1][y-1]>0) return false; //exclude (x-1,y-1);     
            return ret;
        }
        
        //(5,-2)-->(27,3) has a very low gain
        //the one on bottom
        else if ((x == 27) && (y == 2)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y+2]==0) return false; //require the one at y+2
            if (trigger[x][y-1]==0) return false; //require the one at y-1
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y);
            if (trigger[x+1][y+1]>0) return false; //exclude (x+1,y+1)
            if (trigger[x-1][y+1]>0) return false; //exclude (x-1,y+1);
            return ret;
        }
        //the one on top
        else if ((x == 27) && (y == 4)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-2]==0) return false; //require the one at y-2
            if (trigger[x][y-3]==0) return false; //require the one at y-3
            if (trigger[x+1][y-1]>0) return false; //exclude (x+1,y-1)
            if (trigger[x-1][y-1]>0) return false; //exclude (x-1,y-1);
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y-1)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y-1);
            return ret;
        }
        
        //(7,-3) --> (29,2) has a low amplitude
        //the one on toptop - require y=3 and y=1
        else if ((x == 29) && (y == 4)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-1]==0) return false; //require the one on y-1
            if (trigger[x][y-3]==0) return false; //require the one on y-3
            if (trigger[x+1][y]>0) return false; //exclude the one on RIGHT
            if (trigger[x-1][y]>0) return false; //exclude the one on LEFT
            if (trigger[x+1][y-1]>0) return false; //exclude the one on RIGHT
            if (trigger[x-1][y-1]>0) return false; //exclude the one on LEFT
            return ret;
        }
        //the one on top
        else if ((x == 29) && (y == 3)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y+1]==0) return false; //require the one on TOP
            if (trigger[x][y-2]==0) return false; //require the one on y-2
            if (trigger[x+1][y]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y]>0) return false; //require the one on LEFT
            if (trigger[x+1][y-1]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y-1]>0) return false; //require the one on LEFT
            return ret;
        }
        //the one on bottom
        else if ((x == 29) && (y == 1)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-1]==0) return false; //require the one on BOTTOM
            if (trigger[x][y+2]==0) return false; //require the one on TOP
            if (trigger[x+1][y]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y]>0) return false; //require the one on LEFT
            if (trigger[x+1][y+1]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y+1]>0) return false; //require the one on LEFT
            return ret;
        }
        //the one on bottom-bottom
        else if ((x == 29) && (y == 0)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y+1]==0) return false; //require the one on BOTTOM
            if (trigger[x][y+3]==0) return false; //require the one on TOP 
            if (trigger[x+1][y]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y]>0) return false; //require the one on LEFT
            if (trigger[x+1][y+1]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y+1]>0) return false; //require the one on LEFT
            return ret;
        }

        else {
            if (x > 0 && !ishole(x - 1, y)) {
                if (trigger[x - 1][y] > 0) return false;
            }
            if (x < (NX - 1) && !ishole(x + 1, y)) {
                if (trigger[x + 1][y] > 0) return false;
            }

            if ((y > 0) && !ishole(x, y - 1) && y != 5) {
                if (trigger[x][y - 1] == 0) return false;
            }
            if (y < (NY - 1) && !ishole(x, y + 1) && y != 4) {
                if (trigger[x][y + 1] == 0) return false;
            }
            if ((y == 0) || (y == 5) || ishole(x, y - 1)) {
                if (trigger[x][y + 2] == 0) return false;
            }
            if ((y == 4) || (y == 9) || ishole(x, y + 1)) {
                if (trigger[x][y - 2] == 0) return false;
            }

            if (trigger[x][y] == 0) {
                return false;
            }
        }
        return ret;
    }

    // Original selection:
    // no x+1, x-1
    // select y+1, y-1
    // select iy + 2 if y==0 or y==5 or isHole(ix,iy-1)
    // select iy -2 if y==4 or y==9 or isHole(ix,iy+1)

    boolean selectCrystal2(int x, int y) {
        boolean ret = true;

        //dead (-15,-2)--->(8,3)
        if ((x == 8) && (y == 3)) {
            return false;
        }
        //the one below the dead: require y-1 and y+2, exclude (x-1,y) and (x+1,y)
        else if ((x == 8) && (y == 2)) {
            if (triggerLow[x][y] == 0) return false; //require that crystal
            if (trigger[x][1]==0) return false; //require the one below
            if (trigger[x][4]==0) return false; //require the one at y+2
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y);
            return ret;
        }
        //the one on top of the dead: require y-2 and y-3, exclude (x-1,y-1) and (x+1,y-1) and (x-1,y) and (x+1,y)
        else if ((x == 8) && (y == 4)) {
            if (triggerLow[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-2]==0) return false; //require the one at y-2
            if (trigger[x][y-3]==0) return false; //require the one at y-3
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y);
            if (trigger[x+1][y-1]>0) return false; //exclude (x+1,y-1)
            if (trigger[x-1][y-1]>0) return false; //exclude (x-1,y-1);     
            return ret;
        }
        
        //(5,-2)-->(27,3) has a very low gain
        //the one on bottom
        else if ((x == 27) && (y == 2)) {
            if (triggerLow[x][y] == 0) return false; //require that crystal
            if (trigger[x][y+2]==0) return false; //require the one at y+2
            if (trigger[x][y-1]==0) return false; //require the one at y-1
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y);
            if (trigger[x+1][y+1]>0) return false; //exclude (x+1,y+1)
            if (trigger[x-1][y+1]>0) return false; //exclude (x-1,y+1);
            return ret;
        }
        //the one on top
        else if ((x == 27) && (y == 4)) {
            if (triggerLow[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-2]==0) return false; //require the one at y-2
            if (trigger[x][y-3]==0) return false; //require the one at y-3
            if (trigger[x+1][y-1]>0) return false; //exclude (x+1,y-1)
            if (trigger[x-1][y-1]>0) return false; //exclude (x-1,y-1);
            if (trigger[x+1][y]>0) return false; //exclude (x+1,y-1)
            if (trigger[x-1][y]>0) return false; //exclude (x-1,y-1);
            return ret;
        }
        
        //(7,-3) --> (29,2) has a low amplitude
        //the one on toptop - require y=3 and y=1
        else if ((x == 29) && (y == 4)) {
            if (triggerLow[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-1]==0) return false; //require the one on y-1
            if (trigger[x][y-3]==0) return false; //require the one on y-3
            if (trigger[x+1][y]>0) return false; //exclude the one on RIGHT
            if (trigger[x-1][y]>0) return false; //exclude the one on LEFT
            if (trigger[x+1][y-1]>0) return false; //exclude the one on RIGHT
            if (trigger[x-1][y-1]>0) return false; //exclude the one on LEFT
            return ret;
        }
        //the one on top
        else if ((x == 29) && (y == 3)) {
            if (triggerLow[x][y] == 0) return false; //require that crystal
            if (trigger[x][y+1]==0) return false; //require the one on TOP
            if (trigger[x][y-2]==0) return false; //require the one on y-2
            if (trigger[x+1][y]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y]>0) return false; //require the one on LEFT
            if (trigger[x+1][y-1]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y-1]>0) return false; //require the one on LEFT
            return ret;
        }
        //the one on bottom
        else if ((x == 29) && (y == 1)) {
            if (triggerLow[x][y] == 0) return false; //require that crystal
            if (trigger[x][y-1]==0) return false; //require the one on BOTTOM
            if (trigger[x][y+2]==0) return false; //require the one on TOP
            if (trigger[x+1][y]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y]>0) return false; //require the one on LEFT
            if (trigger[x+1][y+1]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y+1]>0) return false; //require the one on LEFT
            return ret;
        }
        //the one on bottom-bottom
        else if ((x == 29) && (y == 0)) {
            if (trigger[x][y] == 0) return false; //require that crystal
            if (trigger[x][y+1]==0) return false; //require the one on BOTTOM
            if (trigger[x][y+3]==0) return false; //require the one on TOP 
            if (trigger[x+1][y]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y]>0) return false; //require the one on LEFT
            if (trigger[x+1][y+1]>0) return false; //require the one on RIGHT
            if (trigger[x-1][y+1]>0) return false; //require the one on LEFT
            return ret;
        }

        else {
            if (x > 0 && !ishole(x - 1, y)) {
                if (trigger[x - 1][y] > 0) return false;
            }
            if (x < (NX - 1) && !ishole(x + 1, y)) {
                if (trigger[x + 1][y] > 0) return false;
            }

            if ((y > 0) && !ishole(x, y - 1) && y != 5) {
                if (trigger[x][y - 1] == 0) return false;
            }
            if (y < (NY - 1) && !ishole(x, y + 1) && y != 4) {
                if (trigger[x][y + 1] == 0) return false;
            }
            if ((y == 0) || (y == 5) || ishole(x, y - 1)) {
                if (trigger[x][y + 2] == 0) return false;
            }
            if ((y == 4) || (y == 9) || ishole(x, y + 1)) {
                if (trigger[x][y - 2] == 0) return false;
            }

            if (triggerLow[x][y] < 2) {
                return false;
            }
        }
        return ret;
    }

    // Selection 3:
    // no x+1, x-1 
    // if X==0, no (x+1,y+1) and (x+1,y-1) when possilbe
    // if X==(NX-1), no (x-1,y+1) and (x-1,y-1) when possilbe
    // require the crystal with the lower threshold
    // select at least other 3 crystals in the same column
    boolean selectCrystal3(int x, int y) {
        boolean ret = true;
        int nY = 0;
        //crystal itself must be present
        if (triggerLow[x][y] == 0) return false;

        //no (x-1,y) if (x-1,y) exists
        if (x > 0 && !ishole(x - 1, y)) {
            if (trigger[x - 1][y] > 0) return false;
        }

        //no (x+1,y) if (x+1,y) exists
        if (x < (NX - 1) && !ishole(x + 1, y)) {
            if (trigger[x + 1][y] > 0) return false;
        }

        //case x==0: also require no x+1,y+1 and no y+1,y-1
        if (x == 0) {
            if (y == 0) {
                if (trigger[x + 1][y + 1] > 0) return false;
            } else if (y == (NY - 1)) {
                if (trigger[x + 1][y - 1] > 0) return false;
            } else {
                if ((trigger[x + 1][y + 1] > 0) || (trigger[x + 1][y - 1] > 0)) return false;
            }
        }

        //case x==(NX-1): also require no x-1,y+1 and no x-1,y-1
        if (x == (NX - 1)) {
            if (y == 0) {
                if (trigger[x - 1][y + 1] > 0) return false;
            } else if (y == (NY - 1)) {
                if (trigger[x - 1][y - 1] > 0) return false;
            } else {
                if ((trigger[x - 1][y + 1] > 0) || (trigger[x - 1][y - 1] > 0)) return false;
            }
        }

        //count crystals in the same column
        for (int iy = 0; iy < NY; iy++) {
            if (y == iy) continue;
            if (ishole(x, iy)) continue;
            if (trigger[x][iy] > 0) nY++;
        }

        if (nY < 3) return false;
        return ret;
    }

    // Selection 4:
    // no x+1, x-1 
    // if X==0, no (x+1,y+1) and (x+1,y-1) when possilbe
    // if X==(NX-1), no (x-1,y+1) and (x-1,y-1) when possilbe
    // require the crystal with the lower threshold
    // select at least other 4 crystals in the same column, only 3 if hole
    boolean selectCrystal4(int x, int y) {
        boolean ret = true;
        int nY = 0;
        //crystal itself
        if (triggerLow[x][y] == 0) return false;

        //no (x-1,y) if (x-1,y) exists
        if (x > 0 && !ishole(x - 1, y)) {
            if (trigger[x - 1][y] > 0) return false;
        }

        //no (x+1,y) if (x+1,y) exists
        if (x < (NX - 1) && !ishole(x + 1, y)) {
            if (trigger[x + 1][y] > 0) return false;
        }

        //case x==0: also require no x+1,y+1 and no y+1,y-1
        if (x == 0) {
            if (y == 0) {
                if (trigger[x + 1][y + 1] > 0) return false;
            } else if (y == (NY - 1)) {
                if (trigger[x + 1][y - 1] > 0) return false;
            } else {
                if ((trigger[x + 1][y + 1] > 0) || (trigger[x + 1][y - 1] > 0)) return false;
            }
        }

        //case x==(NX-1): also require no x-1,y+1 and no x-1,y-1
        if (x == (NX - 1)) {
            if (y == 0) {
                if (trigger[x - 1][y + 1] > 0) return false;
            } else if (y == (NY - 1)) {
                if (trigger[x - 1][y - 1] > 0) return false;
            } else {
                if ((trigger[x - 1][y + 1] > 0) || (trigger[x - 1][y - 1] > 0)) return false;
            }
        }

        //count crystals in the same column
        for (int iy = 0; iy < NY; iy++) {
            if (y == iy) continue;
            if (ishole(x, iy)) continue;
            if (trigger[x][iy] > 0) nY++;
        }

        if (ishole(x, 4)) {
            if (nY < 3) return false;
        } else {
            if (nY < 4) return false;
        }

        return ret;
    }

    /*
    // Selection 2:
    // no x+1, x-1
    // require the crystal 
    // select at least other 3 crystals in the same column
    boolean selectCrystal2(int x, int y) {
        boolean ret = true;
        int nY = 0;
        //crystal itself
        if (trigger[x][y] == 0) return false;
    
        //no x-1
        if (x > 0 && !ishole(x - 1, y)) {
            if (trigger[x - 1][y] > 0) return false;
        }
    
        //no x+1
        if (x < (NX - 1) && !ishole(x + 1, y)) {
            if (trigger[x + 1][y] > 0) return false;
        }
    
        for (int iy = 0; iy < NY; iy++) {
            if (y == iy) continue;
            if (ishole(x, iy)) continue;
            if (trigger[x][iy] > 0) nY++;
        }
    
        if (nY < 3) return false;
        return ret;
    }
    
    // Selection 3:
    // no x+1, x-1
    // ignore the crystal itself
    // select at least other 3 crystals in the same column 
    boolean selectCrystal3(int x, int y) {
        boolean ret = true;
        int nY = 0;
    
        //no x-1
        if (x > 0 && !ishole(x - 1, y)) {
            if (trigger[x - 1][y] > 0) return false;
        }
    
        //no x+1
        if (x < (NX - 1) && !ishole(x + 1, y)) {
            if (trigger[x + 1][y] > 0) return false;
        }
    
        for (int iy = 0; iy < NY; iy++) {
            if (y == iy) continue;
            if (ishole(x, iy)) continue;
            if (trigger[x][iy] > 0) nY++;
        }
    
        if (nY < 3) return false;
        return ret;
    }
    
    // Selection 4:
    // no x+1, x-1
    // select the crystal itelf
    // select at least other 3 crystals in the same column (only 2 if column has hole) 
    boolean selectCrystal4(int x, int y) {
        boolean ret = true;
        int nY = 0;
    
        //crystal itself
        if (trigger[x][y] == 0) return false;
    
        //no x-1
        if (x > 0 && !ishole(x - 1, y)) {
            if (trigger[x - 1][y] > 0) return false;
        }
    
        //no x+1
        if (x < (NX - 1) && !ishole(x + 1, y)) {
            if (trigger[x + 1][y] > 0) return false;
        }
    
        for (int iy = 0; iy < NY; iy++) {
            if (y == iy) continue;
            if (ishole(x, iy)) continue;
            if (trigger[x][iy] > 0) nY++;
        }
    
        if (ishole(x, 4)) {
            if (nY < 2) return false;
        } else {
            if (nY < 3) return false;
        }
        return ret;
    }
    
    // Selection 5:
    // no x+1, x-1
    // ignore the crystal itelf
    // select at least 3 crystals in the same column (only 2 if column has hole) 
    boolean selectCrystal5(int x, int y) {
        boolean ret = true;
        int nY = 0;
    
        //no x-1
        if (x > 0 && !ishole(x - 1, y)) {
            if (trigger[x - 1][y] > 0) return false;
        }
    
        //no x+1
        if (x < (NX - 1) && !ishole(x + 1, y)) {
            if (trigger[x + 1][y] > 0) return false;
        }
    
        for (int iy = 0; iy < NY; iy++) {
            if (y == iy) continue;
            if (ishole(x, iy)) continue;
            if (trigger[x][iy] > 0) nY++;
        }
    
        if (ishole(x, 4)) {
            if (nY < 2) return false;
        } else {
            if (nY < 3) return false;
        }
        return ret;
    }*/

    boolean ishole(int x, int y) {
        return (x > 12 && x < 22 && y > 3 && y < 6);
    }
}
