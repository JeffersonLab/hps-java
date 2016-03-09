package org.hps.users.spaul.feecc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

public class EcalUtil {
    
    static int[] getCrystalIndex(Cluster c){
        if(map == null){
            try{
                readMap();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        c.getPosition();
        int bestix = 0, bestiy = 0;
        double bestdist = 100000;

        double cx = c.getPosition()[0];
        double cy = c.getPosition()[1];
        for(int ix = -23; ix<= 23; ix++){
            if(!map.containsKey(200+ix))
                continue;
            double x = map.get(200+ix)[0];
            if(Math.abs(x-cx)<bestdist){
                bestdist = Math.abs(x-cx);
                bestix = ix;
            }
        }
        
        bestdist = 100000;
        for(int iy = -5; iy<= 5; iy++){
            if(!map.containsKey(100*iy + bestix))
                continue;
            double y = map.get(100*iy + bestix)[1];
            if(Math.abs(y-cy)<bestdist){
                bestdist = Math.abs(y-cy);
                bestiy = iy;
            }
        }
        return new int[]{bestix, bestiy}; 
    }
    
    
    /**
     * Holly's algorithm.  
     * @param x x position of cluster
     * @param y y position of cluster
     * @param e energy of cluster
     * @param pid type of particle (11 = electron; -1 = positron)
     * @return array of doubles
     * <br> [0] theta = atan(py/pz)
     * <br> [1] phi = atan(px/pz) 
     */
    static double [] toHollysCoordinates(double x, double y, double e, int pid){
        if(pid == 11){
            return new double[]{0.00071 * y +0.000357, 0.00071*x + 0.00003055*e + 0.04572/e +0.0006196};
        } else if(pid == -11){
            return new double[]{0.00071 * y +0.000357, 0.00071*x - 0.0006465*e + 0.045757/e +0.003465};
        }
        return null;
    }
    
    static double getSteradians(double x, double y, double e,int pid, double dA){
        double thetaPhi[] = toHollysCoordinates(x, y, e, pid);
        return .00071*.00071*dA/Math.sqrt(thetaPhi[0]*thetaPhi[0]+ thetaPhi[1]*thetaPhi[1]+1);
    }
    
    static boolean isSeedEdge(Cluster c){
        CalorimeterHit seedhit  = (CalorimeterHit)c.getCalorimeterHits().get(0);
        //  seedhit
        int ix = seedhit.getIdentifierFieldValue("ix");
        int iy = seedhit.getIdentifierFieldValue("iy");

        //seedhit.get
        return isEdge(ix, iy);
    }
    static boolean isEdge(int ix, int iy){
        if(iy == 5 || iy == 1 || iy == -1 || iy == -5)
            return true;
        if(ix == -23 || ix == 23)
            return true;
        if((iy == 2 || iy == -2) && (ix >=-11 && ix <= -1))
            return true;
        return false;
    }
    public static boolean fid_ECal(Cluster c){
        return fid_ECal(c.getPosition()[0], c.getPosition()[1]);
    }
    public static boolean fid_ECal(double x, double y)
    {
        y = Math.abs(y);

        boolean in_fid = false;
        double x_edge_low = -262.74;
        double x_edge_high = 347.7;
        double y_edge_low = 33.54;
        double y_edge_high = 75.18;

        double x_gap_low = -106.66;
        double x_gap_high = 42.17;
        double y_gap_high = 47.18;

        y = Math.abs(y);

        if( x > x_edge_low && x < x_edge_high && y > y_edge_low && y < y_edge_high )
        {
            if( !(x > x_gap_low && x < x_gap_high && y > y_edge_low && y < y_gap_high) )
            {
                in_fid = true;
            }
        }

        return in_fid;
    }
    /**
     * 
     * @param x
     * @param y
     * @param d the additional distance from the edge of the ecal in addition
     *       to what is required by fid_Cal(double, double)
     * @return
     */
    public static boolean fid_ecal_more_strict(double x, double y, double d){
        y = Math.abs(y);

        boolean in_fid = false;
         
        double x_edge_low = -262.74 + d;
        double x_edge_high = 347.7 - d;
        double y_edge_low = 33.54 + d;
        double y_edge_high = 75.18 - d;

        double x_gap_low = -106.66 - d;
        double x_gap_high = 42.17 + d;
        double y_gap_high = 47.18 + d;

        y = Math.abs(y);

        if( x > x_edge_low && x < x_edge_high && y > y_edge_low && y < y_edge_high )
        {
            if( !(x > x_gap_low && x < x_gap_high && y > y_edge_low && y < y_gap_high) )
            {
                in_fid = true;
            }
        }

        return in_fid;
    }
    static double[] toSphericalFromBeam(double pxpz, double pypz){
        double x = pxpz, y = pypz, z = 1;
        double beamTilt = .03057;
        double xtemp = Math.cos(beamTilt)*x - Math.sin(beamTilt)*z;
        double ztemp = Math.cos(beamTilt)*z + Math.sin(beamTilt)*x;
        double ytemp = y;
        
        double theta = Math.atan(Math.hypot(xtemp, ytemp)/ ztemp);
        double phi = Math.atan2(ytemp, xtemp);
        
        
        return new double[]{theta, phi};
    }
    static Map<Integer, double[]> map ;
    static void readMap() throws FileNotFoundException{
        Scanner s = new Scanner(new File(System.getenv("HOME") + "/ecal_positions.txt"));
        map = new HashMap();
        
        while(s.hasNext()){
            int ix =s.nextInt();
            int iy = s.nextInt();
            double x = s.nextDouble();
            double y =s.nextDouble();
            map.put(ix+100*iy, new double[]{x, y});
            
        }
        s.close();
    }
    static double getArea(int ix, int iy){
        int ixp = ix+1;
        if(ixp == 0)
            ixp = 1;
        int ixm = ix-1;
        if(ixm == 0)
            ixm = -1;
        double[] plus = map.get(ixp+100*(iy+1));
        double[] minus = map.get(ixm+100*(iy-1));
        return (plus[0]-minus[0])*(plus[1]-minus[1])/4;
    }
    
    public static double[] getThetaPhiSpherical(double x,double y){
        double hcoord[] = toHollysCoordinates(x,y, 1.056, 11);
        return toSphericalFromBeam(Math.tan(hcoord[1]),Math.tan(hcoord[0]));
    }
    /*beam tilt*/
    static double tilt = .03057;
    //assuming FEE electron.

    public static double[] getXY(double theta, double phi){
        
        double ux = Math.cos(phi)*Math.sin(theta)*Math.cos(tilt)+Math.cos(theta)*Math.sin(tilt);
        double uy = Math.sin(phi)*Math.sin(theta);
        double uz = Math.cos(theta)*Math.cos(tilt)-Math.cos(phi)*Math.sin(theta)*Math.sin(tilt);
        double pxpz = ux/uz;
        double pypz = uy/uz;
        //holly's coordinates:
        double h1 = Math.atan(pypz);
        double h2 = Math.atan(pxpz);
        //0.00071 * y +0.000357,
        //0.00071*x + 0.00003055*e + 0.04572/e +0.0006196
        double y = (h1-0.000357)/0.00071;
        double e = 1.056;
        double x = (h2 - 0.00003055*e - 0.04572/e -0.0006196)/0.00071;
        return new double[]{x,y};
    }
    public static boolean fid_ECal_spherical(double theta, double phi){
        double[] xy = getXY(theta, phi);
        double x = xy[0];
        double y = xy[1];
        return fid_ECal(x, y);
    }
    public static boolean fid_ECal_spherical_more_strict(double theta, double phi, double d){
        double[] xy = getXY(theta, phi);
        double x = xy[0];
        double y = xy[1];
        return fid_ecal_more_strict(x, y, d);
    }
    public static void main(String arg[]){
        double x = 0, y = 0;
        double sp[] = getThetaPhiSpherical(x,y); 
        System.out.println(Arrays.toString(getXY(sp[0], sp[1])));
    }
}
    

