package org.hps.analysis.alignment.straighttrack.vertex;

/**
 *
 * @author ngraf
 */
public class Vertex {

    double fX, fY, fZ, fChi2;
    double[] fC = new double[6];
    int fNDF, fNTracks;

    public Vertex() {
    }

    public double x() {
        return fX;
    }

    public double y() {
        return fY;
    }

    public double z() {
        return fZ;
    }

    public double chisq() {
        return fChi2;
    }

    public double[] covMatrix() {
        return fC;
    }

    public int ndf() {
        return fNDF;
    }

    public double ntracks() {
        return fNTracks;
    }
    
    public void setCov(double[] cov)
    {
        System.arraycopy(cov,0,fC,0,6);
    }

    public String toString() {
        return "Vertex : (" + fX + " " + fY + " " + fZ + " )";
    }
}
