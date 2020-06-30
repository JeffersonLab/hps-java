package org.hps.recon.tracking; 

import java.util.ArrayList;
import java.util.List;
import java.util.Random; 

import org.junit.Test; 

import org.hps.recon.tracking.gbl.matrix.Matrix;

import jeigen.DenseMatrix;

import org.ejml.simple.SimpleMatrix;

//import cern.colt.matrix.DoubleFactory2D;


public class LinAlgebraCompTest { 

    Random rand = new Random();

    private static final double TOTAL_MATRICES = 500000.;
    private static final int m = 10;  
    private static final int n = 10;  

    @Test
    public void compMatrixMult() { 
        
        // Start by generating random 10 x 10 matrices that will be used
        // to test the performance of each algorithm. 
        List< double[][] > randMatricesA = new ArrayList< double[][] >();
        List< double[][] > randMatricesB = new ArrayList< double[][] >();

        long startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            double [][] matrixA = new double[m][n]; 
            double [][] matrixB = new double[m][n]; 
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    matrixA[i][j] = (double) rand.nextInt(100000);  
                    matrixB[i][j] = (double) rand.nextInt(100000);  
                }
            }
            randMatricesA.add(matrixA); 
            randMatricesB.add(matrixB); 
        }
        long duration = System.nanoTime() - startTime;
        System.out.println("Time to generate 2D array: " 
                + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 
    
        // Create a list of Java GBL Matrices
        List< Matrix > javaMatricesA = new ArrayList< Matrix >(); 
        List< Matrix > javaMatricesB = new ArrayList< Matrix >(); 
   
        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            javaMatricesA.add(new Matrix(randMatricesA.get(iMatrix), m, n)); 
            javaMatricesB.add(new Matrix(randMatricesB.get(iMatrix), m, n)); 
        }
        duration = System.nanoTime() - startTime;
        System.out.println("Time to instantiate Java GBL matrix: " 
                + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 

        // Create a list of Jama Matrices
        List< Jama.Matrix > jamaMatricesA = new ArrayList< Jama.Matrix >(); 
        List< Jama.Matrix > jamaMatricesB = new ArrayList< Jama.Matrix >(); 
        
        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            jamaMatricesA.add(new Jama.Matrix(randMatricesA.get(iMatrix), m, n)); 
            jamaMatricesB.add(new Jama.Matrix(randMatricesB.get(iMatrix), m, n)); 
        }
        duration = System.nanoTime() - startTime;
        System.out.println("Time to instantiate JAMA matrix: " 
                + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 

        // Create a List of Jeigen Matrices
        List< DenseMatrix > jeigenMatricesA = new ArrayList< DenseMatrix >(); 
        List< DenseMatrix > jeigenMatricesB = new ArrayList< DenseMatrix >(); 
        
        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            jeigenMatricesA.add(new DenseMatrix(randMatricesA.get(iMatrix))); 
            jeigenMatricesB.add(new DenseMatrix(randMatricesB.get(iMatrix)));
        }
        duration = System.nanoTime() - startTime;
        System.out.println("Time to instantiate Jeigen matrix: " 
                + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 


        // Create a List of Jeigen Matrices
        List< SimpleMatrix > ejmlMatricesA = new ArrayList< SimpleMatrix >(); 
        List< SimpleMatrix > ejmlMatricesB = new ArrayList< SimpleMatrix >(); 
        
        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            ejmlMatricesA.add(new SimpleMatrix(randMatricesA.get(iMatrix))); 
            ejmlMatricesB.add(new SimpleMatrix(randMatricesB.get(iMatrix)));
        }
        duration = System.nanoTime() - startTime;
        System.out.println("Time to instantiate EJML matrix: " 
                + ((double) (duration/1000.)/TOTAL_MATRICES) + " us");

        // Create a List of Jeigen Matrices
        //List< DoubleMatrix2D > coltMatricesA = new ArrayList< SimpleMatrix >(); 
        //List< DoubleMatrix2D > coltMatricesB = new ArrayList< SimpleMatrix >(); 
        
        /*startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            coltMatricesA.add(new doubleFactory2D.make(randMatricesA.get(iMatrix))); 
            coltMatricesB.add(new doubleFactory2D.make(randMatricesB.get(iMatrix)));
        }
        duration = System.nanoTime() - startTime;
        System.out.println("Time to instantiate COLT matrix: " 
                + ((double) (duration/1000.)/TOTAL_MATRICES) + " us");
        */

        // Check performance of basic multiplication
        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            javaMatricesA.get(iMatrix).times(javaMatricesB.get(iMatrix)); 
        } 
        duration = System.nanoTime() - startTime;
        System.out.println("Time to multiply " + m + "x" + n 
                + " Java GBL matrices: " + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 
        
        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            jamaMatricesA.get(iMatrix).times(jamaMatricesB.get(iMatrix)); 
        } 
        duration = System.nanoTime() - startTime;
        System.out.println("Time to multiply " + m + "x" + n 
                + " JAMA matrices: " + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 
        
        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            jeigenMatricesA.get(iMatrix).mmul(jeigenMatricesB.get(iMatrix)); 
        } 
        duration = System.nanoTime() - startTime;
        System.out.println("Time to multiply " + m + "x" + n 
                + " Jeigen matrices: " + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 


        startTime = System.nanoTime();
        for (int iMatrix = 0; iMatrix < TOTAL_MATRICES; iMatrix++) {
            ejmlMatricesA.get(iMatrix).mult(ejmlMatricesB.get(iMatrix)); 
        } 
        duration = System.nanoTime() - startTime;
        System.out.println("Time to multiply " + m + "x" + n 
                + " EJML matrices: " + ((double) (duration/1000.)/TOTAL_MATRICES) + " us"); 
    }
}
