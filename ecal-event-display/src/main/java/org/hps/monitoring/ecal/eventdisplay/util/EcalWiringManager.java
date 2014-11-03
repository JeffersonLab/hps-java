package org.hps.monitoring.ecal.eventdisplay.util;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Class <code>EcalWiringManager</code> reads in the crystal hardware
 * data sheet for the calorimeter and stores the data in <code>
 * CrystalDataSet</code> objects for access and reference by the <code>
 * Viewer</code> classes. Crystal LCSim indices are mapped to the data
 * set that corresponds to that crystal.
 * 
 * @author Kyle McCarty
 */
public class EcalWiringManager implements Iterable<CrystalDataSet> {
    // Delimiter class statics.
    public static final int SPACE_DELIMITED = 1;
    public static final int TAB_DELIMITED = 2;
    public static final int COMMA_DELIMITED = 3;
    
    // Internal variables.
    private Map<Point, CrystalDataSet> crystalMap = new HashMap<Point, CrystalDataSet>(442);
    
    /**
     * Initializes an <code>EcalWiringManager</code> database with
     * hardware information loaded from the indicated file. The data
     * file is assumed to be comma delimited and the first line is
     * assumed to be a header line and to not contain data.
     * @param dataFile - The path to the data file.
     * @throws IOException Occurs if there is an error opening or parsing
     * the data file.
     */
    public EcalWiringManager(String dataFile) throws IOException {
        this(dataFile, COMMA_DELIMITED, true);
    }
    
    /**
     * Initializes an <code>EcalWiringManager</code> database with
     * hardware information loaded from the indicated file.
     * @param dataFile - The path to the data file.
     * @param delimiter - The delimiter used by the data file.
     * @param skipFirstLine - Whether the first line should be skipped.
     * @throws IOException Occurs if there is an error opening or parsing
     * the data file.
     */
    public EcalWiringManager(String dataFile, int delimiter, boolean skipFirstLine) throws IOException {
        // Create a file reader.
        FileReader baseReader = new FileReader(dataFile);
        BufferedReader reader = new BufferedReader(baseReader);
        
        // If the first line should be skipped, do that.
        if(skipFirstLine) { reader.readLine(); }
        
        // Iterate over the lines of the data file.
        String curLine = null;
        readLoop:
        while((curLine = reader.readLine()) != null) {
            // If the current line is empty, skip it.
            if(curLine.isEmpty()) { continue readLoop; }
            
            // Create a line scanner.
            Scanner lineScan = new Scanner(curLine);
            if(delimiter == COMMA_DELIMITED) { lineScan.useDelimiter(","); }
            else if(delimiter == SPACE_DELIMITED) { lineScan.useDelimiter(" *"); }
            else if(delimiter == TAB_DELIMITED) { lineScan.useDelimiter("\t"); }
            
            // Get the crystal data values.
            int ix = lineScan.nextInt();
            int iy = lineScan.nextInt();
            int apd = lineScan.nextInt();
            String preamp = lineScan.next();
            int ledChan = lineScan.nextInt();
            double ledDriver = lineScan.nextDouble();
            int fadcSlot = lineScan.nextInt();
            int fadcChan = lineScan.nextInt();
            int splitter = lineScan.nextInt();
            int hvGroup = lineScan.nextInt();
            int jout = lineScan.nextInt();
            String mb = lineScan.next();
            int channel = lineScan.nextInt();
            int gain = lineScan.nextInt();
            lineScan.close();
            
            // Create a crystal data set in which to store the data.
            CrystalDataSet cds = new CrystalDataSet(ix, iy, apd, preamp, ledChan, ledDriver, fadcSlot,
                    fadcChan, splitter, hvGroup, jout, mb, channel, gain);
            
            // Map the crystal index to the crystal data set.
            crystalMap.put(cds.getCrystalIndex(), cds);
        }
        
        // Close the readers.
        reader.close();
        baseReader.close();
    }
    
    /**
     * Gets the set of calorimeter hardware data associated with the
     * crystal at the given index.
     * @param crystalIndex - The index of the crystal for which to obtain
     * the hardware information. This should be in LCSim coordinates.
     * @return Returns the hardware information for the crystal as a
     * <code>CrystalDataSet</code> object or returns <code>null</code>
     * if the crystal index is invalid.
     */
    public CrystalDataSet getCrystalData(Point crystalIndex) { return crystalMap.get(crystalIndex); }
    
    /**
     * Generates a list of all the crystals that match the conditions
     * set in the argument <code>Comparator</code> object.
     * @param conditions - An object defining the conditions that denote
     * a "matched" crystal.
     * @return Returns <code>Point</code> objects that contain the
     * LCSim coordinates of all the crystals that produce a result of
     * <code>true</code> with the method <code>comparator.equals(crystal)
     * </code>.
     */
    public List<Point> getFilteredCrystalList(Comparator<CrystalDataSet> conditions) {
        // Create a list of crystal indices that match the conditions.
        List<Point> crystalList = new ArrayList<Point>();
        
        // Iterate over the crystal data set entries.
        for(CrystalDataSet cds : this) {
            // Check if the crystal data set meets the given conditions.
            if(conditions.equals(cds)) {
                crystalList.add(cds.getCrystalIndex());
            }
        }
        
        // Return the resultant list.
        return crystalList;
    }
    
    @Override
    public Iterator<CrystalDataSet> iterator() {
        return new MapValueIterator<CrystalDataSet>(crystalMap);
    }
}