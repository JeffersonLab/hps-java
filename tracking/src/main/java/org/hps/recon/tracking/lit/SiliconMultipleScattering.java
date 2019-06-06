package org.hps.recon.tracking.lit;

import org.apache.commons.math3.random.EmpiricalDistribution;
/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.lcsim.units.SystemOfUnits;

/**
 * A class to encapsulate the multiple scattering of an electron in 320 microns
 * of silicon as simulated by EGS5
 */
public class SiliconMultipleScattering {

    protected EmpiricalDistribution empiricalDistribution = null;
    protected EmpiricalDistribution empiricalDistribution2 = null;

    protected Map<Integer, EmpiricalDistribution> scatAngleMap = new HashMap<Integer, EmpiricalDistribution>();

    protected File file = null;
    protected URL url = null;
    protected double[] dataArray = null;
    protected final int n = 1000;

    double eLow = 999.;
    double eHigh = -999.;

    public SiliconMultipleScattering() {
        try {
            ObjectInputStream ois = new ObjectInputStream(getClass().getResourceAsStream("SiMSMap.ser"));
            scatAngleMap = (HashMap) ois.readObject();
            Set<Integer> keys = scatAngleMap.keySet();
            for (Integer i : keys) {
                if (i < eLow) {
                    eLow = i;
                }
                if (i > eHigh) {
                    eHigh = i;
                }
            }
            System.out.println("read in Map with " + scatAngleMap.size() + " entries from " + eLow + " to " + eHigh);
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double nextScatteringAngle(double energy) {
        // HPS default units are GeV
        double eMeV = energy / SystemOfUnits.MeV;
        //round energy down to nearest hundred
        eMeV -= eMeV % 100;
        if (eMeV < eLow) {
            eMeV = eLow;
        }
        if (eMeV > eHigh) {
            eMeV = eHigh;
        }
        int key = (int) eMeV;
        //get the empirical distribution which corresponds to this energy bin
        EmpiricalDistribution dist = scatAngleMap.get(key);
        return dist.getNextValue();
    }

    public void setup() {
        empiricalDistribution = new EmpiricalDistribution(n);
//         empiricalDistribution = new EmpiricalDistribution(n, new RandomDataImpl()); // XXX Deprecated API
        url = getClass().getResource("1.056GeV_scatAngle.txt"); //testData.txt");
        final ArrayList<Double> list = new ArrayList<Double>();
        try {
//            empiricalDistribution2 = new EmpiricalDistribution(n);
//             empiricalDistribution2 = new EmpiricalDistribution(n, new RandomDataImpl()); // XXX Deprecated API
            BufferedReader in
                    = new BufferedReader(new InputStreamReader(
                            url.openStream()));
            String str = null;
            while ((str = in.readLine()) != null) {
                list.add(Double.valueOf(str));
            }
            in.close();
            in = null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        dataArray = new double[list.size()];
        int i = 0;
        for (Double data : list) {
            dataArray[i] = data.doubleValue();
            i++;
        }

        try {
            empiricalDistribution.load(url);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println(empiricalDistribution.getSampleStats().getN());
        System.out.println(empiricalDistribution.getSampleStats().getMean());
        System.out.println(empiricalDistribution.getSampleStats().getStandardDeviation());
    }

    public void tstGen() {
        empiricalDistribution.reSeed(1000);
        SummaryStatistics stats = new SummaryStatistics();
        for (int i = 1; i < 1000; i++) {
            stats.addValue(empiricalDistribution.getNextValue());
        }
        System.out.println(stats.getMean());
        System.out.println(stats.getStandardDeviation());
    }

    public void tstSerialization() throws Exception {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream("C:/work/hps/multiplescattering.ser", true);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(empiricalDistribution);
            oos.close();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream("C:/work/hps/multiplescattering.ser");
            ois = new ObjectInputStream(fis);
            empiricalDistribution2 = (EmpiricalDistribution) ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(empiricalDistribution2.getSampleStats().getN());
        System.out.println(empiricalDistribution2.getSampleStats().getMean());
        System.out.println(empiricalDistribution2.getSampleStats().getStandardDeviation());

    }

    public void generatePDFs(List<String> filenames) {
        try {
            for (String s : filenames) {
                File file = new File(s);
                String[] energy = file.getName().split("MeV");
                Integer e = Integer.parseInt(energy[0]);
                System.out.println("processing " + e);

                EmpiricalDistribution dist = new EmpiricalDistribution(1000);
                dist.load(file);
                scatAngleMap.put(e, dist);
            }
            System.out.println("done generating...");
            System.out.println("serializing");
            FileOutputStream fos = new FileOutputStream("C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/SiMSMap.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(scatAngleMap);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readSerializedPDFList() throws Exception {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream("C:/work/hps/SiMSMap.ser");
            ois = new ObjectInputStream(fis);
            scatAngleMap = (HashMap) ois.readObject();
            Set set = scatAngleMap.keySet();
            System.out.println("found " + set.size() + " EmpiricalDistributions");

            Set<Integer> keys = scatAngleMap.keySet();
            System.out.println("found " + keys.size() + " EmpiricalDistributions");
            for (Integer i : keys) {
                System.out.println(i + " MeV");
                EmpiricalDistribution dist = scatAngleMap.get(i);
                System.out.println(dist);
            }
            ois.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
