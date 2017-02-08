package org.hps.users.phansson.testrun;

public class PolarCount {
    
    private String run;
    private String name;
    private double val;
    private Count count;
    
    
    PolarCount(String n, String r, double v, Count c) {
        this.name = n;
        this.run = r;
        this.val = v;
        this.count = c;
    }
    
    PolarCount(PolarCount c) {
        this.name = c.name();
        this.run = c.run();
        this.val = c.val;
        this.count = c.count();
    }
        
    
    public String name() {
        return this.name;
    }

    public void setName(String str) {
        this.name = str;
    }

    public void setCount(Count c) {
        this.count = c;
    }
    
    public String run() {
        return this.run;
    }

    public double val() {
        return this.val;
    }

    public void addSimple(PolarCount l) {
        this.count.addSimple(l.count().n(), l.count().en());
    }
    
    public Count count() {
        return this.count;
    }
    

    
}
