/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.recon.tracking.gbl;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.hps.recon.tracking.gbl.GBLOutput.ClParams;
import org.lcsim.hps.recon.tracking.gbl.GBLOutput.PerigeeParams;
import org.lcsim.hps.alignment.RunAlignment;

/**
 * Handles text file printing for the GBL text file
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @version $Id: GBLFileIO.java,v 1.9 2013/11/07 03:54:58 phansson Exp $ $Date: 2013/11/07 03:54:58 $ $Author: phansson $ 
 */
public class GBLFileIO {

    PrintWriter _pWriter;
    FileWriter _fWriter;
    
    GBLFileIO(String fileName) {
        openFile(fileName);    
    }
    
    public void printEventInfo(int evtnr, double Bz) {
        addLine(String.format("New Event %d %.12f", evtnr, Bz));
    }
    
    protected void addLine(String line) {
        this._pWriter.println(line);
    }

    public void closeFile() {
        try {
            _pWriter.close();
            _fWriter.close();
        } catch(IOException ex) {
             Logger.getLogger(RunAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void openFile(String fileName) {
    	if(fileName.equalsIgnoreCase("")) {
    		System.out.printf("%s: no file name specified \n", this.getClass().getSimpleName());
    		System.exit(1);
    	}
    	try {
            _fWriter = new FileWriter(fileName);
            _pWriter = new PrintWriter(_fWriter);
        } catch (IOException ex) {
            Logger.getLogger(RunAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void printTrackID(int iTrack) {
        addLine(String.format("New Track %d", iTrack));
    }

    void printOldPerTrackParam(HelicalTrackFit htf) {
        addLine(String.format("Track perPar (R phi0 slope d0 z0) %.12f %.12f %.12f %.12f %.12f",htf.R(),htf.phi0(),htf.slope(),htf.dca(),htf.z0()));
    }
    
    String getPerTrackParamStr(PerigeeParams perPar) {
        return String.format("Track perPar (R theta phi d0 z0) %.12f %.12f %.12f %.12f %.12f",1.0/perPar.getKappa(),perPar.getTheta(),perPar.getPhi(),perPar.getD0(),perPar.getZ0());
    }
    
    void printPerTrackParam(PerigeeParams perPar) {
        addLine(this.getPerTrackParamStr(perPar));
    }

    String getPerTrackParamTruthStr(PerigeeParams perPar) {
        return String.format("Truth perPar (kappa theta phi d0 z0) %.12f %.12f %.12f %.12f %.12f",perPar.getKappa(),perPar.getTheta(),perPar.getPhi(),perPar.getD0(),perPar.getZ0());
    }

    void printPerTrackParamTruth(PerigeeParams perPar) {
        addLine(this.getPerTrackParamTruthStr(perPar));
    }

    String getClTrackParamTruthStr(ClParams perPar) {
        return String.format("Truth clPar (q/p lambda phi xT yT) %.12f %.12f %.12f %.12f %.12f",perPar.getQoverP(),perPar.getLambda(),perPar.getPhi(),perPar.getXt(),perPar.getYt());
    }

    void printClTrackParamTruth(ClParams perPar) {
        addLine(this.getClTrackParamTruthStr(perPar));
    }

    String getClTrackParamStr(ClParams perPar) {
        return String.format("Track clPar (q/p lambda phi xT yT) %.12f %.12f %.12f %.12f %.12f",perPar.getQoverP(),perPar.getLambda(),perPar.getPhi(),perPar.getXt(),perPar.getYt());
    }
    void printClTrackParam(ClParams perPar) {
        addLine(String.format("%s",this.getClTrackParamStr(perPar)));
    }

    void printNrHits(int n) {
        addLine(String.format("Track nr hits <%d>",n));
    }
    
    void printStripJacPointToPoint(int id, int layer, double s, BasicMatrix jac) {
        String str = String.format("Strip id <%d> layer <%d> s <%.10f> jac <",id,layer,s);
        for(int r=0;r<jac.getNRows();++r) {
            for(int c=0;c<jac.getNColumns();++c) {
                str += String.format("%.10f ", jac.e(r, c));
            }
        }
        str += ">";
        addLine(str);
    }

    
    void printStripScatJacPointToPoint(int id, int layer, double s, BasicMatrix jac) {
        String str = String.format("Strip scat id <%d> layer <%d> s <%.10f> jac <",id,layer,s);
        for(int r=0;r<jac.getNRows();++r) {
            for(int c=0;c<jac.getNColumns();++c) {
                str += String.format("%.10f ", jac.e(r, c));
            }
        }
        str += ">";
        addLine(str);
    }

    void printStripL2m(int id, int layer, double s, BasicMatrix jac) {
        String str = String.format("Strip LocalToMeas id <%d> layer <%d> s <%.10f> L2m <",id,layer,s);
        for(int r=0;r<jac.getNRows();++r) {
            for(int c=0;c<jac.getNColumns();++c) {
                str += String.format("%.10f ", jac.e(r, c));
            }
        }
        str += ">";
        addLine(str);
    }

    void printPerTrackCov(HelicalTrackFit htf) {
        String str = "Track perCov (idx: dca,phi0,curv,z0,slope) ";
        SymmetricMatrix cov = htf.covariance();
        for(int irow=0;irow<cov.getNRows();++irow) {
            for(int icol=0;icol<cov.getNColumns();++icol) {
                str += String.format("%e ", cov.e(irow, icol));
            }    
        }
        addLine(str);
    }

    void printCLTrackCov(BasicMatrix cov) {
        String str = "Track clCov ";
        for(int irow=0;irow<cov.getNRows();++irow) {
            for(int icol=0;icol<cov.getNColumns();++icol) {
                str += String.format("%.10f ", cov.e(irow, icol));
            }    
        }
        addLine(str);
    }

    
    void printStripTrackDir(double sinPhi, double sinLambda) {
        String str = String.format("Strip sinPhi sinLambda %.10f %.10f",sinPhi,sinLambda);
        addLine(str);
    }
    
    void printStripTrackDirFull(Hep3Vector dir) {
        addLine(String.format("Strip track dir %.10f %.10f %.10f",dir.x(),dir.y(),dir.z()));
    }
    
    void printStripTrackPos(Hep3Vector pos) {
        addLine(String.format("Strip track pos %.10f %.10f %.10f",pos.x(),pos.y(),pos.z()));
    }

    void printStrip(int id, int layer) {
        addLine(String.format("New Strip id layer %d %d", id,layer));
    }

    void printStripPathLen(double s) {
        addLine(String.format("Strip pathLen %.10f", s));
    }

     void printStripPathLen3D(double s) {
        addLine(String.format("Strip pathLen3D %.10f", s));
    }
    
    void printStereoAngle(double stereoAngle) {
        addLine(String.format("Strip stereo angle %.10f", stereoAngle));
    }

    void printStripMeas(double u) {
        addLine(String.format("Strip u %.10f", u));
    }

    void printStripMeasRes(double ures, double uresErr) {
        addLine(String.format("Strip ures %.10f %.10f", ures, uresErr));
    }

    void printStripMeasResTruth(double ures, double uresErr) {
        addLine(String.format("Strip truth ures %.10f %.10f", ures, uresErr));
    }
    
    void printStripMeasResSimHit(double ures, double uresErr) {
        addLine(String.format("Strip simhit ures %.10f %.10f", ures, uresErr));
    }
    
    void printStripScat(double scatAngle) {
        addLine(String.format("Strip scatangle %.10f",scatAngle));
    }

    void printMeasDir(Hep3Vector u) {
        addLine(String.format("Strip meas dir %.10f %.10f %.10f", u.x(), u.y(), u.z()));
    }

    void printNonMeasDir(Hep3Vector u) {
        addLine(String.format("Strip non-meas dir %.10f %.10f %.10f", u.x(), u.y(), u.z()));
    }

    void printNormalDir(Hep3Vector u) {
        addLine(String.format("Strip normal dir %.10f %.10f %.10f", u.x(), u.y(), u.z()));
    }

    
    void printMomentum(double p, double p_truth) {
        addLine(String.format("Track mom %.10f %.10f", p, p_truth));
    }

    void printPerToClPrj(Hep3Matrix perToClPrj) {
        String str = "Track perToClPrj ";
        for(int irow=0;irow<perToClPrj.getNRows();++irow) {
            for(int icol=0;icol<perToClPrj.getNColumns();++icol) {
                str += String.format("%.10f ", perToClPrj.e(irow, icol));
            }    
        }
        addLine(str);
    }

    void printChi2(double[] chisq, int[] ndf) {
        addLine(String.format("Track chi2/ndf (circle,zfit) %.10f %d %.10f %d",chisq[0],ndf[0],chisq[1],ndf[1]));
    }

    void printOrigin(Hep3Vector pos) {
        addLine(String.format("Strip origin pos %.10f %.10f %.10f",pos.x(),pos.y(),pos.z()));
    }

    void printHitPos3D(Hep3Vector pos) {
        addLine(String.format("Strip 3D hit pos %.10f %.10f %.10f",pos.x(),pos.y(),pos.z()));
    }



}
