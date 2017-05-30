package org.hps.recon.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hps.conditions.ecal.EcalCrystalPosition;
import org.hps.conditions.ecal.EcalCrystalPosition.EcalCrystalPositionCollection;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

public class SnapToEdge {
 //parameters for the snap-to-edge algorithm
    
    private double x_outer_bot[];
    private double y_outer_bot[];
    private double x_inner_bot[];
    private double y_inner_bot[];
    private double x_outer_top[];
    private double y_outer_top[];
    private double x_inner_top[];
    private double y_inner_top[];
    
    public void loadEdges(EcalCrystalPositionCollection positions){
        int nCol = 46;
        x_outer_bot= new double[nCol];
        x_inner_bot= new double[nCol];
        y_outer_bot= new double[nCol];
        y_inner_bot= new double[nCol];
        x_outer_top= new double[nCol];
        x_inner_top= new double[nCol];
        y_outer_top= new double[nCol];
        y_inner_top= new double[nCol];
        
        
        double tolerance = 2;
        List<Double> xs = new ArrayList<Double>();
        for(EcalCrystalPosition pos : positions){

            double cx = pos.getFrontX();
            boolean duplicate = false;
            for(double x : xs){
                if(Math.abs(cx-x)<tolerance){
                    duplicate = true;
                    break;
                }
            }
            if(!duplicate)
                xs.add(cx);
            
        }
        
        Collections.sort(xs);
        
        for(EcalCrystalPosition pos : positions){
            
            
            double cx = pos.getFrontX();
            double cy = pos.getFrontY();
            
            int ix = -1;
            for(int i = 0; i< xs.size(); i++){
                if(Math.abs(xs.get(i)-cx)<tolerance ){
                    ix = i;
                    break;
                }
            }
            
            
            
            
            if(cy < 0) {
                if(y_outer_bot[ix] == 0 || y_outer_bot[ix]>cy){
                    y_outer_bot[ix] = cy;
                    x_outer_bot[ix] = cx;        
                }
                if(y_inner_bot[ix] == 0 || y_inner_bot[ix]<cy){
                    y_inner_bot[ix] = cy;
                    x_inner_bot[ix] = cx;        
                }
            }
            if(cy>0) {
                if(y_outer_top[ix] == 0 || y_outer_top[ix]<cy){
                    y_outer_top[ix] = cy;
                    x_outer_top[ix] = cx;        
                }
                if(y_inner_top[ix] == 0 || y_inner_top[ix]>cy){
                    y_inner_top[ix] = cy;
                    x_inner_top[ix] = cx;        
                }
            }
        }
        
        /*System.out.println(Arrays.toString(x_inner_top));
        System.out.println(Arrays.toString(y_inner_top));
        System.out.println(Arrays.toString(x_inner_bot));
        System.out.println(Arrays.toString(y_inner_bot));*/
    }

    public Hep3Vector snapToEdge(Hep3Vector tPos) {
        double ty = tPos.y();
        double tx = tPos.x();

        
        Double snapToY = null;
        
        if(ty > 0)
        {
            double outer_edge = y_outer_top[0];
            double inner_edge = y_inner_top[0];
            for(int i = 0; i< x_outer_bot.length-1; i++){
                if(tx > x_outer_top[i] && tx < x_outer_top[i+1])
                    outer_edge = y_outer_top[i] + (tx-x_outer_top[i])
                                    *(y_outer_top[i+1]-y_outer_top[i])
                                    /(x_outer_top[i+1]-x_outer_top[i]);

                if(tx > x_inner_top[i] && tx < x_inner_top[i+1])
                    inner_edge = y_inner_top[i] + (tx-x_inner_top[i])
                                    *(y_inner_top[i+1]-y_inner_top[i])
                                    /(x_inner_top[i+1]-x_inner_top[i]);
                
            }
            if(ty>outer_edge)
                snapToY = outer_edge;
            if(ty<inner_edge)
                snapToY = inner_edge;
        }
        if(ty < 0)
        {
            double outer_edge = y_outer_bot[0];
            double inner_edge = y_inner_bot[0];
            for(int i = 0; i< x_outer_bot.length-1; i++){
                if(tx > x_outer_bot[i] && tx < x_outer_bot[i+1])
                    outer_edge = y_outer_bot[i] + (tx-x_outer_bot[i])
                                    *(y_outer_bot[i+1]-y_outer_bot[i])
                                    /(x_outer_bot[i+1]-x_outer_bot[i]);

                if(tx > x_inner_bot[i] && tx < x_inner_bot[i+1])
                    inner_edge = y_inner_bot[i] + (tx-x_inner_bot[i])
                                    *(y_inner_bot[i+1]-y_inner_bot[i])
                                    /(x_inner_bot[i+1]-x_inner_bot[i]);
                
            }
            if(ty<outer_edge)
                snapToY = outer_edge;
            if(ty>inner_edge)
                snapToY = inner_edge;
        }
        if(snapToY == null)
            return tPos;
        return new BasicHep3Vector(tPos.x(), snapToY, tPos.z());
    }
}
