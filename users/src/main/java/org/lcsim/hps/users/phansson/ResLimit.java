/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



/**Map< Integer, Map<Integer
 *
 * @author phansson
 */
public class ResLimit {
    
    Map< Integer, Map<Integer,Double[]> >  map_top = new HashMap< Integer, Map<Integer,Double[]> >();
    Map< Integer, Map<Integer,Double[]> >  map_bottom = new HashMap< Integer, Map<Integer,Double[]> >();
    
    
    public void ResLimit() {
            
    }
    
    
    public void add(int side, int layer,int dir,double xmin,double xmax) {
        Map< Integer, Map<Integer,Double[]> >  map;
        if(side==0) {
            map = map_top;
        } else {
            map = map_bottom;
        }
        if(map.containsKey(layer)) {
            Double v[] = {xmin,xmax};        
            map.get(layer).put(dir,v);
        } else {
            map.put(layer,new HashMap<Integer,Double[]>());
            add(side,layer,dir,xmin,xmax);
        }
        
        
    }
    
    public double getMin(int side,int l,int d) {
        return getLim(side,l,d)[0];
        
    }

    public double getMax(int side,int l,int d) {
        return getLim(side,l,d)[1];
        
    }

    public Double[] getLim(int side,int l,int d) {
        Map< Integer, Map<Integer,Double[]> >  map;
        if(side==0) {
            map = map_top;
        } else {
            map = map_bottom;
        }
        if(!map.containsKey(l)) {
            System.out.println("ERROR this layer " + l + " doesn't exist in ResLimit!");
            System.exit(1);
        }
        if(!map.get(l).containsKey(d)) {
            System.out.println("ERROR this layer " + l + " doesn't have direction " + d + " in ResLimit!");
            System.exit(1);
        }
        return map.get(l).get(d);
    }
    
    public void print() {
        System.out.print("---Residual limits:");
        for(int side=0;side<2;++side) {
            System.out.print("Side: " + side);
            Map< Integer, Map<Integer,Double[]> >  map;
            if(side==0) {
                map = map_top;
            } else {
                map = map_bottom;
            }
            Iterator it = map.keySet().iterator();
            System.out.printf("%5s %5.1d %5.1d \n","Layer","Dir","Min","Max");
        
        while(it.hasNext()) {
            Integer l = (Integer)it.next();
            Map<Integer,Double[]> m = map.get(l);
            Iterator itt = m.keySet().iterator();
            while(itt.hasNext()) {
                Integer dir = (Integer)itt.next();
                Double[] lim = m.get(dir);
                System.out.printf("%5d %5d %5.1f %5.1f \n",l,dir,lim[0],lim[1]);
                
            }
        }
        }
    }
    
    
    
}
