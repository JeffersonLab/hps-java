package org.hps.users.spaul.fee_old;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.io.File;
import java.io.FileNotFoundException;

public class ShowCustomBinningXY extends ShowCustomBinning{
    ShowCustomBinningXY(File file) throws FileNotFoundException
    {
        super(file);
    }
    @Override
    void drawCustomBinRectangles(Graphics g){
        for(int i = 0; i<binning.nTheta; i++){
            g.setColor(i%2 == 0 ? altBin1 : altBin2);
            for(int j = 0; j<binning.phiMax[i].length; j++){
                double phi1 = binning.phiMax[i][j];
                double phi2 = binning.phiMin[i][j];
                double theta1 = binning.thetaMin[i];
                double theta2 = binning.thetaMax[i];

                double nK = 10;
                for(int sign = -1; sign <=1; sign +=2){

                    Polygon p = new Polygon();
                    for(int k = 0; k< nK; k++){
                        drawLineFromPolar(theta1, sign*(phi1+(phi2-phi1)*k/nK), theta1, sign*(phi1+(phi2-phi1)*(k+1)/nK),g,p);
                    }
                    for(int k = 0; k< nK; k++){
                        drawLineFromPolar(theta1 + (theta2-theta1)*k/nK, sign*phi2, theta1 + (theta2-theta1)*(k+1)/nK, sign*phi2,g,p);
                    }
                    for(int k = 0; k< nK; k++){
                        drawLineFromPolar(theta2, sign*(phi2+(phi1-phi2)*k/nK), theta2, sign*(phi2+(phi1-phi2)*(k+1)/nK),g, p);
                    }
                    for(int k = 0; k< nK; k++){
                        drawLineFromPolar(theta2 + (theta1-theta2)*k/nK, sign*phi1, theta2 + (theta1-theta2)*(k+1)/nK, sign*phi1,g, p);
                    }
                    
                    closePolarFigure(g, i%2 == 0 ? altBin1 : altBin2, i%2 == 0 ? fillBin1 : fillBin2, p);
                }

            }
        }
    }
    
    protected void drawLine(Graphics g, double ux1, double uy1, double ux2, double uy2){
    	
    	ux1+=.0305;
    	ux2+=.0305;
    	
    	g.drawLine(getX(ux1), getY(uy1), getX(ux2), getY(uy2));
    }
    
