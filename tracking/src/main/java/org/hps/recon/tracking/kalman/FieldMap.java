package org.hps.recon.tracking.kalman;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.lcsim.geometry.field.FieldOverlay;

// Retrieve the magnetic field vector from the HPS field map.
// The HPS field is down, in the -y direction in the map coordinates, or the z direction in local Kalman coordinates
// The constructor reads the map from a text file or binary file.
// The field map is in coordinates different from the Kalman fitter coordinates
//     x map =  x Kalman
//     y map = -z Kalman
//     z map =  y Kalman
// These map coordinates are the HPS global coordinates (not HPS tracking coordinates)
// This class is used for standalone testing of the Kalman code. See KalmanInterface.java for how the B-field access works
// when running in hps-java.

public class FieldMap extends FieldOverlay {
    private int nX, nY, nZ;
    private double[][][] bX, bY, bZ;
    private double[] X, Y, Z;
    private double dX, dY, dZ;
    private Vec offsets; // Offset of the map coordinates from the HPS coordinates
    private boolean uniform;

    public FieldMap(String FileName, String type, boolean uniform, double xOffset, double yOffset, double zOffset) throws IOException {
        // The offsets are in HPS coordinates and come from HPSDipoleFieldMap3D

        this.uniform = uniform; // To allow testing the effects of fitting with a uniform field
        if (type == "binary") { // This is far faster than scanning the text file (and the file is ~1/3 the size)!
            FileInputStream ifile = new FileInputStream(FileName);
            BufferedInputStream bis = new BufferedInputStream(ifile); // This buffering is essential!
            DataInputStream dis = new DataInputStream(bis);
            nX = dis.readInt();
            nY = dis.readInt();
            nZ = dis.readInt();
            System.out.format("FieldMap.java, buffered read of binary data: nX=%d, nY=%d, nZ=%d\n", nX, nY, nZ);
            X = new double[nX];
            Y = new double[nY];
            Z = new double[nZ];
            bX = new double[nX][nY][nZ];
            bY = new double[nX][nY][nZ];
            bZ = new double[nX][nY][nZ];
            int nEcho = 0;
            for (int ix = 0; ix < nX; ix++) {
                for (int iy = 0; iy < nY; iy++) {
                    for (int iz = 0; iz < nZ; iz++) { // The field map is expected to be uniformly spaced in each
                                                      // coordinate!
                        X[ix] = dis.readFloat();
                        Y[iy] = dis.readFloat();
                        Z[iz] = dis.readFloat();
                        bX[ix][iy][iz] = dis.readFloat();
                        bY[ix][iy][iz] = dis.readFloat();
                        bZ[ix][iy][iz] = dis.readFloat();
                        if (nEcho < 10) {
                            System.out.format("x=%12.4e, y=%12.4e, z=%12.4e, Bx=%12.4e, By=%12.4e, Bz=%12.4e\n", X[ix], Y[iy], Z[iz],
                                    bX[ix][iy][iz], bY[ix][iy][iz], bZ[ix][iy][iz]);
                            nEcho++;
                        }
                    }
                }
            }

            dis.close();
        } else {
            Scanner scan = new Scanner(new File(FileName));
            nX = 0;
            boolean foundEnd = false;
            while (scan.hasNextLine() && !foundEnd) {
                if (scan.hasNextInt() && nX == 0) {
                    nX = scan.nextInt();
                    nY = scan.nextInt();
                    nZ = scan.nextInt();
                    System.out.format("FieldMap.java, scanning text-format data: nX=%d, nY=%d, nZ=%d\n", nX, nY, nZ);
                } else {
                    if (scan.findInLine("End of Header") != null) {
                        System.out.format("FieldMap.java: end of header found in the field map file\n");
                        foundEnd = true;
                    }
                    String s = scan.nextLine();
                    System.out.println(s);
                }
            }

            X = new double[nX];
            Y = new double[nY];
            Z = new double[nZ];
            bX = new double[nX][nY][nZ];
            bY = new double[nX][nY][nZ];
            bZ = new double[nX][nY][nZ];
            int nEcho = 0;
            for (int ix = 0; ix < nX; ix++) {
                for (int iy = 0; iy < nY; iy++) {
                    for (int iz = 0; iz < nZ; iz++) {
                        X[ix] = scan.nextDouble();
                        Y[iy] = scan.nextDouble();
                        Z[iz] = scan.nextDouble();
                        bX[ix][iy][iz] = scan.nextDouble();
                        bY[ix][iy][iz] = scan.nextDouble();
                        bZ[ix][iy][iz] = scan.nextDouble();
                        if (!scan.hasNextLine()) {
                            System.out.format("FieldMap.java: stopped reading the text field map at i=%d, j=%d, k=%d\n", ix, iy, iz);
                            break;
                        }
                        scan.nextLine();
                        if (nEcho < 10) {
                            System.out.format("x=%12.4e, y=%12.4e, z=%12.4e, Bx=%12.4e, By=%12.4e, Bz=%12.4e\n", X[ix], Y[iy], Z[iz],
                                    bX[ix][iy][iz], bY[ix][iy][iz], bZ[ix][iy][iz]);
                            nEcho++;
                        }
                    }
                }
            }

            scan.close();
        }
        dX = X[1] - X[0];
        dY = Y[1] - Y[0];
        dZ = Z[1] - Z[0];
        System.out.format("FieldMap.java: dX=%10.7f, dY=%10.7f, dZ=%10.7f\n", dX, dY, dZ);
        offsets = new Vec(xOffset, yOffset, zOffset);
        offsets.print("field map offsets");
    }
    
