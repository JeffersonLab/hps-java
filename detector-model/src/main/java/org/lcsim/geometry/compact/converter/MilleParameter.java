package org.lcsim.geometry.compact.converter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;



public class MilleParameter {
    private static double corrScaleFactor = -1.;
    private int id;
    private double value; 
    private double presigma;
    private static final Map<Integer,String> dMap;
    private static final Map<Integer,String> tMap;
    private static final Map<Integer,String> hMap;
    static {
        dMap = new HashMap<Integer,String>();
        dMap.put(1, "x");dMap.put(2, "y"); dMap.put(3, "z");
        tMap = new HashMap<Integer,String>();
        tMap.put(1, "");tMap.put(2, "r");
        hMap = new HashMap<Integer,String>();
        hMap.put(1, "t");hMap.put(2, "b");
        }
    public static final int half_offset = 10000;
    public static final int type_offset = 1000; 
    public static final int dimension_offset = 100;
    public static enum Type {
        TRANSLATION(1), ROTATION(2);
        private int value;
        private Type(int value) {this.value = value;}
        public int getType() {return this.value;}
    };
    
    public MilleParameter(String line) {
        String[] vals = StringUtils.split(line);// line.split("\\s+");
        if(vals.length <3) {
            System.out.println("this line is ill-formatted (" + vals.length + ")");
            System.out.println(line);
            System.exit(1);
        }
        try {
        //for(String v : vals) System.out.println("\"" + v + "\"");
        setId(Integer.parseInt(vals[0]));
        setValue( corrScaleFactor * Double.parseDouble(vals[1]) );
        setPresigma(Double.parseDouble(vals[2]));
        
        } catch (NumberFormatException e) {
            System.out.println(vals[0] + " " + vals[1] + " " + vals[2]);
            throw new RuntimeException("problem parsing string ", e);
        }
    }
    
    public MilleParameter(int id, double value, double presigma) {
        setId(id);
        setValue(value);
        setPresigma(presigma);
    }
    
    public String getXMLName() {
        String d = dMap.get(getDim());
        String t = tMap.get(getType());
        String h = hMap.get(getHalf());
        int s = getSensor();
        return String.format("%s%s%d%s_align", t,d,s,h);
        
    }

    public int getDim() {
        int h = (int) (getHalf() * half_offset);
        int t = (int) (getType() * type_offset);
        return (int) Math.floor((id- h -t)/(double)dimension_offset);
    }
    
    public int getSensor() {
        int h = (int) (getHalf() * half_offset);
        int t = (int) (getType() * type_offset);
        int d = (int) (getDim() * dimension_offset);
        return (id - h - t -d);
    }

    public int getType() {
        int h = (int) (getHalf() * half_offset);
        return (int) Math.floor((id -h)/(double)type_offset);
    }

    public int getHalf() {
        return (int)Math.floor(id/(double)half_offset);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getPresigma() {
        return presigma;
    }

    public void setPresigma(double presigma) {
        this.presigma = presigma;
    }
    
    public String toString() {
        return String.format("Milleparameter id=%d half=%d type=%d dim=%d sensor=%d value=%f", this.getId(), this.getHalf(), this.getType(), this.getDim(), this.getSensor(), this.getValue());
    }


}