    private void drawLineFromPolar(double theta1, double phi1, double theta2,
            double phi2, Graphics g, Polygon p) {
        double[] xy1 = toXY(theta1, phi1);
        double[] xy2 = toXY(theta2, phi2);

        if(p == null)
            p = new Polygon();
        /*g.drawLine(getX(xy1[0]), 
                getY(xy1[1]), 
                getX(xy2[0]),
                getY(xy2[1]));*/
        p.addPoint(getX(xy2[0]), getY(xy2[1]));
    }
    private void closePolarFigure(Graphics g, Color outlineColor, Color fillColor, Polygon p){
        g.setColor(fillColor);
        g.fillPolygon(p);
        g.setColor(outlineColor);
        g.drawPolygon(p);
        
    }
    double xtilt =  .0294;
    double ytilt = -.00082;
    double[] toXY(double theta, double phi){
        double ux = Math.cos(phi)*Math.sin(theta)*Math.cos(xtilt)+Math.cos(theta)*Math.sin(xtilt);
        double uy = Math.sin(phi)*Math.sin(theta);
        double uz = Math.cos(theta)*Math.cos(xtilt)-Math.cos(phi)*Math.sin(theta)*Math.sin(xtilt);
        
        double temp = Math.cos(ytilt)*uy+Math.sin(ytilt)*uz;
        uz = Math.cos(ytilt)*uz-Math.sin(ytilt)*uy;
        uy = temp;
        
        double pxpz = ux/uz;
        double pypz = uy/uz;
        return new double[]{pxpz, pypz};
    }
    int getX(double pxpz){
        //return (int)((pxpz -(-.16))/(.34-(-.16))*getWidth());
        return (int)((.32-pxpz)/(.32-(-.18))*getWidth()); 
    }
    int getY(double pypz){
        return getHeight()-(int)((pypz -(-.125))/(.125-(-.125))*getHeight()); 
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
        double x_edge_low = -269.56;
        double x_edge_high = 354.52;
        double y_edge_low = 26.72;
        double y_edge_high = 82;

        double x_gap_low = -99.84;
        double x_gap_high = 33.35;
        double y_gap_high = 40.36;
        double x1,y1, x2, y2;
            x1 = x_gap_high;
            x2 = x_edge_high;
            y1 = y_edge_low;
            y2 = y1;
            drawEcalFaceLine(g, x1, y1, x2, y2);
            drawEcalFaceLine(g, x1, -y1, x2, -y2);
            
            
            x1 = x_edge_low;
            x2 = x_gap_low;
            y1 = y2 = y_edge_low;
            drawEcalFaceLine(g, x1, y1, x2, y2);
            drawEcalFaceLine(g, x1, -y1, x2, -y2);
            
            
            
            x1 = x_gap_low;
            x2 = x_gap_high;
            y1 = y2 = y_gap_high;
            drawEcalFaceLine(g, x1, y1, x2, y2);
            drawEcalFaceLine(g, x1, -y1, x2, -y2);
            
            x1 = x_edge_low;
            x2 = x_edge_high;
            y1 = y2 = y_edge_high;
            drawEcalFaceLine(g, x1, y1, x2, y2);
            drawEcalFaceLine(g, x1, -y1, x2, -y2);
        
        drawEcalFaceLine(g, x_gap_low, y_edge_low, x_gap_low, y_gap_high);
        drawEcalFaceLine(g, x_gap_high, y_edge_low, x_gap_high, y_gap_high);

        drawEcalFaceLine(g, x_edge_low, y_edge_low, x_edge_low, y_edge_high);
        drawEcalFaceLine(g, x_edge_high, y_edge_low, x_edge_high, y_edge_high);
        

        drawEcalFaceLine(g, x_gap_low, -y_edge_low, x_gap_low, -y_gap_high);
        drawEcalFaceLine(g, x_gap_high, -y_edge_low, x_gap_high, -y_gap_high);

        drawEcalFaceLine(g, x_edge_low, -y_edge_low, x_edge_low, -y_edge_high);
        drawEcalFaceLine(g, x_edge_high, -y_edge_low, x_edge_high, -y_edge_high);
    }
    void drawEcalFaceLine(Graphics g, double x1, double y1, double x2, double y2){

        double[] h1 = EcalUtil.toHollysCoordinates(x1, y1, 1.057, 11);
        double[] h2 =  EcalUtil.toHollysCoordinates(x2, y2, 1.057, 11);
        g.drawLine(getX(Math.tan(h1[1])), getY(Math.tan(h1[0])), getX(Math.tan(h2[1])), getY(Math.tan(h2[0])));
        
    }
    
    void drawXAxis(Graphics g){
        //x axis
        g.drawString("px/pz", getX(.30)-10, getY(0) - 45);
        g.drawLine(getX(-.15), getY(0), getX(.30), getY(0));
        for(int i = -15; i<= 30; i++){
            if(i%5 == 0 && i != 0){
                g.drawString(i/100.+"", getX(i/100.)-15, getY(0)-20);
                g.drawLine(getX(i/100.), getY(0), getX(i/100.), getY(0)-15);
            }
            g.drawLine(getX(i/100.), getY(0), getX(i/100.), getY(0)-5);
        }
    }
    void drawYAxis(Graphics g){
        g.drawString("py/pz", getX(0), getY(.1)-20);
        g.drawLine(getX(0), getY(-.1), getX(0), getY(.1));
        for(int i = -10; i<= 10; i++){
            if(i%5 == 0 && i != 0){
                g.drawString(i/100.+"", getX(0)+15, getY(i/100.) + 5);

                g.drawLine(getX(0), getY(i/100.), getX(0) + 10, getY(i/100.));
            }
            if(i == 0){
                //g.drawString(i/10.+"", getX(0)+10, getY(i/10.) - 15);
            }
            g.drawLine(getX(0), getY(i/100.), getX(0) + 5, getY(i/100.));
        }
    }
    public void paint(Graphics g){
        super.paint(g);
        drawBeamspot(g);
    }
    
    void drawBeamspot(Graphics g){
        g.setColor(Color.red);
        int x = getX(xtilt), y = getY(ytilt);
        g.drawLine(x+10, y, x-10, y);
        g.drawLine(x+10, y+1, x-10, y+1);
        g.drawLine(x+10, y-1, x-10, y-1);
        g.drawLine(x, y-10, x, y+10);
        g.drawLine(x+1, y-10, x+1, y+10);
        g.drawLine(x-1, y-10, x-1, y+10);
    }
}
