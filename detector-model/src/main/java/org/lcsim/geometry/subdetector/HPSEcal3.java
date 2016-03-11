package org.lcsim.geometry.subdetector;

import hep.graphics.heprep.HepRep;
import hep.graphics.heprep.HepRepFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.detector.converter.heprep.DetectorElementToHepRepConverter;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.util.IDEncoder;

/**
 * Reconstruction version of HPS ECal with crystal array.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Timothy Nelson <tknelsonm@slac.stanford.edu>
 * @version $Id: HPSEcal3.java,v 1.3 2012/04/30 18:04:38 jeremy Exp $
 */
public class HPSEcal3 extends AbstractSubdetector {
    private int nx;
    private int ny;
    //private double beamgap;
    //private double dface;
    private boolean oddX;
    List<CrystalRange> removeCrystals = new ArrayList<CrystalRange>();

    public static class NeighborMap extends HashMap<Long, Set<Long>> {
        IIdentifierHelper helper;

        public NeighborMap(IIdentifierHelper helper) {
            this.helper = helper;
        }

        public String toString() {
            System.out.println("NeighborMap has " + this.size() + " entries.");
            StringBuffer buff = new StringBuffer();
            for (long id : this.keySet()) {
                buff.append(helper.unpack(new Identifier(id))).append("\n");
                Set<Long> nei = this.get(id);
                for (long nid : nei) {
                    buff.append("  " + helper.unpack(new Identifier(nid))).append("\n");
                }
            }
            return buff.toString();
        }
    }

    private NeighborMap neighborMap = null;