    Vec getField(Vec r) { // Interpolate the 3D field map
        Vec rHPS;
        if (uniform) {
            rHPS = new Vec(0., 0., 505.57);
        } else {
            rHPS = new Vec(r.v[0], -r.v[2], r.v[1]); // Converting from Kalman coordinates to HPS coordinates
        }
        Vec rMag = rHPS.dif(offsets); // Converting to magnet coordinates
        // rMag.print("rMag original");
        int iX = (int) Math.floor((rMag.v[0] - X[0]) / dX);
        if (iX < 0) {
            iX = 0;
            rMag.v[0] = X[0];
        }
        if (iX > nX - 2) {
            iX = nX - 2;
            if (rMag.v[0] > X[nX - 1]) rMag.v[0] = X[nX - 1];
        }
        int iY = (int) Math.floor((rMag.v[1] - Y[0]) / dY);
        if (iY < 0) {
            iY = 0;
            rMag.v[1] = Y[0];
        }
        if (iY > nY - 2) {
            iY = nY - 2;
            if (rMag.v[1] > Y[nY - 1]) rMag.v[1] = Y[nY - 1];
        }
        int iZ = (int) Math.floor((rMag.v[2] - Z[0]) / dZ);
        if (iZ < 0) {
            iZ = 0;
            rMag.v[2] = Z[0];
            if (rMag.v[2] > Z[nZ - 1]) rMag.v[2] = Z[nZ - 1];
        }
        if (iZ > nZ - 2) iZ = nZ - 2;

        // r.print("r");
        // rHPS.print("rHPS");
        // rMag.print("rMag");
        // System.out.format("FieldMap.getField: iX=%d, iY=%d, iZ=%d, X=%10.5f,
        // Y=%10.5f, Z=%10.5f\n",iX,iY,iZ,X[iX],Y[iY],Z[iZ]);
        double xd = (rMag.v[0] - X[iX]) / dX;
        double yd = (rMag.v[1] - Y[iY]) / dY;
        double zd = (rMag.v[2] - Z[iZ]) / dZ;
        double Bx = triLinear(iX, iY, iZ, xd, yd, zd, bX) * 1000.;
        double By = triLinear(iX, iY, iZ, xd, yd, zd, bY) * 1000.;
        double Bz = triLinear(iX, iY, iZ, xd, yd, zd, bZ) * 1000.;
        // double Bxc = bX[iX][iY][iZ]*1000.;
        // double Byc = bY[iX][iY][iZ]*1000.;
        // double Bzc = bZ[iX][iY][iZ]*1000.;
        // new Vec(-Bxc,Bzc,Byc).print("B on grid");
        
        // This transforms the field from the HPS global coordinates to Kalman global coordinates
        if (uniform) {
            return new Vec(0.,0.,-By); // constant field
        }
        return new Vec(Bx, Bz, -By); // correct HPS field
        // return new Vec(-Bx, -Bz, +By); // reversed field
    }

    private double triLinear(int i, int j, int k, double xd, double yd, double zd, double[][][] f) {
        // System.out.format(" triLinear: xd=%10.7f, yd=%10.7f, zd=%10.7f\n", xd,yd,zd);
        // System.out.format(" 000=%12.4e, 100=%12.4e\n", f[i][j][k],f[i+1][j][k]);
        // System.out.format(" 001=%12.4e, 101=%12.4e\n", f[i][j][k+1],f[i+1][j][k+1]);
        // System.out.format(" 010=%12.4e, 110=%12.4e\n", f[i][j+1][k],f[i+1][j+1][k]);
        // System.out.format(" 011=%12.4e, 111=%12.4e\n",
        // f[i][j+1][k+1],f[i+1][j+1][k+1]);
        double c00 = f[i][j][k] * (1.0 - xd) + f[i + 1][j][k] * xd; // interpolate in x
        double c01 = f[i][j][k + 1] * (1.0 - xd) + f[i + 1][j][k + 1] * xd;
        double c10 = f[i][j + 1][k] * (1.0 - xd) + f[i + 1][j + 1][k] * xd;
        double c11 = f[i][j + 1][k + 1] * (1.0 - xd) + f[i + 1][j + 1][k + 1] * xd;
        double c0 = c00 * (1.0 - yd) + c10 * yd; // interpolate in y
        double c1 = c01 * (1.0 - yd) + c11 * yd;
        double c = c0 * (1.0 - zd) + c1 * zd; // interpolate in z
        // System.out.format(" c=%12.4e\n", c);
        return c;
    }

    void writeBinaryFile(String fName) { // Make a binary field map file that can be read much more quickly
        FileOutputStream ofile;
        try {
            ofile = new FileOutputStream(fName);
            DataOutputStream dos = new DataOutputStream(ofile);
            dos.writeInt(nX);
            dos.writeInt(nY);
            dos.writeInt(nZ);
            for (int i = 0; i < nX; i++) {
                for (int j = 0; j < nY; j++) {
                    for (int k = 0; k < nZ; k++) {
                        dos.writeFloat((float) X[i]);
                        dos.writeFloat((float) Y[j]);
                        dos.writeFloat((float) Z[k]);
                        dos.writeFloat((float) bX[i][j][k]);
                        dos.writeFloat((float) bY[i][j][k]);
                        dos.writeFloat((float) bZ[i][j][k]);
                    }
                }
            }

            dos.close();
        } catch (IOException e) {
            System.out.println("FieldMap.writeBinaryFile: IOException : " + e);
        }
    }
}
