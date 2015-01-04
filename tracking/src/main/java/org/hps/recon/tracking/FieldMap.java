package org.hps.recon.tracking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.lcsim.conditions.ConditionsManager;
import org.hps.util.Pair;

/**
 * @author Mathew Graham <mgraham@slac.stanford.edu> $Id: FieldMap.java,v 1.14
 *         2012/07/23 23:02:57 mgraham Exp $
 */
// FIXME: Delete me.
public class FieldMap {

    // TODO: Change all pairs such that FPGA is the fist value
    // TODO: Change all map keys to type SiSensor?

    public static Map<Pair<Integer/*
                                   * zbin
                                   */, Integer/*
                                               * xbin
                                               */>, Double/*
                                                           * b_y
                                                           */> fieldMap = new HashMap<Pair<Integer, Integer>, Double>();;
    public static Map<Pair<Integer/*
                                   * zbin
                                   */, Integer/*
                                               * zbin
                                               */>, Pair<Double/*
                                                                * zpos
                                                                */, Double/*
                                                                           * xpos
                                                                           */>> fieldBins = new HashMap<Pair<Integer, Integer>, Pair<Double, Double>>();;
    private static boolean fieldMapLoaded = false;
    private static boolean debug = false;

    /**
     * Default Ctor
     */
    private FieldMap() {
    }

    public static void loadFieldMap() {
        loadFieldMap(-1);
    }

    public static void loadFieldMap(int run) {
        System.out.println("Loading fieldmap for run " + run);
        // write something here to read in constants from calibration file
        ConditionsManager conditions = ConditionsManager.defaultInstance();

        String filePath = null;

        // TODO: if we ever have more than one field map, make a list and
        // uncomment this
        // filePath =
        // HPSCalibrationListener.getCalibForRun("FieldMap/bfieldmap", run);

        if (filePath == null) {
            filePath = "FieldMap/bfieldmap.dat";
        }

        try {
            Reader baselineReader = conditions.getRawConditions(filePath).getReader();
            loadBField(baselineReader);
        } catch (IOException e) {
            throw new RuntimeException("couldn't get baseline file", e);
        }

    }

    private static void loadBField(Reader baselineReader) throws IOException {

        BufferedReader bufferedBaseline = new BufferedReader(baselineReader);
        String line;
        Double xval = 0.0, zval = 0.0, bY;
        Integer iz = -1, ix = 0;
        while (true) {
            try {
                line = bufferedBaseline.readLine();
            } catch (IOException e) {
                throw new RuntimeException("couldn't parse baseline file", e);
            }
            if (line == null) {
                break;
            }

            if (line.indexOf("#") != -1) {
                line = line.substring(0, line.indexOf("#"));
            }

            StringTokenizer tok = new StringTokenizer(line);
            if (debug) {
                System.out.println(line.toString());
            }
            List<Double> vals = new ArrayList<Double>();

            while ((vals = getNumbersInLine(tok)).size() > 0) {
                if (debug) {
                    System.out.println(getNumbersInLine(tok).size());
                }
                for (int i = 0; i < vals.size(); i++) {
                    double val = vals.get(i);
                    if (i == 0 && val >= 0) {
                        xval = 0.0;
                        zval = val * 10.0;// convert from cm to mm
                        ix = 0;
                        iz++;
                    } else {
                        bY = val / 10000.0 * 0.491 / 0.5;// gauss-->tesla and
                                                         // normalize by our
                                                         // nominal bfield (file
                                                         // is for 0.5T)
                        bY = Math.abs(bY);
                        Pair zx = new Pair(zval, xval);
                        Pair izix = new Pair(iz, ix);
                        // System.out.println("Putting B = "+bY+" & (Z,X) = "+zval+","+xval+")");
                        fieldMap.put(izix, bY);
                        // System.out.println("Making _fieldBins pair = ("+iz+","+ix+")");
                        fieldBins.put(izix, zx);
                        xval += 10.0;
                        ix++;
                    }
                }
            }
        }
        fieldMapLoaded = true;

    }

    public static boolean fieldMapLoaded() {
        return fieldMapLoaded;
    }

    private static List<Double> getNumbersInLine(StringTokenizer tok) throws IOException {
        List<Double> nums = new ArrayList<Double>();
        while (tok.hasMoreTokens()) {
            String tokVal = tok.nextToken();
            // System.out.println(tokVal);
            nums.add(Double.valueOf(tokVal).doubleValue());
        }
        // System.out.println("Returning list");
        return nums;
    }

    public static double getFieldFromMap(double zval, double xval) {
        Pair<Integer, Integer> bin = getFieldBin(zval, xval);
        return fieldMap.get(bin);
    }

    private static Pair<Integer, Integer> getFieldBin(double zval, double xval) {
        Integer iz, ix;
        int nZ = 150;
        int nX = 25;
        double zNew = Math.abs(zval - BeamlineConstants.DIPOLE_EDGE_TESTRUN / 2);
        double xNew = Math.abs(xval);
        for (iz = 0; iz < nZ; iz++) {
            Pair tmp = fieldBins.get(new Pair(iz, 0));
            Double zpair = (Double) tmp.getFirstElement();
            if (zpair > zNew) {
                break;
            }
        }
        for (ix = 0; ix < nX; ix++) {
            Pair tmp = fieldBins.get(new Pair(1, ix));
            Double xpair = (Double) tmp.getSecondElement();
            if (xpair > xNew) {
                break;
            }
        }
        Pair izix = new Pair(iz, ix);
        return izix;

    }

    public static void printFieldMap() {
        int xval = 0;
        System.out.printf("---- B-field ----\n");
        System.out.printf("%5s %5s %10s\n", "z", "x", "B");
        for (int zval = -100; zval < 1400; ++zval) {
            double bfield = FieldMap.getFieldFromMap((double) zval, (double) xval);
            System.out.printf("%5d %5d %10.3f\n", zval, xval, bfield);
        }
    }

}