    HPSEcal3(Element node) throws JDOMException {
        super(node);

        Element layout = node.getChild("layout");

        nx = layout.getAttribute("nx").getIntValue();
        ny = layout.getAttribute("ny").getIntValue();        
        //beamgap = layout.getAttribute("beamgap").getDoubleValue();
        //dface = layout.getAttribute("dface").getDoubleValue();

        if (nx % 2 != 0)
            oddX = true;

        // Setup range of indices to be skipped.
        for (Object obj : layout.getChildren("remove")) {
            Element remove = (Element) obj;
            try {
                removeCrystals.add(new CrystalRange(remove));
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        }

        /*
         * <remove ixmin="2" ixmax="10" iymin="-1" iymax="1"/>
         */
    }

    private static class CrystalRange {
        int ixmin;
        int ixmax;
        int iymin;
        int iymax;

        CrystalRange(Element elem) throws Exception {
            ixmin = ixmax = iymin = iymax = 0;

            if (elem.getAttribute("ixmin") != null) {
                ixmin = elem.getAttribute("ixmin").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmin parameter.");
            }

            if (elem.getAttribute("ixmax") != null) {
                ixmax = elem.getAttribute("ixmax").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmax parameter.");
            }

            if (elem.getAttribute("iymin") != null) {
                iymin = elem.getAttribute("iymin").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmax parameter.");
            }

            if (elem.getAttribute("iymax") != null) {
                iymax = elem.getAttribute("iymax").getIntValue();
            } else {
                throw new RuntimeException("Missing iymax parameter.");
            }
        }
    }

    private boolean isValidXY(int ix, int iy) {
        if (!isValidX(ix))
            return false;
        if (!isValidY(iy))
            return false;
        return checkRange(ix, iy, this.removeCrystals);
    }

    private boolean checkRange(int ix, int iy, List<CrystalRange> ranges) {
        if (ranges.size() == 0)
            return true;
        for (CrystalRange range : ranges) {
            if ((ix >= range.ixmin && ix <= range.ixmax) && ((iy >= range.iymin) && (iy <= range.iymax))) {
                return false;
            }

        }
        return true;
    }

    //public double distanceToFace() {
    //    return dface;
    //}

    //public double beamGap() {
    //    return beamgap;
    //}

    /**
     * The number of crystals in X in one section.
     * 
     * @return the number of crystals in X in one section
     */
    public double nx() {
        return nx;
    }

    /**
     * The number of crystals in y in one section.
     * 
     * @return the number of crystals in Y in one section
     */
    public double ny() {
        return ny;
    }

    // Class for storing neighbor indices in XY and side.
    static class XY implements Comparator<XY> {
        int x;
        int y;

        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public boolean equals(Object o) {
            XY xy = (XY) o;
            return xy.x() == x && xy.y() == y;
        }

        public int compare(XY o1, XY o2) {
            if (o1.equals(o2)) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    /**
     * Get the neighbors for a given cell ID. Each crystal not on an edge has 8 neighbors. Edge crystals have fewer. 
     * @param id The cell ID.
     * @return A <code>Set</code> containing the cell's neighbors.
     */
    Set<Long> getNeighbors(Long id) {
        // Get the IDDecoder.
        IDDecoder dec = getIDDecoder();

        // Set the ID.
        dec.setID(id);

        // Get ID field values.
        int x = dec.getValue("ix");
        int y = dec.getValue("iy");

        // Get field indices.
        int ix = dec.getFieldIndex("ix");
        int iy = dec.getFieldIndex("iy");

        // Get X, Y, & side neighbor data for this crystal.
        Set<XY> neighbors = getNeighbors(x, y);

        // Get buffer with values from current ID.
        int[] buffer = new int[dec.getFieldCount()];
        dec.getValues(buffer);

        // Create an encoder to make neighbor IDs.
        IDEncoder enc = new IDEncoder(dec.getIDDescription());

        // Set to hold neighbor IDs.
        Set<Long> ids = new HashSet<Long>();

        // Loop over neighbor objects to make IDs.
        for (XY xyside : neighbors) {
            buffer[ix] = xyside.x;
            buffer[iy] = xyside.y;
            long nId = enc.setValues(buffer);
            ids.add(nId);
        }

        return ids;
    }

    Set<XY> getNeighbors(int ix, int iy) {
        Set<Integer> xneighbors = getXNeighbors(ix);
        Set<Integer> yneighbors = getYNeighbors(iy);

        Set<XY> neighbors = new HashSet<XY>();

        for (Integer jx : xneighbors) {
            for (Integer jy : yneighbors) {
                // Filter out self.
                if (jx == ix && jy == iy) {
                    continue;
                }

                // Check for valid neighbor.
                // FIXME: Duplication of isValidX + isValidY.
                if (!isValidXY(jx, jy))
                    continue;

                neighbors.add(new XY(jx, jy));
            }
        }

        return neighbors;
    }

    Set<Integer> getXNeighbors(int ix) {
        Set<Integer> neighbors = new HashSet<Integer>();

        // Add self.
        neighbors.add(ix);

        // Left neighbor.
        if (isValidX(ix - 1)) {
            neighbors.add(ix - 1);
        } else if (isValidX(ix - 2)) {
            neighbors.add(ix - 2);
        }

        // Right neighbor.
        if (isValidX(ix + 1)) {
            neighbors.add(ix + 1);
        } else if (isValidX(ix + 2)) {
            neighbors.add(ix + 2);
        }

        return neighbors;
    }

    Set<Integer> getYNeighbors(int iy) {
        Set<Integer> neighbors = new HashSet<Integer>();

        // Add self.
        neighbors.add(iy);

        // Lower neighbor.
        if (isValidY(iy - 1)) {
            neighbors.add(iy - 1);
        }

        // Upper neighbor.
        if (isValidY(iy + 1)) {
            neighbors.add(iy + 1);
        }

        return neighbors;
    }

    boolean isValidY(int iy) {
        // Zero is not valid because ID scheme goes from 1.
        return iy >= -ny && iy <= ny && iy != 0;
    }

    boolean isValidX(int ix) {
        // Even case.
        if (!oddX) {
            return ix >= -nx / 2 && ix <= nx / 2 && ix != 0;
        }
        // Odd case.
        else {
            return ix >= (-nx - 1) / 2 && ix <= (nx + 1) / 2;
        }
    }

    /**
     * Create a map of crystal IDs to the <code>Set</code> of neighbor crystal IDs.
     * 
     * @return A map of neighbors for each crystal ID.
     */
    public NeighborMap getNeighborMap() {
        if (neighborMap != null) {
            return neighborMap;
        }

        // Setup the private instance of the map.
        neighborMap = new NeighborMap(this.getDetectorElement().getIdentifierHelper());

        IDDecoder dec = getIDDecoder();
        IDEncoder enc = new IDEncoder(dec.getIDDescription());

        int nfields = dec.getFieldCount();
        int[] vals = new int[nfields];

        vals[dec.getFieldIndex("system")] = getSystemID();

        int idxx = dec.getFieldIndex("ix");
        int idxy = dec.getFieldIndex("iy");

        /*
        int hnx = nx;

        // Calculate number of X for loop. (from LCDD conv)
        if (oddX) {
            hnx -= 1;
            hnx /= 2;
        } else {
            hnx /= 2;
        }
        */

        // Loop over y.
        for (int iy = -ny; iy <= ny; iy++) {
            int loopx = (int) Math.floor(nx / 2);
            // Loop over x.
            for (int ix = -loopx; ix <= loopx; ix++) {
                if (!isValidXY(ix, iy))
                    continue;

                vals[idxx] = ix;
                vals[idxy] = iy;

                Long id = enc.setValues(vals);
                Set<Long> neighbors = getNeighbors(id);

                neighborMap.put(id, neighbors);
            }
        }

        return neighborMap;
    }

    public void appendHepRep(HepRepFactory factory, HepRep heprep) {
        DetectorElementToHepRepConverter.convert(getDetectorElement(), factory, heprep, -1, false, getVisAttributes().getColor());
    }
}