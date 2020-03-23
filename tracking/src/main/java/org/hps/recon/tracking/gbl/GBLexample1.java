package org.hps.recon.tracking.gbl;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import java.util.List;
import java.util.ArrayList;
import static java.lang.Math.sqrt;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.lcsim.util.aida.AIDA;

//import hep.aida.IHistogram1D;
import java.io.IOException;

public class GBLexample1 {
    
    private int nTry = 10000;
    private int nLayer = 10;
    private NormalDistribution norm = new NormalDistribution();
    private String outputPlots = "example1.root";
    private boolean debug = false;
    
    
    public AIDA aida;
        
    public void setupPlots() {
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        aida.histogram1D("Chi2", 500, 0, 100);
        aida.histogram1D("Ndf", 25,0,25);
        aida.histogram1D("Chi2_Ndf", 100,0,20);
        aida.histogram1D("clPar_true_0",100,-0.01,0.01);
        aida.histogram1D("clPar_true_1",100,-0.8,0.8);
        aida.histogram1D("clPar_true_2",100,-1.2,1.2);
        aida.histogram1D("clPar_true_3",100,-1,1);
        aida.histogram1D("clPar_true_4",100,-2.5,2.5);

        aida.histogram1D("clPar_fit_0",100,-0.01,0.01);
        aida.histogram1D("clPar_fit_1",100,-0.8,0.8);
        aida.histogram1D("clPar_fit_2",100,-1.2,1.2);
        aida.histogram1D("clPar_fit_3",100,-1,1);
        aida.histogram1D("clPar_fit_4",100,-2.5,2.5);

        
    }
    
