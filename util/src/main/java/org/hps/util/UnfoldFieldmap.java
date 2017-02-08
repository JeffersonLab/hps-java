package org.hps.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import static java.lang.Math.abs;

public class UnfoldFieldmap {

    // Storage space for the table
    private double[][][] _xField;
    private double[][][] _yField;
    private double[][][] _zField;
    // The dimensions of the table
    private int _nx, _ny, _nz;
    // The physical limits of the defined region
    private double _minx, _maxx, _miny, _maxy, _minz, _maxz;
    // The physical extent of the defined region
    private double _dx, _dy, _dz;
    // Offsets if field map is not in global coordinates
    private double _xOffset;
    private double _yOffset;
    private double _zOffset;
    // normalizations for the GUI
    // double[] _maxDim = new double[3];
    // double[] _offSet = new double[3];
    // double _maxScale = 0.;
    // maximum field strength
    private double _bMax;
    List<double[]> lines = new ArrayList<double[]>();
    double[][] Bx;
    double[][] By;
    double[][] Bz;
    double[] xVals;
    double[] zVals;
    double[] yVals;

    private double _scaleFactor;
    private String _map2read;

    public UnfoldFieldmap() {
        _scaleFactor = 1.;
    }

    public UnfoldFieldmap(String map2read, double scaleFactor) {
        _map2read = map2read;
        _scaleFactor = scaleFactor;
    }

