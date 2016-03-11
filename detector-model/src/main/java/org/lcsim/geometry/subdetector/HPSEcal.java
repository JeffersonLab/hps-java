package org.lcsim.geometry.subdetector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.util.IDEncoder;

/**
 * Reconstruction version of HPS ECal with crystal array.
 * 
 * @author Jeremy McCormick, SLAC
 * @author Timothy Nelson, SLAC
 */
public class HPSEcal extends AbstractSubdetector {

    private int nx;
    private int ny;
    private double beamgap;
    private double dface;
    private boolean oddX;

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

    HPSEcal(Element node) throws JDOMException {
        super(node);

        Element layout = node.getChild("layout");

        nx = layout.getAttribute("nx").getIntValue();
        ny = layout.getAttribute("ny").getIntValue();
        beamgap = layout.getAttribute("beamgap").getDoubleValue();
        dface = layout.getAttribute("dface").getDoubleValue();

        if (nx % 2 != 0) {
            oddX = true;
        }
    }

    public double distanceToFace() {
        return dface;
    }

    public double beamGap() {
        return beamgap;
    }

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

    // Class for storing neighbor incides in XY and side.
    static class XYSide implements Comparator<XYSide> {

        int x;
        int y;
        int side;

        public XYSide(int x, int y, int side) {
            this.x = x;
            this.y = y;
            this.side = side;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int side() {
            return side;
        }

        public boolean equals(Object o) {
            XYSide xy = (XYSide) o;
            return xy.x() == x && xy.y() == y && xy.side() == side;
        }

        public int compare(XYSide o1, XYSide o2) {
            if (o1.equals(o2)) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    /**
     * Get the neighbors for a given cell ID. Each crystal not on an edge has 8 neighbors. Edge crystals have fewer.
     * 
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
        int side = dec.getValue("side");

        // Get field indices.
        int ix = dec.getFieldIndex("ix");
        int iy = dec.getFieldIndex("iy");
        int iside = dec.getFieldIndex("side");

        // Get X, Y, & side neighbor data for this crystal.
        Set<XYSide> neighbors = getNeighbors(x, y, side);

        // Get buffer with values from current ID.
        int[] buffer = new int[dec.getFieldCount()];
        dec.getValues(buffer);

        // Create an encoder to make neighbor IDs.
        IDEncoder enc = new IDEncoder(dec.getIDDescription());

        // Set to hold neighbor IDs.
        Set<Long> ids = new HashSet<Long>();

        // Loop over neighbor objects to make IDs.
        for (XYSide xyside : neighbors) {
            buffer[ix] = xyside.x;
            buffer[iy] = xyside.y;
            buffer[iside] = xyside.side;
            long nId = enc.setValues(buffer);
            ids.add(nId);
        }

        return ids;
    }

    Set<XYSide> getNeighbors(int ix, int iy, int side) {
        Set<Integer> xneighbors = getXNeighbors(ix);
        Set<Integer> yneighbors = getYNeighbors(iy);

        Set<XYSide> neighbors = new HashSet<XYSide>();

        for (Integer jx : xneighbors) {
            for (Integer jy : yneighbors) {
                // Filter out self.
                if (jx == ix && jy == iy) {
                    continue;
                }

                neighbors.add(new XYSide(jx, jy, side));
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
        return iy > 0 && iy <= ny;
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

        int hnx = nx;

        // Calculate number of X for loop. (from LCDD conv)
        if (oddX) {
            hnx -= 1;
            hnx /= 2;
        } else {
            hnx /= 2;
        }

        for (int side = -1; side <= 1; side++) {
            if (side == 0)
                continue;
            vals[dec.getFieldIndex("side")] = side;
            // Loop over y.
            for (int iy = 1; iy <= ny; iy++) {
                // Loop over x.
                for (int ix = 0; ix <= hnx; ix++) {
                    // Loop for positive and negative x.
                    for (int j = -1; j <= 1; j++) {
                        if (j == 0)
                            continue;

                        vals[idxx] = ix * j;
                        vals[idxy] = iy;

                        Long id = enc.setValues(vals);
                        Set<Long> neighbors = getNeighbors(id);

                        neighborMap.put(id, neighbors);
                    }
                }
            }
        }

        return neighborMap;
    }
}