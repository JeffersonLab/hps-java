package org.hps.users.spaul.fee_old;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;
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
        Canvas c = new ShowCustomBinning(new File(arg[0]));
        String outdir = arg[1];
        frame.add(c);
        frame.setSize(1200, 800);
        frame.setVisible(true);
        
        
        try {
            BufferedImage im = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
            c.paint(im.getGraphics());
            ImageIO.write(im, "PNG", new File(outdir +"/bins.png"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        c = new ShowCustomBinningXY(new File(arg[0]));
        frame.add(c);
        frame.setSize(1200, 615);
        frame.setVisible(true);
        
        try {
            BufferedImage im = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
            c.paint(im.getGraphics());
            ImageIO.write(im, "PNG", new File(outdir + "/bins_xy.png"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void paint(Graphics g){
        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
        
        drawEcalOutline(g);
        drawFidEcalOutline(g);
        drawSVTOutline5(g);
        drawCustomBinRectangles(g);
        g.setColor(Color.BLACK);
        drawXAxis(g);
        drawYAxis(g);
    }
    
    private void drawSVTOutline5(Graphics g) {
    	g.setColor(Color.RED);
		double x_edge_high = 0.160;
		double y_edge_low =  0.008;
		double y_edge_high = 0.059;
		double x_edge_low = -.119;
		
		double ux1,uy1, ux2, uy2;
        double nPoints = 200;
        for(int i = 0; i< nPoints-1; i++){
            ux1 = x_edge_high;
            ux2 = x_edge_high;
            uy1 = y_edge_low + i*(y_edge_high-y_edge_low)/nPoints;
            uy2 = y_edge_low+ (i+1)*(y_edge_high-y_edge_low)/nPoints;
            drawLine(g, ux1, uy1, ux2, uy2);
            drawLine(g, ux1, -uy1, ux2, -uy2);
            
            ux1 = x_edge_low;
            ux2 = x_edge_low;
            uy1 = y_edge_low + i*(y_edge_high-y_edge_low)/nPoints;
            uy2 = y_edge_low+ (i+1)*(y_edge_high-y_edge_low)/nPoints;
            drawLine(g, ux1, uy1, ux2, uy2);
            drawLine(g, ux1, -uy1, ux2, -uy2);
            
            ux1 = x_edge_low + i*(x_edge_high-x_edge_low)/nPoints;
            ux2 = x_edge_low + (i+1)*(x_edge_high-x_edge_low)/nPoints;
            uy1 = y_edge_low;
            uy2 = y_edge_low;
            drawLine(g, ux1, uy1, ux2, uy2);
            drawLine(g, ux1, -uy1, ux2, -uy2);
            
            ux1 = x_edge_low + i*(x_edge_high-x_edge_low)/nPoints;
            ux2 = x_edge_low + (i+1)*(x_edge_high-x_edge_low)/nPoints;
            uy1 = y_edge_high;
            uy2 = y_edge_high;
            drawLine(g, ux1, uy1, ux2, uy2);
            drawLine(g, ux1, -uy1, ux2, -uy2);
        }
	}
        
    protected void drawLine(Graphics g, double ux1, double uy1, double ux2, double uy2){
    	double theta1 = Math.atan(Math.hypot(ux1, uy1));
    	double theta2 = Math.atan(Math.hypot(ux2, uy2));
    	
    	double phi1 = Math.atan2(uy1, ux1);
    	double phi2 = Math.atan2(uy2, ux2);
    	
    	g.drawLine(getX(theta1), getY(phi1), getX(theta2), getY(phi2));
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
        /*double x_edge_low = -262.74;
        double x_edge_high = 347.7;
        double y_edge_low = 33.54;
        double y_edge_high = 75.18;

        double x_gap_low = -106.66;
        double x_gap_high = 42.17;
        double y_gap_high = 47.18;*/
        
        g.setColor(Color.BLACK);
        double x_edge_low = -269.56;
        double x_edge_high = 354.52;
        double y_edge_low = 26.72;
        double y_edge_high = 82;

        double x_gap_low = -99.84;
        double x_gap_high = 33.35;
        double y_gap_high = 40.36;
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
    Color fillBin1 = new Color(196, 196, 255);
    Color fillBin2 = new Color(196,255,196);
    void drawCustomBinRectangles(Graphics g){
        for(int i = 0; i<binning.nTheta; i++){
            g.setColor(i%2 == 0 ? altBin1 : altBin2);
            for(int j = 0; j<binning.phiMax[i].length; j++){
                double phi1 = binning.phiMax[i][j];
                double phi2 = binning.phiMin[i][j];
                double theta1 = binning.thetaMin[i];
                double theta2 = binning.thetaMax[i];
                
                int x =getX(theta1)+1, y = getY(phi1), w =  getX(theta2)-getX(theta1), h = getY(phi2)-getY(phi1);

                g.setColor(i%2 == 0 ? fillBin1 : fillBin2);
                g.fillRect(x, y, w, h);
                g.setColor(i%2 == 0 ? altBin1 : altBin2);
                g.drawRect(x, y, w, h);
                 x =getX(theta1)+1; y = getY(-phi2); w =  getX(theta2)-getX(theta1); h = getY(-phi1)-getY(-phi2);
                 g.setColor(i%2 == 0 ? fillBin1 : fillBin2);
                    g.fillRect(x, y, w, h);
                    g.setColor(i%2 == 0 ? altBin1 : altBin2);
                    g.drawRect(x, y, w, h);
            }
            
        }
    }
    void drawXAxis(Graphics g){
        //x axis
        g.drawString("θ", getX(.28), getY(0) - 40);
        g.drawLine(getX(0), getY(0), getX(.28), getY(0));
        for(int i = 0; i< 28; i++){
            if(i%5 == 0 && i != 0){
                g.drawString(i/100.+"", getX(i/100.)-15, getY(0)-20);
                g.drawLine(getX(i/100.), getY(0), getX(i/100.), getY(0)-15);
            }
            g.drawLine(getX(i/100.), getY(0), getX(i/100.), getY(0)-5);
        }
    }
    void drawYAxis(Graphics g){
        g.drawString("φ", getX(0)+70, getY(3));
        g.drawLine(getX(0), getY(-3), getX(0), getY(3));
        for(int i = -30; i<= 30; i++){
            if(i%5 == 0 && i != 0){
                g.drawString(i/10.+"", getX(0)+20, getY(i/10.) + 5);

                g.drawLine(getX(0), getY(i/10.), getX(0) + 15, getY(i/10.));
            }
            if(i == 0){
                //g.drawString(i/10.+"", getX(0)+10, getY(i/10.) - 15);
            }
            g.drawLine(getX(0), getY(i/10.), getX(0) + 5, getY(i/10.));
        }
    }
    
    
    void drawEcalFaceLine(Graphics g, double x1, double y1, double x2, double y2){

        double[] polar1 = EcalUtil.getThetaPhiSpherical(x1, y1);
        double[] polar2 = EcalUtil.getThetaPhiSpherical(x2, y2);
        g.drawLine(getX(polar1[0]), getY(polar1[1]), getX(polar2[0]), getY(polar2[1]));
        
    }
    int getX(double theta){
        return (int)(this.getWidth()*theta/.3)+left_margin;
    }
    int left_margin = 20;
    int getY(double phi){
        return (int)(this.getHeight()*(3.2-phi)/6.4);
    }
    static void print(CustomBinning binning){
        System.out.println("  Bin \\# & $\\theta_{\\textrm{min}}$ & $\\theta_{\\textrm{max}}$ & $\\phi_{\\textrm{min 1}}$ & $\\phi_{\\textrm{max 1}}$ & $\\phi_{\\textrm{min 2}}$ & $\\phi_{\\textrm{max 2}}$ & Solid angle  \\\\");
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