    void setup() {
        // System.out.println(this.getClass().getClassLoader().getResource(map2read));
        // InputStream is = this.getClass().getClassLoader().getResourceAsStream(map2read);

        System.out.println("\n-----------------------------------------------------------"
                + "\n      Reading Magnetic Field map " + _map2read + "\n      Scaling values by " + _scaleFactor
                + "\n      Flipping sign of By " + "\n-----------------------------------------------------------");

        try {
            FileInputStream fin = new FileInputStream(_map2read);
            BufferedReader myInput = new BufferedReader(new InputStreamReader(new BufferedInputStream(fin)));

            String thisLine;
            // ignore the first blank line
            // thisLine = myInput.readLine();
            // next line has table dimensions
            _nx = 51;
            _ny = 15;
            _nz = 301;

            Bx = new double[_nx][_nz];
            By = new double[_nx][_nz];
            Bz = new double[_nx][_nz];
            xVals = new double[_nx];
            yVals = new double[_ny];
            zVals = new double[_nz];
            // Set up storage space for table
            _xField = new double[_nx + 1][_ny + 1][_nz + 1];
            _yField = new double[_nx + 1][_ny + 1][_nz + 1];
            _zField = new double[_nx + 1][_ny + 1][_nz + 1];

            // Ignore other header information
            // The first line whose second character is '0' is considered to
            // be the last line of the header.
            // do {
            // thisLine = myInput.readLine();
            // st = new StringTokenizer(thisLine, " ");
            // } while (!st.nextToken().trim().equals("0"));

            // now ready to read in the values in the table
            // format is:
            // x y z Bx By Bz
            //
            // TOSCA files have distance measured
            // TOSCA files have field in OERSTED
            // Geant4 requires mm and .0001T, so need to convert
            // OERSTED to Tesla = 10000
            // Tesla to Geant4 internal units where 1 Tesla is equal to 0.001
            // field conversion factor is 10000000
            //
            double fieldConversionFactor = 10000000.;
            double lengthConversionFactor = 10.;
            int ix, iy, iz;
            double xval = 0.;
            double yval = 0.;
            double zval = 0.;
            double bx, by, bz;
            for (ix = 0; ix < _nx; ix++) {
                for (iy = 0; iy < _ny; iy++) {
                    for (iz = 0; iz < _nz; iz++) {
                        thisLine = myInput.readLine();
                        // System.out.println(thisLine);
                        StringTokenizer st = new StringTokenizer(thisLine, " ");
                        // System.out.println("xval "+xval);
                        xval = Double.parseDouble(st.nextToken());
                        // System.out.println("xval "+xval);
                        yval = Double.parseDouble(st.nextToken());
                        // System.out.println("yval "+yval);
                        zval = Double.parseDouble(st.nextToken());
                        // System.out.println("zval "+zval);
                        bx = Double.parseDouble(st.nextToken());
                        // System.out.println("bx "+bx);
                        by = -Double.parseDouble(st.nextToken());
                        // System.out.println("by "+by);
                        bz = Double.parseDouble(st.nextToken());
                        // System.out.println("bz "+bz);
                        double[] line = {xval, yval, zval, bx, by, bz};
                        lines.add(line);
                        Bx[ix][iz] = _scaleFactor * bx / fieldConversionFactor; // convert to magnetic field units used
                                                                                // by Geant4
                        By[ix][iz] = _scaleFactor * by / fieldConversionFactor; //
                        Bz[ix][iz] = _scaleFactor * bz / fieldConversionFactor; //
                        xVals[ix] = xval * lengthConversionFactor;
                        yVals[iy] = yval * lengthConversionFactor;
                        zVals[iz] = zval * lengthConversionFactor;
                        if (ix == 0 && iy == 0 && iz == 0) {
                            _minx = xval;
                            _miny = yval;
                            _minz = zval;
                        }
                        _xField[ix][iy][iz] = bx;
                        _yField[ix][iy][iz] = by;
                        _zField[ix][iy][iz] = bz;
                        double b = bx * bx + by * by + bz * bz;
                        if (b > _bMax) {
                            _bMax = b;
                        }
                    }
                }
            }
            _bMax = sqrt(_bMax);

            _maxx = xval;
            _maxy = yval;
            _maxz = zval;

            System.out.println("\n ---> ... done reading ");
            System.out.println(" ---> assumed the order:  x, y, z, Bx, By, Bz " + "\n ---> Min values x,y,z: " + _minx
                    + " " + _miny + " " + _minz + " cm " + "\n ---> Max values x,y,z: " + _maxx + " " + _maxy + " "
                    + _maxz + " cm " + "\n Maximum Field strength: " + _bMax + " "
                    + "\n ---> The field will be offset by " + _xOffset + " " + _yOffset + " " + _zOffset + " cm ");

            _dx = _maxx - _minx;
            _dy = _maxy - _miny;
            _dz = _maxz - _minz;
            System.out.println("\n ---> Range of values x,y,z: " + _dx + " " + _dy + " " + _dz + " cm in z "
                    + "\n-----------------------------------------------------------");

            System.out.println(lines.size());
            myInput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // want to normalize the dimensions to fit into the visible 1x1x1 cube of the applet
        // _maxDim[0] = (_maxx - _minx) / 2.;
        // _maxDim[1] = (_maxy - _miny) / 2.;
        // _maxDim[2] = (_maxz - _minz) / 2.;
        // for (int ii = 0; ii < 3; ++ii) {
        // if (_maxDim[ii] > _maxScale) {
        // _maxScale = _maxDim[ii];
        // }
        // }
        // _offSet[0] = (_maxx + _minx) / 2.;
        // _offSet[1] = (_maxy + _miny) / 2.;
        // _offSet[2] = (_maxz + _minz) / 2.;

    }

    public void writeout() {
        try {
            // output : Unicode to Cp850 (MS-DOS Latin-1)
            FileOutputStream fos = new FileOutputStream("out.dat");
            Writer w = new BufferedWriter(new OutputStreamWriter(fos, "Cp850"));
            w.write("\n");
            w.write((2 * _nx - 1) + " " + (2 * _ny - 1) + " " + (2 * _nz - 1) + "\n");
            w.write(" 1 X(mm) \n");
            w.write(" 2 Y(mm) \n");
            w.write(" 3 Z(mm) \n");
            w.write(" 4 BX(1000T) \n");
            w.write(" 5 BY(1000T) \n");
            w.write(" 6 BZ(1000T) \n");
            w.write(" 0 End of Header. Data follows in above format \n");

            // want to reflect the field in x and z
            // for now, assume (0,0) is minimum for (x,z)

            for (int i = -(_nx - 1); i < _nx; ++i) {
                for (int j = -(_ny - 1); j < _ny; ++j) {
                    for (int k = -(_nz - 1); k < _nz; ++k) {
                        double xSign = (i < 0) ? -1. : 1.;
                        double ySign = (j < 0) ? -1. : 1.;
                        double zSign = (k < 0) ? -1. : 1.;
                        // double x = (i < 0) ? -xVals[abs(i)] : xVals[abs(i)];
                        // double y = (j < 0) ? -yVals[abs(j)] : yVals[abs(j)];
                        // double z = (k < 0) ? -zVals[abs(k)] : zVals[abs(k)];
                        w.write(xSign * xVals[abs(i)] + " " + ySign * yVals[abs(j)] + " " + zSign * zVals[abs(k)] + " "
                                + xSign * Bx[abs(i)][abs(k)] + " " + By[abs(i)][abs(k)] + " " + zSign
                                * Bz[abs(i)][abs(k)] + " \n");
                    }
                }
            }

            w.flush();
            w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // public void writeoutGnuplot()
    // {
    // try {
    // // output : Unicode to Cp850 (MS-DOS Latin-1)
    // FileOutputStream fos = new FileOutputStream("gnuplot.dat");
    // Writer w =
    // new BufferedWriter(new OutputStreamWriter(fos, "Cp850"));
    // // want to reflect the field in x and z
    // // for now, assume (0,0) is minimum for (x,z)
    // for (int i = -(_nx - 1); i < _nx; ++i) {
    // //for (int j = 0; j < _ny; ++j) {
    // for (int k = -(_nz - 1); k < _nz; ++k) {
    // double x = (i < 0) ? -xVals[abs(i)] : xVals[abs(i)];
    // double z = (k < 0) ? -zVals[abs(k)] : zVals[abs(k)];
    // w.write(x + " " + z + " " + By[abs(i)][abs(k)] + "\n");
    // }
    // w.write("\n");
    // //}
    // }
    //
    // w.flush();
    // w.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

    public static void main(String[] args) {
        String map2read = "209acm2_5kg.table";
        double scaleFactor = 1.0;
        if (args.length > 0) {
            map2read = args[0];
        }
        if (args.length > 1) {
            scaleFactor = Double.parseDouble(args[1]);
        }
        System.out.println("Reading in field map " + map2read);
        System.out.println("Scaling field map by " + scaleFactor);
        UnfoldFieldmap doit = new UnfoldFieldmap(map2read, scaleFactor);
        doit.setup();
        doit.writeout();
        // doit.writeoutGnuplot();
    }
}
