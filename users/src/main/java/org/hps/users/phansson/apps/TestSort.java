package org.hps.users.phansson.apps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestSort {

    public static class CmpClass implements Comparable<CmpClass> {
        private int _val;
        public CmpClass(int i) {
            _val = i;
        }
        public void set(int i) {
            _val = i;
        }
        public int get() {
            return _val;
        }

        @Override
        public int compareTo(CmpClass o) {
            return o._val-_val;
        }        
    
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        List<CmpClass> list = new ArrayList<CmpClass>();
        list.add(new CmpClass(1));
        list.add(new CmpClass(2));
        list.add(new CmpClass(3));

        System.out.printf("list:\n");
        
        for (CmpClass c : list) {
            System.out.printf("%d\n",c._val);
        }

        Collections.sort(list);

        System.out.printf("after sort:\n");
        
        for (CmpClass c : list) {
            System.out.printf("%d\n",c._val);
        }

        list.get(1).set(4);

        System.out.printf("list:\n");
       
        for (CmpClass c : list) {
            System.out.printf("%d\n",c._val);
        }

        Collections.sort(list);

        System.out.printf("after sort:\n");
        
        for (CmpClass c : list) {
            System.out.printf("%d\n",c._val);
        }

    
    }
}
