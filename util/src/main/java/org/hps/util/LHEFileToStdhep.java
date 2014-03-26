/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.util;

/**
 *
 * @author richp
 * modified by mbussonn to take arguments from command line
 */
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

import hep.io.stdhep.StdhepBeginRun;
import hep.io.stdhep.StdhepEndRun;
import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import java.util.Random;

public class LHEFileToStdhep {

    /**
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
//        String file = args[0];
        String file = "/nfs/sulky21/g.ec.u12/users/mgraham/DarkPhoton/SignalEvents/6W_0.200_A_unweighted_events.lhe";
//        String file = "/nfs/sulky21/g.ec.u12/users/mgraham/DarkPhoton/SignalEvents/6GeV_W_0.500_unweighted_events.lhe";
        String infile = file;
        String outfile = file.substring(0, file.length() - 4) + "_100u_beamspot.stdhep";//replace .txt by .stdhep
        System.out.println("==== processing " + infile + " into " + outfile + " ====");
        process(infile, outfile);
    }

    private static void process(String infile, String outfile) throws IOException {
//        try {
        Random generator = new Random();
        double _xoff = 0.03;
        double _yoff = 0;
        double _zoff = 0;
        double sigx = 0.0;        // Luminous region size in x (mm)
        double sigy = 0.1;        // Luminous region size in y (mm)
        double sigz = 0.1;         // Luminous region size in z (mm)

        FileReader fr = new FileReader(infile);
        StreamTokenizer tok = new StreamTokenizer(fr);
        tok.resetSyntax();
        tok.wordChars(33, 255);
        tok.parseNumbers();

        tok.whitespaceChars(0, ' ');
        tok.eolIsSignificant(true);
        double nevts = -999;
        while (nevts == -999) {
            nevts = getNumberOfEvents(tok);
        }
        System.out.println("Found " + nevts + "events");
//        return;

        //List<Double> beg = getNumbersInLine(tok);
        //if (beg.size() != 7) {
        //throw new RuntimeException("Unexpected entry in begin run record");
        //}
        int nreq = (int) nevts;
        int ngen = (int) nevts;
        int nwrit = (int) nevts;
        float ecm = (float) 6.0;
        float xsec = (float) 99999997952.;
        double rn1 = 12345321;
        double rn2 = 66666666;
        StdhepWriter sw = new StdhepWriter(outfile, "Imported Stdhep Events",
                "From file " + infile, nwrit);
        sw.setCompatibilityMode(false);
        StdhepBeginRun sb = new StdhepBeginRun(nreq, ngen, nwrit, ecm, xsec, rn1, rn2);
        sw.writeRecord(sb);


        tok.resetSyntax();
        tok.wordChars(33, 255);
        tok.wordChars('0', '9');        // java.io.StreamTokenizer fails to parse
        tok.wordChars('e', 'e');        // scientific notation like "1.09E-008".
        tok.wordChars('E', 'E');        // The solution is to read and parse
        tok.wordChars('.', '.');        // coordinates as "words".
        tok.wordChars('+', '+');        // You run into trouble if the input file
        tok.wordChars('-', '-');        // contains text with "e" or "E" which is
        tok.whitespaceChars(0, ' ');
        tok.eolIsSignificant(true);


        double[] beam = {0, 0, 0, 0};
        int nevhep = 0;
        for (int icross = 0; icross < nwrit; icross++) {
            beam[0] = sigx * generator.nextGaussian() + _xoff;
            beam[1] = sigy * generator.nextGaussian() + _yoff;
            beam[2] = sigz * generator.nextGaussian() + _zoff;
            getToNextEvent(tok);
            List<Double> nums = getNumbersInLine(tok);
//            System.out.println(nums.toString());

            if (nums.size() != 6) {
                throw new RuntimeException("Unexpected entry for number of particles");
            }
            nevhep++;
            int nhep = nums.get(0).intValue();
            System.out.println("Number of particles for event " + nevhep + ": " + nhep);


            int isthep[] = new int[nhep];
            int idhep[] = new int[nhep];
            int jmohep[] = new int[2 * nhep];
            int jdahep[] = new int[2 * nhep];
            double phep[] = new double[5 * nhep];
            double vhep[] = new double[4 * nhep];
            int i = 0;
            for (int npart = 0; npart < nhep; npart++) {
                List<Double> vals = getNumbersInLine(tok);
                if (vals.size() != 13) {
                    throw new RuntimeException("Unexpected entry for a particle");
                }
                if (vals.get(1).intValue() != 9) {//ignore the vertex for now

//                int ip = vals.get(0).intValue();
//                if (ip != i + 1) {
//                    throw new RuntimeException("Particle numbering mismatch");
//                }
                    isthep[i] = vals.get(1).intValue();

                    if (vals.get(1).intValue() == -1) {
                        isthep[i] = 3;
                    }


                    idhep[i] = vals.get(0).intValue();
                    if (idhep[i] == 611) {
                        idhep[i] = 11;
                    }
                    if (idhep[i] == -611) {
                        idhep[i] = -11;
                    }
                    jmohep[2 * i] = vals.get(2).intValue();
                    jmohep[2 * i + 1] = vals.get(3).intValue();
                    jdahep[2 * i] = vals.get(4).intValue();
                    jdahep[2 * i + 1] = vals.get(5).intValue();
                    for (int j = 0; j < 5; j++) {
                        phep[5 * i + j] = vals.get(j + 6);
                    }

                    for (int j = 0; j < 4; j++) {
//                    vhep[4 * i + j] = vals.get(j + 12);

                        vhep[4 * i + j] = beam[j];

                    }
                    // swap x and z axes...
                    double px = phep[5 * i + 0];
                    double pz = phep[5 * i + 2];
                    phep[5 * i + 0] = pz;
                    phep[5 * i + 2] = px;
                    //                                if (i == 0 || i == nhep - 1) {
                    System.out.println(i + " st: " + isthep[i] + " id: " + idhep[i] +
                            " jmo: " + jmohep[2 * i] + " " + jmohep[2 * i + 1] +
                            " jda: " + jdahep[2 * i] + " " + jdahep[2 * i + 1]);
                    System.out.println("p: " + phep[5 * i] + " " + phep[5 * i + 1] + " " +
                            phep[5 * i + 2] + " " + phep[5 * i + 3] + " " + phep[5 * i + 4]);
                    System.out.println("v: " + vhep[4 * i] + " " + vhep[4 * i + 1] + " " +
                            vhep[4 * i + 2] + " " + vhep[4 * i + 3]);
//                                }
                    i++;
                }
            }
            StdhepEvent ev = new StdhepEvent(nevhep, nhep, isthep, idhep, jmohep, jdahep, phep, vhep);
            sw.writeRecord(ev);
        }
//        List<Double> end = getNumbersInLine(tok);
//        if (end.size() != 7) {
//            throw new RuntimeException("Unexpected entry in begin run record");
//        }

        int nreqe = (int) nevts;
        int ngene = (int) nevts;
        int nwrite = (int) nevts;
        float ecme = (float) 6.0;
        float xsece = (float) 99999997952.;
        double rn1e = 12345321;
        double rn2e = 66666666;
        StdhepEndRun se = new StdhepEndRun(nreqe, ngene, nwrite, ecme, xsece, rn1e, rn2e);
        sw.writeRecord(se);

        sw.close();
        fr.close();
        //        } catch (IOException e) {
        //             System.out.println("Error opening " + infile);
        //        }
        System.out.println("Ok...done!");
    }

    @SuppressWarnings("static-access")
    private static List<Double> getNumbersInLine(StreamTokenizer tok) throws IOException {
        List<Double> nums = new ArrayList<Double>();
        while (tok.nextToken() != tok.TT_EOF) {
//            System.out.println(tok.toString());
            if (tok.ttype == tok.TT_EOL) {
                break;
            }

//            if (tok.ttype != tok.TT_NUMBER) {
//                throw new RuntimeException("Non numeric data encountered");
//            }

            String tokVal = tok.sval;
            nums.add(Double.valueOf(tokVal).doubleValue());
        }

        return nums;
    }

    @SuppressWarnings("static-access")
    private static double getNumberOfEvents(StreamTokenizer tok) throws IOException {
        boolean fndNumber = false;
        boolean fndOf = false;
        boolean fndEvents = false;
        double evts = -999;
        while (tok.nextToken() != tok.TT_EOF) {
//            System.out.println(tok.toString());
            if (tok.ttype == tok.TT_EOL) {
                break;
            }
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Number")) {
                fndNumber = true;
            }
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("of")) {
                fndOf = true;
            }
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Events")) {
                fndEvents = true;
            }
            if (tok.ttype == tok.TT_NUMBER && fndEvents && fndOf && fndNumber) {
                evts = tok.nval;
            }
        }
        return evts;
    }

    @SuppressWarnings("static-access")
    private static void getToNextEvent(StreamTokenizer tok) throws IOException {
        while (tok.nextToken() != tok.TT_EOF) {
//            System.out.println(tok.toString());           
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("<event>")) {
                tok.nextToken();//get to the EOL
                return;
            }
        }

    }
}
