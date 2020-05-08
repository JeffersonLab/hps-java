package org.hps.analysis.MC;

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
import java.util.StringTokenizer;

/**
 * Reads in text file in LUND format and creates events in stdhep file format.
 *
 */
/* Header Format:
/* 1 Number of particles in event, blah, blah blah (user defined)
 /*
 /* Particle Format: 
 /* 1 index 
 /* 2 lifetime (ns) 
 /* 3 type (1 is active) 
 /* 4 pdgID 
 /* 5 index of parent 
 /* 6 index of child
 /* 7 x momentum (GeV) 
 /* 8 y momentum (GeV) 
 /* 9 z momentum (GeV) 
 /* 10 energy (GeV) 
 /* 11 mass (GeV) 
 /* 12 x vertex (cm) 
 /* 13 y vertex (cm) 
 /* 14 z vertex (cm)
 /*
 * @author Norman A. Graf
 */
public class LundToStdhepConverter {

    public static void main(String args[]) throws IOException {

        // remind user of correct usage
        if (args.length < 1) {
            usage();
        }
        if (args.length == 1 && args[0].equals("-h")) {
            usage();
        }
        String fileName = args[0];

        FileInputStream fin1 = new FileInputStream(fileName);

        double zOffset = 0.;
        if (args.length > 1) {
            zOffset = Double.parseDouble(args[1]);
            System.out.println("Offsetting z vertex by "+zOffset+" mm.");
        }
        File outputDir = new File(".");
        if (args.length > 2) {
            outputDir = new File(args[2]);
        }
        // check if output directory exists

        if (!outputDir.exists()) {
            System.out.println("\n\n  Directory " + outputDir + " does not exist!");
            System.exit(1);
        }

        int dot = fileName.lastIndexOf(".txt");
        String stdhepFileName = fileName.substring(0, dot);

        stdhepFileName += ".stdhep";

        String outputFile = outputDir + "/" + stdhepFileName;
        System.out.println(outputFile);
        StdhepWriter w = null;
        try {
            w = new StdhepWriter(outputFile, "Imported Stdhep Events v1.0", "From file " + fileName, 10);
            w.setCompatibilityMode(false);
        } catch (java.io.IOException ex) {
            System.err.println("Error opening file: " + outputFile);
            ex.printStackTrace();
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
 /* IstHep convention: */
 /* 0      - final state particle if JdaHEP=0 */
 /*          intermediate particle if JdaHEP>0 */
 /*          (NEUGEN extension; was "null") */
 /* 1      - final state particle */
 /* 2      - intermediate state */
 /* 3      - documentation line */
 /* 4-10   - reserved for future */
 /* 11-200 - reserved for specific model use */
 /* 201+   - reserved for users */
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

        String thisLine;

        // write a begin run record
        w.writeRecord(new StdhepBeginRun(nevtreq, nevtgen, nevtwrt, stdecom, stdxsec, stdseed1, stdseed2));
        // now loop over contents of this file...
        FileInputStream fin2 = new FileInputStream(fileName);
        try {
            BufferedReader myInput = new BufferedReader(new InputStreamReader(fin2));
            double[] values = new double[14];
            int nEvent = 0;
            // read header to get number of particles in event.
            while ((thisLine = myInput.readLine()) != null) {
                // tokenize the string and convert to double values
                StringTokenizer st = new java.util.StringTokenizer(thisLine, " ");
                int nParticles = Integer.valueOf(st.nextToken()).intValue();
                if (nParticles >= _nmax) {
                    throw new RuntimeException("\nAre you sure you want to create an event with more than " + _nmax + " particles?\nIf so, please recompile.");
                }
                _nhep = 0;
                // read file line by line, each line represents one particle
                for (int i = 0; i < nParticles; ++i) {
                    thisLine = myInput.readLine();
                    int j = 0;
                    // tokenize the string and convert to double values
                    st = new java.util.StringTokenizer(thisLine, " ");
                    int numTokens = st.countTokens();

                    while (st.hasMoreElements()) {
                        values[j++] = Double.parseDouble(st.nextToken());
                    }
                    pos[0] = values[11] * 10.; //convert cm to mm
                    mom[0] = values[6];
                    pos[1] = values[12] * 10.;//convert cm to mm
                    mom[1] = values[7];
                    pos[2] = values[13] * 10. + zOffset;//convert cm to mm and translate z by zOffset
                    mom[2] = values[8];

                    double energy = values[9];
                    // now populate the HEPEVT "common block"
                    _isthep[_nhep] = 1; // final state particle
                    _idhep[_nhep] = (int) values[3];
                    _phep[0 + 5 * _nhep] = mom[0]; //px
                    _phep[1 + 5 * _nhep] = mom[1]; //py
                    _phep[2 + 5 * _nhep] = mom[2]; //pz
                    _phep[3 + 5 * _nhep] = energy; //E
                    _phep[4 + 5 * _nhep] = values[10]; // mass
                    _vhep[0 + 4 * _nhep] = pos[0]; //x
                    _vhep[1 + 4 * _nhep] = pos[1]; //y
                    _vhep[2 + 4 * _nhep] = pos[2]; //z
                    // increment the number of particles in this event
                    _nhep++;
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

    public static void usage() {
        System.out.println("LundToStdhepConvertor: \n  an application to read in text files in LUND format and convert to stdhep format.\n");
        System.out.println("Usage: \n\n >> java LundToStdhepConvertor InputFile.txt <zOffSet[mm]> <output directory> \n");
        System.out.println(" Where: \n InputFile.txt    is an input text file in Lund format to process ");
        System.out.println(" Writes to the current working directory unless output directory is specified");
        System.out.println("\n e.g. >> java LundToStdhepConvertor LundEvents.txt -5 \n");
        System.out.println("  will convert the events in LundEvents.txt to LundEvents.stdhep offset by -5mm in z in the same directory");
        System.exit(0);
    }
}
