package org.hps.users.spaul.feecc;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.JFrame;

import hep.aida.IAnalysisFactory;
import hep.aida.ICloud2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterRegion;
import hep.aida.IPlotterStyle;
import hep.aida.ITreeFactory;

public class ShowCustomBinning extends Canvas{
	/**
	 * show Rafo's fiducial cuts translated into rotated theta and phi, 
	 * and overlay this with my bins in x and y.  
	 * @param arg
	 * @throws FileNotFoundException 
	 */
	public static void main(String arg[]) throws FileNotFoundException{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new ShowCustomBinning(new File(arg[0])));
		frame.setSize(800, 800);
		frame.setVisible(true);
		
	}
	public void paint(Graphics g){
		drawEcalOutline(g);
		drawFidEcalOutline(g);
		drawCustomBinRectangles(g);
	}
	
	void drawFidEcalOutline(Graphics g){
		g.setColor(Color.GRAY);
		double x_edge_low = -262.74;
		double x_edge_high = 347.7;
		double y_edge_low = 33.54;
		double y_edge_high = 75.18;

		double x_gap_low = -106.66;
		double x_gap_high = 42.17;
		double y_gap_high = 47.18;
		double x1,y1, x2, y2;
		double nPoints = 500;
		for(int i = 0; i< nPoints-1; i++){
			x1 = x_gap_high+i/nPoints*(x_edge_high-x_gap_high);
			x2 = x_gap_high+(i+1)/nPoints*(x_edge_high-x_gap_high);
			y1 = y_edge_low;
			y2 = y1;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
			
			
			x1 = x_edge_low+i/nPoints*(x_gap_low-x_edge_low);
			x2 = x_edge_low+(i+1)/nPoints*(x_gap_low-x_edge_low);
			y1 = y2 = y_edge_low;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
			
			
			
			x1 = x_gap_low+i/nPoints*(x_gap_high-x_gap_low);
			x2 = x_gap_low+(i+1)/nPoints*(x_gap_high-x_gap_low);
			y1 = y2 = y_gap_high;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
			
			x1 = x_edge_low+i/nPoints*(x_edge_high-x_edge_low);
			x2 = x_edge_low+(i+1)/nPoints*(x_edge_high-x_edge_low);
			y1 = y2 = y_edge_high;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
		}
		drawEcalFaceLine(g, x_gap_low, y_edge_low, x_gap_low, y_gap_high);
		drawEcalFaceLine(g, x_gap_high, y_edge_low, x_gap_high, y_gap_high);

		drawEcalFaceLine(g, x_edge_low, y_edge_low, x_edge_low, y_edge_high);
		drawEcalFaceLine(g, x_edge_high, y_edge_low, x_edge_high, y_edge_high);
		

		drawEcalFaceLine(g, x_gap_low, -y_edge_low, x_gap_low, -y_gap_high);
		drawEcalFaceLine(g, x_gap_high, -y_edge_low, x_gap_high, -y_gap_high);

		drawEcalFaceLine(g, x_edge_low, -y_edge_low, x_edge_low, -y_edge_high);
		drawEcalFaceLine(g, x_edge_high, -y_edge_low, x_edge_high, -y_edge_high);
	
	}
	
	void drawEcalOutline(Graphics g){
		g.setColor(Color.BLACK);
		double x_edge_low = -276.50;
		double x_edge_high = 361.55;
		double y_edge_low = 20.17;
		double y_edge_high = 89;

		double x_gap_low = -93.30;
		double x_gap_high = 28.93;
		double y_gap_high = 33.12;
		double x1,y1, x2, y2;
		double nPoints = 500;
		for(int i = 0; i< nPoints-1; i++){
			x1 = x_gap_high+i/nPoints*(x_edge_high-x_gap_high);
			x2 = x_gap_high+(i+1)/nPoints*(x_edge_high-x_gap_high);
			y1 = y_edge_low;
			y2 = y1;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
			
			
			x1 = x_edge_low+i/nPoints*(x_gap_low-x_edge_low);
			x2 = x_edge_low+(i+1)/nPoints*(x_gap_low-x_edge_low);
			y1 = y2 = y_edge_low;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
			
			
			
			x1 = x_gap_low+i/nPoints*(x_gap_high-x_gap_low);
			x2 = x_gap_low+(i+1)/nPoints*(x_gap_high-x_gap_low);
			y1 = y2 = y_gap_high;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
			
			x1 = x_edge_low+i/nPoints*(x_edge_high-x_edge_low);
			x2 = x_edge_low+(i+1)/nPoints*(x_edge_high-x_edge_low);
			y1 = y2 = y_edge_high;
			drawEcalFaceLine(g, x1, y1, x2, y2);
			drawEcalFaceLine(g, x1, -y1, x2, -y2);
		}
		drawEcalFaceLine(g, x_gap_low, y_edge_low, x_gap_low, y_gap_high);
		drawEcalFaceLine(g, x_gap_high, y_edge_low, x_gap_high, y_gap_high);

		drawEcalFaceLine(g, x_edge_low, y_edge_low, x_edge_low, y_edge_high);
		drawEcalFaceLine(g, x_edge_high, y_edge_low, x_edge_high, y_edge_high);
		

		drawEcalFaceLine(g, x_gap_low, -y_edge_low, x_gap_low, -y_gap_high);
		drawEcalFaceLine(g, x_gap_high, -y_edge_low, x_gap_high, -y_gap_high);

		drawEcalFaceLine(g, x_edge_low, -y_edge_low, x_edge_low, -y_edge_high);
		drawEcalFaceLine(g, x_edge_high, -y_edge_low, x_edge_high, -y_edge_high);
	}
	
	CustomBinning binning;
	
	ShowCustomBinning(File file) throws FileNotFoundException{
		this.binning = new CustomBinning(file);
		print(this.binning);
	}
	Color altBin1 = new Color(0, 0, 128);
	Color altBin2 = new Color(0,128,0);
	void drawCustomBinRectangles(Graphics g){
		for(int i = 0; i<binning.nTheta; i++){
			g.setColor(i%2 == 0 ? altBin1 : altBin2);
			for(int j = 0; j<binning.phiMax[i].length; j++){
				double phi1 = binning.phiMax[i][j];
				double phi2 = binning.phiMin[i][j];
				double theta1 = binning.thetaMin[i];
				double theta2 = binning.thetaMax[i];
				
				int x =getX(theta1)+1, y = getY(phi1), w =  getX(theta2)-getX(theta1), h = getY(phi2)-getY(phi1);
				g.drawRect(x, y, w, h);
				 x =getX(theta1)+1; y = getY(-phi2); w =  getX(theta2)-getX(theta1); h = getY(-phi1)-getY(-phi2);

				g.drawRect(x, y, w, h);
				
				
			}
		}
	}
	
	
	void drawEcalFaceLine(Graphics g, double x1, double y1, double x2, double y2){

		double[] polar1 = EcalUtil.getThetaPhiSpherical(x1, y1);
		double[] polar2 = EcalUtil.getThetaPhiSpherical(x2, y2);
		g.drawLine(getX(polar1[0]), getY(polar1[1]), getX(polar2[0]), getY(polar2[1]));
		
	}
	int getX(double theta){
		return (int)(this.getWidth()*theta/.3);
	}
	int getY(double phi){
		return (int)(this.getHeight()*(3.2-phi)/6.4);
	}
	static void print(CustomBinning binning){
		System.out.println("  Bin \\# &	$\\theta_{\\textrm{min}}$ &	$\\theta_{\\textrm{max}}$ & $\\phi_{\\textrm{min 1}}$ &	$\\phi_{\\textrm{max 1}}$ & $\\phi_{\\textrm{min 2}}$ & $\\phi_{\\textrm{max 2}}$ & Solid angle  \\\\");
		for(int i = 0; i<binning.nTheta; i++){
			if(binning.phiMax[i].length == 1)
				System.out.printf("%d & %.0f & %.0f & %.0f & %.0f & -- & -- & %.0f \\\\\n",
					i+1,
					binning.thetaMin[i]*1000,
					binning.thetaMax[i]*1000,
					binning.phiMin[i][0]*1000,
					binning.phiMax[i][0]*1000.,
					binning.getSteradians(i)*1e6);
			if(binning.phiMax[i].length == 2)
				System.out.printf("%d & %.0f & %.0f & %.0f & %.0f & %.0f & %.0f & %.0f \\\\\n",
					i+1,
					binning.thetaMin[i]*1000,
					binning.thetaMax[i]*1000,
					binning.phiMin[i][0]*1000,
					binning.phiMax[i][0]*1000,
					binning.phiMin[i][1]*1000,
					binning.phiMax[i][1]*1000,
					binning.getSteradians(i)*1e6);
		}
		System.out.println("total " + binning.getTotSteradians()*1e6 + " microsteradians");
	} 
}
