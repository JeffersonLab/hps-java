/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

/**
 *
 * @author phansson
 */
public class Count {
    
    private String run;
    private String name;
    private double n;
    private double en;
    
    Count(String n, String r, double N, double err) {
        this.name = n;
        this.run = r;
        this.n = N;
        this.en = err;
    }
    
    public String name() {
        return this.name;
    }

    public String run() {
        return this.run;
    }

    public double n() {
        return this.n;
    }

    public double en() {
        return this.en;
    }
    public void addSimple(double N,double eN) {
        //Assume these are just counts so simply add up
        this.n = this.n + N;
        this.en = this.en + eN;
    }

    
}
