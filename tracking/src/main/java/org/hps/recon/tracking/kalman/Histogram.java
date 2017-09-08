package kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

class Histogram {  // Very light interface into Gnuplot to make histograms while testing the fitting package.
	               // The output histograms are just text files that can be displayed by Gnuplot.
	    int N;
	    double BW, B0;
	    ArrayList<Integer> counts;
	    String T, XL, YL;
	    double sumX, sumX2;
	    int nEntry;
	    ArrayList<Double> X, Y, Ex, Ey;

	    Histogram(int nBins, double bin0, double binWidth, String title, String xlabel, String ylabel) {
	        N= nBins;
	        BW= binWidth;
	        B0= bin0;
	        T= title;
	        XL= xlabel;
	        YL= ylabel;
	        sumX= 0.;
	        sumX2= 0.;
	        nEntry= 0;
	        counts = new ArrayList<Integer>(nBins);
	        for (int i=0; i<nBins; ++i) {
	            counts.add(i, 0);  
	        }
	    }
	    
	    String Title() {
	        return T;
	    }

	    void entry(double x) {
	        int bin = (int)Math.floor((x - B0)/BW);
	        if (bin >=0 && bin < N) {
	        	int cnt = counts.get(bin);
	        	counts.set(bin, cnt+1);
	        }
	        if (x >= B0 && x <= B0+N*BW) {
	            sumX += x;
	            sumX2 += x*x;
	            nEntry++;
	        }
	    }    
	    void entry(float xf) {
	        double x= (double)xf;
	        int bin = (int)Math.floor((x - B0)/BW);
	        if (bin >=0 && bin < N) {
	        	int cnt = counts.get(bin);
	        	counts.set(bin, cnt+1);
	        }
	        if (x >= B0 && x <= B0+N*BW) {
	            sumX += x;
	            sumX2 += x*x;
	            nEntry++;
	        }
	    }    
	    void entry(int i) {
	        double x= (double)(i);
	        int bin = (int)Math.floor((x - B0)/BW);
	        if (bin >=0 && bin < N) {
	        	int cnt = counts.get(bin);
	        	counts.set(bin, cnt+1);
	        }
	        if (x >= B0 && x <= B0+N*BW) {
	            sumX += x;
	            sumX2 += x*x;
	            nEntry++;
	        }
	    }        
	    
	    void plot(String fileName, boolean stats, String choice, String txt) {
	        X = new ArrayList<Double>(N);
	        Y = new ArrayList<Double>(N);
	        Ex = new ArrayList<Double>(N);
	        Ey = new ArrayList<Double>(N);
	        for (int i=0; i<N; ++i) {
	            X.add(i, B0 + (((double)i)+0.5)*BW);
	            Y.add(i,(double)counts.get(i));
	            Ex.add(i, BW/2.);
	            Ey.add(i, Math.sqrt(counts.get(i)));
	        }
	        double mean=-999.;
	        double rms=-999.;
	        if (nEntry>0) {
	            mean = sumX/(double)(nEntry);
	            rms = Math.sqrt(sumX2/nEntry - mean*mean);
	        }
			File file = new File(fileName);
			file.getParentFile().mkdirs();
			PrintWriter pW = null;
			try {
				pW = new PrintWriter(file);
			} catch (FileNotFoundException e1) {
				System.out.format("Histogram.plot: could not create the gnuplot output file for histogram %s\n", T);
				e1.printStackTrace();
				return;
			}
	        pW.format("#*** This file is intended to be displayed by gnuplot.\n");
	        pW.format("#*** Either double click on the file (works in Windows at least),\n");
	        pW.format("#*** or else start up gnuplot and use the load command to display the plot.\n");
	        //The following line will make the plot persist in linux when double clicking on it, but it doesn't work in multiplot mode.
	        //pW.format("set term X11 persist\n");
	        if (stats) {
	            pW.format("set label 777 'mean=%7.3f' at graph 0.67, 0.9 left font 'Verdana,12'\n",mean);
	            pW.format("set label 778 'rms=%8.3f' at graph 0.67, 0.85 left font 'Verdana,12'\n",rms);
	            pW.format("set label 779 'counts=%d' at graph 0.67, 0.80 left font 'Verdana,12'\n",nEntry);
	        }
	        pW.format("set xtics font 'Verdana,12'\n");
	        pW.format("set ytics font 'Verdana,12'\n");
	        pW.format("set title '%s %s' font 'Verdana,12'\n",T,txt);
	        pW.format("set xlabel '%s' font 'Verdana,12'\n",XL); 
	        pW.format("set ylabel '%s' font 'Verdana,12'\n",YL);
	        pW.format("set xrange[%7.4f : %7.4f]\n",B0,B0+N*BW);
	        pW.format("set nokey\n");
	        if (choice == "errors") {
	        	pW.format("plot '-' with xyerrorbars \n");
	            for (int i=0; i<N; i++) {
	            	pW.format("%8.3e %8.3e %8.3e %8.3e\n",X.get(i),Y.get(i),Ex.get(i),Ey.get(i));
	            }
	            pW.format("e\n");
	        } else {
	        	pW.format("plot '-' with boxes\n"); 
	            for (int i=0; i<N; i++) {
	            	pW.format("%8.3e %8.3e\n",X.get(i),Y.get(i));
	            }
	            pW.format("e\n");
	        }
	        pW.close();
	    }
}
