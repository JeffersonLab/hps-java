package org.hps.analysis.alignment;

import hep.io.stdhep.StdhepBeginRun;
import hep.io.stdhep.StdhepEndRun;
import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Math.sqrt;
import java.util.StringTokenizer;

/**
 * Reads in text file and creates events in stdhep file format.
 *
 * Format: x(mm) y(mm) z(mm) px(GeV) py(GeV) pz(GeV) pdgId
 *
 * @author Norman A. Graf
 * @version 1.0
 */
public class TextToStdhepConverter {

    public static void main(String args[]) throws IOException {
        // remind user of correct usage
        if (args.length < 1) {
            usage();
        }
        if (args.length == 1 && args[0].equals("-h")) {
            usage();
        }

        File listOfInputFiles = new File(args[0]);
        if (!listOfInputFiles.exists()) {
            System.out.println("\n\n  Input File " + listOfInputFiles + " does not exist!");
            System.exit(1);
        }
        FileInputStream fin1 = new FileInputStream(args[0]);

        File outputDir = new File(".");
        if (args.length > 1) {
            outputDir = new File(args[1]);
        }
        // check if output directory exists
        if (!outputDir.exists()) {
            System.out.println("\n\n  Directory " + outputDir + " does not exist!");
            System.exit(1);
        }

        /*--------------------------------------------------------*/
 /* NEVHEP          - event number (or some special meaning*/
 /*                    (see documentation for details)     */
 /* NHEP            - actual number of entries in current  */
 /*                    event.                              */
 /* ISTHEP[IHEP]    - status code for IHEP'th entry - see  */
 /*                    documentation for details           */
 /* IDHEP [IHEP]    - IHEP'th particle identifier according*/
 /*                    to PDG.                             */
 /* JMOHEP[IHEP][0] - pointer to position of 1st mother    */
 /* JMOHEP[IHEP][1] - pointer to position of 2nd mother    */
 /* JDAHEP[IHEP][0] - pointer to position of 1st daughter  */
 /* JDAHEP[IHEP][1] - pointer to position of 2nd daughter  */
 /* PHEP  [IHEP][0] - X momentum [Gev/c]                   */
 /* PHEP  [IHEP][1] - Y momentum [Gev/c]                   */
 /* PHEP  [IHEP][2] - Z momentum [Gev/c]                   */
 /* PHEP  [IHEP][3] - Energy [Gev]                         */
 /* PHEP  [IHEP][4] - Mass[Gev/c^2]                        */
 /* VHEP  [IHEP][0] - X vertex [mm]                        */
 /* VHEP  [IHEP][1] - Y vertex [mm]                        */
 /* VHEP  [IHEP][2] - Z vertex [mm]                        */
 /* VHEP  [IHEP][3] - production time [mm/c]               */
 /*========================================================*/
        //IstHep convention:
        //                            0      - final state particle if JdaHEP=0
        //                                     intermediate particle if JdaHEP>0
        //                                     (NEUGEN extension; was "null")
        //                            1      - final state particle
        //                            2      - intermediate state
        //                            3      - documentation line
        //                            4-10   - reserved for future
        //                            11-200 - reserved for specific model use
        //                            201+   - reserved for users
        int _nmax = 500000;
        int _eventnum = 0;
        int _nhep = 0;
        int[] _isthep = new int[_nmax];
        int[] _idhep = new int[_nmax];
        int[] _jmohep = new int[2 * _nmax];
        int[] _jdahep = new int[2 * _nmax];
        double[] _phep = new double[5 * _nmax];
        double[] _vhep = new double[4 * _nmax];

        //Dummy values...
        int nevtreq = 1;
        int nevtgen = 1;
        int nevtwrt = 1;
        float stdecom = 2.F;
        float stdxsec = 2.F;
        double stdseed1 = 137.;
        double stdseed2 = 138.;

        double[] mom = new double[3];
        double[] pos = new double[3];
        double mass = 0.00051099906; // electron mass

// read the list of guineapig files from the input file
        String thisLine;
        BufferedReader myInput1 = new BufferedReader(new InputStreamReader(fin1));
        while ((thisLine = myInput1.readLine()) != null) {
            System.out.println("processing: " + thisLine);
            File f = new File(thisLine);
            if (!f.exists()) {
                System.out.println("\n\n  File " + f + " does not exist!");
                System.exit(1);
            }
            // file exists, get its name...
            String stdhepFileName = null;
            StringTokenizer st = new StringTokenizer(thisLine, "/");
            while (st.hasMoreTokens()) {
                stdhepFileName = st.nextToken();
            }
            // now remove .HEPEvt from the input filename
            int dot = stdhepFileName.lastIndexOf(".");
            stdhepFileName = stdhepFileName.substring(0, dot);

            stdhepFileName += ".stdhep";

            String outputFile = outputDir + "/" + stdhepFileName;
            StdhepWriter w = null;
            try {
                w = new StdhepWriter(outputFile, "Imported Stdhep Events v1.0", "From file " + stdhepFileName, 10);
                w.setCompatibilityMode(false);
            } catch (java.io.IOException ex) {
                System.err.println("Error opening file: " + outputFile);
                ex.printStackTrace();
                System.exit(1);
            }

            // write a begin run record
            w.writeRecord(new StdhepBeginRun(nevtreq, nevtgen, nevtwrt, stdecom, stdxsec, stdseed1, stdseed2));
            // now loop over contents of this file...
            FileInputStream fin2 = new FileInputStream(f);
            try {
                BufferedReader myInput = new BufferedReader(new InputStreamReader(fin2));
                double[] values = new double[7];
                int nEvent = 0;
                // read file line by line, each line represents one particle
                while ((thisLine = myInput.readLine()) != null) {
                    _nhep = 0;      // number of particles in this event
                    int j = 0;
                    // tokenize the string and convert to double values
                    st = new java.util.StringTokenizer(thisLine, " ");
                    int numTokens = st.countTokens();
                    if (numTokens == 1) {
                        continue;
                    }
                    while (st.hasMoreElements()) {
                        values[j++] = Double.valueOf(st.nextToken()).doubleValue();
                    }
                    pos[0] = values[0];
                    mom[0] = values[3];
                    pos[1] = values[1];
                    mom[1] = values[4];
                    pos[2] = values[2];
                    mom[2] = values[5];

                    double energy = sqrt(mom[0] * mom[0] + mom[1] * mom[1] + mom[2] * mom[2] + mass * mass);
                    // now populate the HEPEVT "common block"
                    _isthep[_nhep] = 1; // final state particle
                    _idhep[_nhep] = (int) values[6];
                    _phep[0 + 5 * _nhep] = mom[0]; //px
                    _phep[1 + 5 * _nhep] = mom[1]; //py
                    _phep[2 + 5 * _nhep] = mom[2]; //pz
                    _phep[3 + 5 * _nhep] = energy; //E
                    _phep[4 + 5 * _nhep] = mass; // mass
                    _vhep[0 + 4 * _nhep] = pos[0]; //x
                    _vhep[1 + 4 * _nhep] = pos[1]; //y
                    _vhep[2 + 4 * _nhep] = pos[2]; //z
                    // increment the number of particles in this event
                    _nhep++;
                    if (_nhep >= _nmax) {
                        throw new RuntimeException("\nAre you sure you want to create an event with more than " + _nmax + " particles?\nIf so, please recompile.");
                    }
                    //
                    // Create an StdhepEvent and write it out...
                    //
                    StdhepEvent event = new StdhepEvent(_eventnum++, _nhep, _isthep, _idhep, _jmohep, _jdahep, _phep, _vhep);
                    w.writeRecord(event);
                    nEvent++;
                }
                // write an end run record
                w.writeRecord(new StdhepEndRun(nevtreq, nevtgen, nevtwrt, stdecom, stdxsec, stdseed1, stdseed2));
                // close the file
                try {
                    System.out.println(" Closing file: " + outputFile + " with " + nEvent + " events");
                    w.close();
                } catch (java.io.IOException ex) {
                    System.err.println("Error closing file: " + outputFile);
                    ex.printStackTrace();
                    System.exit(1);
                }
                fin2.close();
            } catch (EOFException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void usage() {
        System.out.println("TextToStdhepConvertor: \n  an application to read in text files and convert to stdhep format.\n");
        System.out.println("Usage: \n\n >> java TextToStdhepConvertor listOfInputFiles.txt <output directory> \n");
        System.out.println(" Where: \n listOfInputFiles.txt    is a list of input files to process ");
        System.out.println(" Format: x(mm) y(mm) z(mm) px(GeV) py(GeV) pz(GeV) pdgId");
        System.out.println(" Writes to the current working directory unless output directory is specified");
        System.out.println("\n e.g. >> java TextToStdhepConvertor pairs.txt \n");
        System.out.println("  will convert the files listed in pairs.txt to the same named files.stdhep");
        System.exit(0);
    }

}
