/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.lit;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class SiliconMultipleScatteringTest extends TestCase
{

    public static void testIt() throws Exception
    {

//        generatePDF();
        SiliconMultipleScattering sms = new SiliconMultipleScattering();
        AIDA aida = AIDA.defaultInstance();
        for (int i = 1; i < 24; ++i) {
            double energy = i / 10.; // GeV
            for (int j = 0; j < 100000; ++j) {
                double angle = sms.nextScatteringAngle(energy);
                if (angle < 0.02) {
                    aida.cloud1D("Scattering angle for " + energy).fill(angle);
                }
            }
        }
        try {
            aida.saveAs("SiliconMultipleScatteringTest.aida");
        } catch (IOException exception) {
            throw new RuntimeException("Failed to save AIDA file.", exception);
        }

//        readPDF();
    }

//    public static void testIt()
//    {
//        SiliconMultipleScattering sms = new SiliconMultipleScattering();
//        sms.tstGen();
//        try {
//            sms.tstSerialization();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
    public static void generatePDF()
    {
        String[] fileNames = {
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/100MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/200MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/300MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/400MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/500MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/600MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/700MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/800MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/900MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1000MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1100MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1200MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1300MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1400MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1500MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1600MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1700MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1800MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/1900MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/2000MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/2100MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/2200MeV_10M_scatAngle.txt",
            "C:/work/svn/hps/tracking/src/main/resources/org/hps/recon/tracking/lit/2300MeV_10M_scatAngle.txt"
        };

        List<String> files = Arrays.asList(fileNames);

        SiliconMultipleScattering sims = new SiliconMultipleScattering();
        sims.generatePDFs(files);
    }

    public static void readPDF() throws Exception
    {
        SiliconMultipleScattering sims = new SiliconMultipleScattering();
        sims.readSerializedPDFList();
    }
}