    public void runExample() {
        setupPlots();
        System.out.println("Running GBL Example!");
        
        double sinLambda = 0.3;
        double cosLambda = Math.sqrt(1.0-sinLambda*sinLambda);
        double sinPhi = 0.;
        double cosPhi = Math.sqrt(1.0 - sinPhi * sinPhi);
        
        Matrix uvDir = new Matrix(2, 3);
        uvDir.set(0, 0, -sinPhi);
        uvDir.set(0, 1, cosPhi);
        uvDir.set(0, 2, 0.);
        uvDir.set(1, 0, -sinLambda * cosPhi);
        uvDir.set(1, 1, -sinLambda * sinPhi);
        uvDir.set(1, 2, cosLambda);

        if (debug) {
            System.out.println("UVDIR");
            uvDir.print(3,4);
        }
          

        Vector measErr = new Vector(2);
        measErr.set(0,0.001);
        measErr.set(1,0.001);
        Vector measPrec = new Vector(2);
        measPrec.set(0, 1.0 / (measErr.get(0) * measErr.get(0)));
        measPrec.set(1, 1.0 / (measErr.get(0) * measErr.get(0)));

        if (debug) {
            System.out.println("measPrec");
            measPrec.print(2,4);
        }

        Matrix measInvCov = new Matrix(2, 2);
        //Set it to zero
        measInvCov.times(0);
        measInvCov.set(0,0,measPrec.get(0));
        measInvCov.set(1,1,measPrec.get(1));

        if (debug) {
            System.out.println("measInvCov");
            measInvCov.print(2,4);
        }
        
        Vector scatErr = new Vector(2);
        scatErr.set(0,0.001);
        scatErr.set(1,0.001);
        Vector scatPrec = new Vector(2);
        scatPrec.set(0, 1.0 / (scatErr.get(0) * scatErr.get(0)));
        scatPrec.set(1, 1.0 / (scatErr.get(1) * scatErr.get(1)));

        if (debug) {
            System.out.println("scatPrec");
            scatPrec.print(2,4);
        }

        Vector clPar = new Vector(5);
        Vector clErr = new Vector(5);
        clErr.set(0,0.001);
        clErr.set(1,-0.1);
        clErr.set(2,0.2);
        clErr.set(3,-0.15);
        clErr.set(4,0.25);

        if (debug) {
            System.out.println("clErr");
            clErr.print(2,4);
        }

        Matrix clCov = new Matrix(5, 5);
        
        
        double bfac = 0.2998;
        double step = 1.5 / cosLambda;
        
        double Chi2Sum = 0.;
        int NdfSum = 0;
        double LostSum = 0.;
        int numFit = 0;
        Matrix jacPointToPoint = new Matrix(5, 5);
        
        //Check on factor
        //System.out.println(norm.sample());
        //double norm_sample = 0.5;
        
        for (int iTry = 1; iTry<=nTry; iTry+=1) {
            for (int i = 0; i<5; i+=1) {
                
                clPar.set(i, clErr.get(i)*norm.sample());
                //System.out.println("clPar " + i + " " + clPar.get(i));
                aida.histogram1D("clPar_true_"+String.valueOf(i)).fill(clPar.get(i));
            }
            
            clCov.times(0);
            for (int i = 0; i<5; i+=1) {
                clCov.set(i,i,1.0*clErr.get(i)*clErr.get(i));
            }
            
            if (debug) {
                System.out.println("clPar");
                clPar.print(2,4);
                System.out.println("clCov");
                clCov.print(5,7);
            }
            
            
            double s = 0;
            jacPointToPoint.UnitMatrix();
            List<GblPoint> listOfPoints = new ArrayList<GblPoint>();
            
            for (int iLayer = 0; iLayer<nLayer; iLayer+=1) {
                double sinStereo = (iLayer % 2 == 0) ? 0. : 0.1;
                double cosStereo = sqrt(1.0 - sinStereo * sinStereo);
                Matrix mDirT = new Matrix(3, 2);
                mDirT.times(0);
                mDirT.set(1,0,cosStereo);
                mDirT.set(2,0,sinStereo);
                mDirT.set(1,1,-sinStereo);
                mDirT.set(2,1,cosStereo);

                if (debug) {
                    System.out.println("mDirT");
                    mDirT.print(3,5);
                }
                
                Matrix proM2l = uvDir.times(mDirT);

                if (debug) {
                    System.out.println("ProM2L");
                    proM2l.print(2,3);
                }
                
                Matrix proL2m = proM2l.copy();
                proL2m = proL2m.inverse();
                
                if (debug) {
                    System.out.println("proL2m");
                    proL2m.print(2,3);
                }
                
                GblPoint pointMeas = new GblPoint(jacPointToPoint);
                if (debug) {
                    System.out.println("JacPointToPoint");
                    jacPointToPoint.print(5,5);
                }
                
                Vector meas = new Vector(2);
                Vector clPar_tail = new Vector(2);
                clPar_tail.set(0,clPar.get(3));
                clPar_tail.set(1,clPar.get(4));
                
                if (debug) {
                    System.out.println("clPar.tail");
                    clPar_tail.print(2,3);
                }
                
                meas = proL2m.times(clPar_tail);
                if (debug) {
                    System.out.println("measurement before smearing");
                    meas.print(2,3);
                }
                
                for (int i=0; i<2; i+=1) {
                    meas.set(i,meas.get(i)+measErr.get(i) * norm.sample());
                }
                
                if (debug) {
                    System.out.println("measurement after smearing");
                    meas.print(2,3);
                }
                
                pointMeas.addMeasurement(proL2m,meas,measPrec,0.);
                //pointMeas.printPoint(1);
                
                
                listOfPoints.add(pointMeas);

                if (debug) {
                    System.out.println("Points::" + listOfPoints.size());
                }
                
                jacPointToPoint = gblSimpleJacobianSvn(step, cosLambda, bfac);
                
                clPar = jacPointToPoint.times(clPar);
                clCov = jacPointToPoint.times(clCov.times(jacPointToPoint.transpose()));
                s=s+step;
                
                if (iLayer < nLayer-1) {
                    Vector scat = new Vector(2);
                    scat.set(0,0.);
                    scat.set(1,0.);
                    GblPoint pointScat = new GblPoint(jacPointToPoint);
                    pointScat.addScatterer(scat,scatPrec);
                    listOfPoints.add(pointScat);
                    
                    if (debug) {
                        System.out.println("clPar before Scatter");
                        clPar.print(1,3);
                        clCov.print(5,5);
                    }
                    
                    //Only change the slopes
                    for (int i =0; i<2; i+=1) {
                        double dslope = scatErr.get(i)*norm.sample();
                        double slope  = clPar.get(i + 1);
                        double dslope_prec = scatErr.get(i) * scatErr.get(i);
                        double slope_prec = clCov.get(i+1, i+1);
                        clPar.set(i + 1,slope+dslope);
                        clCov.set(i + 1, i + 1, slope_prec + dslope_prec);
                    }
                    if (debug) {
                        System.out.println("clPar after Scatter");
                        clPar.print(1,3);
                        clCov.print(5,5);
                    }
                    
                    clPar = jacPointToPoint.times(clPar);
                    clCov = jacPointToPoint.times(clCov.times(jacPointToPoint.transpose()));
                    s=s+step;
                }
                
                if (debug) {
                    System.out.println("--------");
                    System.out.println("clPar/clCov/ds before next point");
                    clPar.print(1,3);
                    System.out.println("--------");
                    clCov.print(5,5);
                    System.out.println("--------");
                    System.out.println(s);
                    System.out.println("--------");
                }
                    
                
            }//layers

            GblTrajectory traj = new GblTrajectory(listOfPoints);
            if (!traj.isValid()) {
                System.out.println("Example1: " + " Invalid GblTrajectory -> skip");
            }
            double[] dVals = new double[2];
            int [] iVals = new int[1];
            traj.fit(dVals,iVals,"");
            Chi2Sum += dVals[0];
            NdfSum += iVals[0];
            LostSum += dVals[1];
            aida.histogram1D("Chi2").fill(dVals[0]);
            aida.histogram1D("Ndf").fill(iVals[0]);
            aida.histogram1D("Chi2_Ndf").fill(dVals[0]/(double)iVals[0]);
        }
        
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                System.out.println("Coulnd't save outputPlots for example1");
            }
            
        }
    }

    
    private Matrix gblSimpleJacobianSvn(double ds, double cosl, double bfac) {
        
        Matrix jac = new Matrix(5,5);
        jac.UnitMatrix();
        jac.set(1, 0, -bfac * ds * cosl);
        jac.set(3, 0, -0.5 * bfac * ds * ds * cosl);
        jac.set(3, 1, ds);
        jac.set(4, 2, ds);
        return jac;
        
    }   
}

    
