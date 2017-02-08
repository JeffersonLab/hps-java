package org.hps.users.phansson.alignment;

import java.util.ArrayList;
import java.util.List;

public class GlobalParameters {

    //private List<GlobalParameter> _gps;
    
    public static final int NGL = 10;
    private List<GlobalParameter> _gps = new ArrayList<GlobalParameter>(); 
    public static final int[] _types = {1000,2000};
    public static final int[] _sides = {10000,20000};
    public static final int[] _dirs = {100,200,300};
    
    public GlobalParameters() {
        //initialize the parameters
        //Naming scheme:
        //[Top]:
        // 10000 = top
        // 20000 = bottom
        //[Type]: 
        // 1000 - translation
        // 2000 - rotation
        //[Direction] (tracker coord. frame)
        // 100 - x (beamline direction)
        // 200 - y (non-measurement plane / bend plane)
        // 300 - z (measurement direction)
        // [Layer]
        // 1-10
              
        boolean ON;
        for(int iside=0;iside<2;++iside) {
            for(int ilayer=1;ilayer<11;++ilayer) {
                for (int itype=0;itype<2;++itype) {
                    for (int idir=0;idir<3;++idir) {
                        //if(_sides[iside]==10000 && ilayer==3 && _types[itype]==1000 && _dirs[idir]==300) ON=true;
                        //if(ilayer==3 && _types[itype]==1000 && _dirs[idir]==300) ON=true;
                        ON =true;
                        //if(_types[itype]==1000 && _dirs[idir]==300) ON=true;
                        //else ON=false;
                        GlobalParameter gp = new GlobalParameter("test",_sides[iside],ilayer,_types[itype],_dirs[idir],ON);
                        _gps.add(gp);
                    }
                }
            }
        }
    }
        
        
    

    

    public List<GlobalParameter> getList() {
        return _gps;
    }
    
    public void print() {
        System.out.println("---- Global parameters ("+ _gps.size() + ")");
        for(GlobalParameter p : _gps) {
            p.print();
        }
        System.out.println("----");
    }
    
}
