package org.lcsim.detector.tracker.silicon;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.GeomOp2D;
import org.lcsim.detector.solids.GeomOp3D;
import org.lcsim.detector.solids.Line3D;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Point3D;

/**
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class ThinSiStrips extends SiStrips {

    private int channelOffset = 255;
    double _length;

    /**
     * Constructor
     */
    public ThinSiStrips(ChargeCarrier carrier, double pitch, IDetectorElement detector, ITransform3D parentToLocal) {
        super(carrier, pitch, detector, parentToLocal);
    //        System.out.println("ThinSiStrips::Setting up strips with pitch = " + pitch);
        _length = getLength();
    }

    /**
     *
     */
    public ThinSiStrips(ChargeCarrier carrier, double pitch, int nStrips, IDetectorElement detector, ITransform3D parentToLocal) {
        super(carrier, pitch, nStrips, detector, parentToLocal);
//        System.out.println("ThinSiStrips::Setting up strips with pitch = " + pitch);
        _length = getLength();
    }

    @Override
    public int getNeighborCell(int cell, int nCells0, int nCells1) {

        if ((cell == (getNCells() / 2 - 1)) && (nCells0 == 1))
            return -1;
        else if ((cell == getNCells()) && (nCells0 == -1))
            return -1;

        return super.getNeighborCell(cell, nCells0, 0);
    }

    @Override
    public int getCellID(Hep3Vector position) {
//        System.out.println("[ ThinSiStrips ][ getCellID ]: Local to Global: " + getLocalToGlobal().transformed(position).toString());
//        System.out.println("[ ThinSiStrips ][ getCellID ]: Local to Parent: " + getParentToLocal().inverse().transformed(position).toString());

        int id = (int) Math.round((position.x() + _strip_offset) / _pitch);
//        System.out.println("[ ThinSiStrips ][ getCellID ]: ID Before check: " + id);
        if (position.y() > 0)
            id = (getNCells() - id - 1);
//        System.out.println("[ ThinSiStrips ][ getCellID ]: ID After check: " + id);
        return id;
    }

    @Override
    protected void setStripNumbering() {

        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        for (Point3D vertex : getGeometry().getVertices()) {
            System.out.println("[ ThinSiStrips ][ setStripNumber ]: Vertex: " + vertex.toString());
            xmin = Math.min(xmin, vertex.x());
            xmax = Math.max(xmax, vertex.x());
        }

  //      System.out.println("[ ThinSiStrips ][ setStripNumbering ]: x min: " + xmin);
  //      System.out.println("[ ThinSiStrips ][ setStripNumbering ]: x max: " + xmax);

        int nStrips = 2 * ((int) Math.ceil((xmax - xmin) / getPitch(0)));

//        System.out.println("[ ThinSiStrips ][ setStripNumbering ]: Number of strips: " + nStrips);

        super.setNStrips(nStrips);
    }

    @Override
    protected void setStripOffset() {

        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        for (Point3D vertex : getGeometry().getVertices()) {
            xmin = Math.min(xmin, vertex.x());
            xmax = Math.max(xmax, vertex.x());
        }

//        System.out.println("[ ThinSiStrips ][ setStripOffset ]: x min: " + xmin);
//        System.out.println("[ ThinSiStrips ][ setStripOffset ]: x max: " + xmax);

        double stripsCenter = (xmin + xmax) / 2;
 //       System.out.println("[ ThinSiStrips ][ setStripOffset ]: strips center: " + stripsCenter);
 //       System.out.println("[ ThinSiStrips ][ setStripOffset ]: ((nStrips/2) - 1)*pitch)/2: " + (((_nstrips / 2) - 1) * _pitch) / 2);

        _strip_offset = (((_nstrips / 2) - 1) * _pitch) / 2 - stripsCenter;

 //       System.out.println("[ ThinSiStrips ][ setStripOffset ]: Offset: " + _strip_offset);
    }
    // length of strip

    public double getStripLength(int cell_id) {
//        System.out.println("strip_length: "+getStrip(cell_id).getLength());
        return _length / 2.0;  // all strips are the same length
    }

    @Override
    public Hep3Vector getCellPosition(int stripNumber) {
        int stripFromEdge = stripNumber;
        double vposition = -0.5 * this.getStripLength(stripNumber);
//        System.out.println("[ ThinSiStrips ][ getCellPosition ]: Before strip #: " + stripNumber);  
        if (stripNumber >= getNCells() / 2) {
            stripFromEdge = (getNCells() - stripNumber - 1);
            vposition = 0.5 * this.getStripLength(stripNumber);
        }

//        System.out.println("[ ThinSiStrips ][ getCellPosition ]: After strip #: " + stripNumber);  
//        System.out.println("[ ThinSiStrips ][ getCellPosition ]: strip # = " + stripNumber
//                + " stripFromEdge = " + stripFromEdge);
        Hep3Vector stripPosition = new BasicHep3Vector(stripFromEdge * _pitch - _strip_offset, vposition, 0.0);
//        System.out.println("[ ThinSiStrips ][ getCellPosition ]: stripPosition = " + stripPosition.toString());
        return stripPosition;
    }

    // line segment for strip
    public double getLength() {
        Point3D fakeStrip = new Point3D(0 - _strip_offset, 0, 0);
        Line3D strip_line = new Line3D(fakeStrip, getUnmeasuredCoordinate(0));

        //System.out.println("strip_line start point: "+strip_line.getStartPoint());
        //System.out.println("strip_line direction: "+strip_line.getDirection());
        List<Point3D> intersections = new ArrayList<Point3D>();

        // Get intersections between strip line and edges of electrode polygon
        for (LineSegment3D edge : getGeometry().getEdges())
            //System.out.println("edge start point: "+edge.getStartPoint());
            //System.out.println("edge end point: "+edge.getEndPoint());

            if (GeomOp2D.intersects(strip_line, edge))
                intersections.add(GeomOp3D.lineBetween(strip_line, edge).getStartPoint());

        // Check for rare occurrence of duplicates (can happen at corners of polygon)
        List<Point3D> strip_ends = new ArrayList<Point3D>(intersections);
        if (intersections.size() > 2)
            for (int ipoint1 = 0; ipoint1 < intersections.size(); ipoint1++) {
                Point3D point1 = intersections.get(ipoint1);
                for (int ipoint2 = ipoint1 + 1; ipoint2 < intersections.size(); ipoint2++) {
                    Point3D point2 = intersections.get(ipoint2);
                    if (GeomOp3D.intersects(point1, point2)) {
                        strip_ends.remove(point2);
                        if (strip_ends.size() == 2)
                            break;
                    }
                }
            }

        return new LineSegment3D(strip_ends.get(0), strip_ends.get(1)).getLength();
    }

    // center of strip
    @Override
    public Hep3Vector getStripCenter(int cell_id) {
//        LineSegment3D strip = getStrip(cell_id);
//        return strip.getEndPoint(strip.getLength()/2);
        return new Point3D(getCellPosition(cell_id));
    }

}
