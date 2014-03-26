package org.hps.users.mgraham.alignment;
/*
 * SiStrips.java
 *
 * Created on July 22, 2005, 4:07 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

//import static org.lcsim.units.clhep.SystemOfUnits.*;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.solids.GeomOp2D;
import org.lcsim.detector.solids.GeomOp3D;
import org.lcsim.detector.solids.Line3D;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.ChargeDistribution;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;

/**
 *
 * @author tknelson
 */
public class HPSStrips  extends SiStrips
{
    
    // Fields
    
    // Object definition
    private ChargeCarrier _carrier; // charge carrier collected
    private int _nstrips; // number of strips
    private double _pitch; // sense pitch
    private IDetectorElement _detector; // associated detector element
    private ITransform3D _parent_to_local; // parent to local transform
    private ITransform3D _local_to_global; // transformation to global coordinates
    private ITransform3D _global_to_local; // transformation from global coordinates
    private Polygon3D _geometry; // region in which strips are defined
    private double _capacitance_intercept = 10.;  // fixed capacitance independent of strip length
    private double _capacitance_slope = 0.1;  //  capacitance per unit length of strip
    
    // Cached for convenience
    private double _strip_offset;
    
 

     public HPSStrips(ChargeCarrier carrier, double pitch, IDetectorElement detector, ITransform3D parent_to_local,ITransform3D misalignment)
    {

//        System.out.println("Plane of polygon in sensor coordinates has... ");
//        System.out.println("                        normal: "+((SiSensor)detector).getBiasSurface(carrier).getNormal());
//        System.out.println("                        distance: "+((SiSensor)detector).getBiasSurface(carrier).getDistance());

        setCarrier(carrier);
        setPitch(pitch);
        setGeometry(((SiSensor)detector).getBiasSurface(carrier).transformed(parent_to_local));
        setStripNumbering();
        setDetectorElement(detector);
        setParentToLocal(parent_to_local);
        setGlobalToLocal(Transform3D.multiply(Transform3D.multiply(parent_to_local,detector.getGeometry().getGlobalToLocal()),misalignment));
        setLocalToGlobal(getGlobalToLocal().inverse());
    }

    public HPSStrips(ChargeCarrier carrier, double pitch, int nstrips, IDetectorElement detector, ITransform3D parent_to_local,ITransform3D misalignment)
    {
        setCarrier(carrier);
        setPitch(pitch);
        setGeometry(((SiSensor)detector).getBiasSurface(carrier).transformed(parent_to_local));
        setNStrips(nstrips);
        setDetectorElement(detector);
        setParentToLocal(parent_to_local);
        setGlobalToLocal(Transform3D.multiply(Transform3D.multiply(parent_to_local,detector.getGeometry().getGlobalToLocal()),misalignment));
        setLocalToGlobal(getGlobalToLocal().inverse());
    }
    

    // SiSensorElectrodes interface
    //=============================

    // Mechanical properties
    public int getNAxes()
    {
        return 1;
    }

    public IDetectorElement getDetectorElement()
    {
        return _detector;
    }

    public ITransform3D getParentToLocal()
    {
        return _parent_to_local;
    }

    public ITransform3D getLocalToGlobal()
    {
        return _local_to_global;
    }

    public ITransform3D getGlobalToLocal()
    {
        return _global_to_local;
    }

    public Polygon3D getGeometry()
    {
        return _geometry;
    }

    public Hep3Vector getMeasuredCoordinate(int axis)
    {
        if (axis == 0) return new BasicHep3Vector(1.0,0.0,0.0);
        else return null;
    }

    public Hep3Vector getUnmeasuredCoordinate(int axis)
    {
        if (axis == 0) return new BasicHep3Vector(0.0,1.0,0.0);
        else return null;
    }

    public int getNeighborCell(int cell, int ncells_0, int ncells_1)
    {
        int neighbor_cell = cell + ncells_0;
        if (isValidCell(neighbor_cell)) return neighbor_cell;
        else return -1;
    }

    public Set<Integer> getNearestNeighborCells(int cell)
    {
        Set<Integer> neighbors = new HashSet<Integer>();
        for (int ineigh = -1 ; ineigh <= 1; ineigh=ineigh+2)
        {
            int neighbor_cell = getNeighborCell(cell,ineigh,0);
            if (isValidCell(neighbor_cell)) neighbors.add(neighbor_cell);
        }
        return neighbors;
    }

    public boolean isValidCell(int cell)
    {
        return (cell >= 0 && cell < getNCells());
    }

    public int getNCells()
    {
        return _nstrips;
    }

    public int getNCells(int axis)
    {
        if (axis == 0)
        {
            return _nstrips;
        }
        else return 1;
    }

    public double getPitch(int axis)
    {
        if (axis == 0)
        {
            return _pitch;
        }
        else return 0;
    }
    public int getCellID(Hep3Vector position)
    {
        return (int)Math.round((position.x()+_strip_offset)/_pitch);
    }


    public int getRowNumber(Hep3Vector position)
    {
        return 0;
    }

    public int getColumnNumber(Hep3Vector position)
    {
        return getCellID(position);
    }

    public int getCellID(int row_number, int column_number)
    {
        return column_number;
    }

    public int getRowNumber(int cell_id)
    {
        return 0;
    }

    public int getColumnNumber(int cell_id)
    {
        return cell_id;
    }

    public Hep3Vector getPositionInCell(Hep3Vector position)
    {
        return VecOp.sub(position,getCellPosition(getCellID(position)));
    }

    public Hep3Vector getCellPosition(int strip_number)
    {
        return new BasicHep3Vector(strip_number*_pitch-_strip_offset,0.0,0.0);
    }

    // Electrical properties

    /**
     * Capacitance intercept parameter.  Units are pF.
     *
     * Capacitance is calculated as:
     * C = capacitance_intercept + strip_length * capacitance slope
     *
     * @param capacitance_intercept
     */
    public void setCapacitanceIntercept(double capacitance_intercept) {
        _capacitance_intercept = capacitance_intercept;
    }

    /**
     * Capacitance per unit strip length.  Units are pF / mm.
     *
     * @param capacitance_slope
     */
    public void setCapacitanceSlope(double capacitance_slope) {
        _capacitance_slope = capacitance_slope;
    }

    public ChargeCarrier getChargeCarrier()
    {
        return _carrier;
    }

    /**
     * Capacitance for a particular cell.  Units are pF.
     *
     * @param cell_id
     * @return
     */
    public double getCapacitance(int cell_id) // capacitance in pF
    {
        return _capacitance_intercept + _capacitance_slope*getStripLength(cell_id);
    }

    /**
     * Nominal capacitance used for throwing random noise in the sensor.
     * Calculated using middle strip.  Units are pF.
     *
     * @return
     */
    public double getCapacitance() {
        return getCapacitance(getNCells(0) / 2);
    }

    public SortedMap<Integer,Integer> computeElectrodeData(ChargeDistribution distribution)
    {
        SortedMap<Integer,Integer> electrode_data = new TreeMap<Integer,Integer>();

        int base_strip = getCellID(distribution.getMean());

        // put charge on strips in window 3-sigma strips on each side of base strip
        int axis = 0;
        int window_size = (int)Math.ceil(3.0*distribution.sigma1D(getMeasuredCoordinate(axis))/getPitch(axis));

        double integral_lower = distribution.getNormalization();
        double integral_upper = distribution.getNormalization();

        for (int istrip = base_strip-window_size; istrip <= base_strip+window_size; istrip++)
        {
            double cell_edge_upper = getCellPosition(istrip).x() + getPitch(axis)/2.0;

//            System.out.println("cell_edge_upper: "+cell_edge_upper);

            double integration_limit = cell_edge_upper;        //cell_edge_upper-distribution.mean().x();

//            System.out.println("integration_limit: "+integration_limit);

            integral_upper = distribution.upperIntegral1D(getMeasuredCoordinate(axis),integration_limit);

//            System.out.println("integral_upper: "+integral_upper);

            if (integral_lower<integral_upper)
            {
                throw new RuntimeException("Error in integrating Gaussian charge distribution!");
            }

            int strip_charge = (int)Math.round(integral_lower-integral_upper);

//            System.out.println("strip_charge: "+strip_charge);

            if (strip_charge != 0)
            {
                electrode_data.put(istrip,strip_charge);
            }

            integral_lower = integral_upper;
        }

        return electrode_data;

    }

    // Strip specific methods

    // length of strip
    public double getStripLength(int cell_id)
    {
//        System.out.println("strip_length: "+getStrip(cell_id).getLength());
        return getStrip(cell_id).getLength();
    }

    // center of strip
    public Hep3Vector getStripCenter(int cell_id)
    {
        LineSegment3D strip = getStrip(cell_id);
        return strip.getEndPoint(strip.getLength()/2);
    }

    // line segment for strip
    public LineSegment3D getStrip(int cell_id)
    {
        Line3D strip_line = new Line3D(new Point3D(getCellPosition(cell_id)),getUnmeasuredCoordinate(0));

//        System.out.println("Number of strips: "+this._nstrips);
//        System.out.println("Strip offset: "+this._strip_offset);
//        System.out.println("Pitch: "+this._pitch);
//        System.out.println("cell_id: "+cell_id);
//        System.out.println("strip_line start point: "+strip_line.getStartPoint());
//        System.out.println("strip_line direction: "+strip_line.getDirection());

        List<Point3D> intersections = new ArrayList<Point3D>();

        // Get intersections between strip line and edges of electrode polygon
        for (LineSegment3D edge : _geometry.getEdges())
        {
//            System.out.println("edge start point: "+edge.getStartPoint());
//            System.out.println("edge end point: "+edge.getEndPoint());

            if (GeomOp2D.intersects(strip_line,edge))
            {
                intersections.add(GeomOp3D.lineBetween(strip_line,edge).getStartPoint());
            }
        }

        // Check for rare occurrence of duplicates (can happen at corners of polygon)
        List<Point3D> strip_ends = new ArrayList<Point3D>(intersections);
        if (intersections.size() > 2)
        {
            for (int ipoint1 = 0; ipoint1 < intersections.size(); ipoint1++)
            {
                Point3D point1 = intersections.get(ipoint1);
                for (int ipoint2 = ipoint1+1; ipoint2 < intersections.size(); ipoint2++)
                {
                    Point3D point2 = intersections.get(ipoint2);
                    if (GeomOp3D.intersects(point1,point2))
                    {
                        strip_ends.remove(point2);
                        if (strip_ends.size() == 2) break;
                    }
                }
            }
        }

        return new LineSegment3D(strip_ends.get(0),strip_ends.get(1));
    }

    // Private setters
    //==================
    public void setCarrier(ChargeCarrier carrier)
    {
        _carrier = carrier;
    }

    public void setGeometry(Polygon3D geometry)
    {
//        System.out.println("Plane of polygon has... ");
//        System.out.println("                        normal: "+geometry.getNormal());
//        System.out.println("                        distance: "+geometry.getDistance());
//
//        System.out.println("Working plane has... ");
//        System.out.println("                        normal: "+GeomOp2D.PLANE.getNormal());
//        System.out.println("                        distance: "+GeomOp2D.PLANE.getDistance());

        if (GeomOp3D.equals(geometry.getPlane(),GeomOp2D.PLANE))
        {
            _geometry = geometry;
        }
        else
        {
            throw new RuntimeException("Electrode geometry must be defined in x-y plane!!");
        }
    }

    private void setStripNumbering()
    {
        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        for (Point3D vertex : _geometry.getVertices())
        {
            xmin = Math.min(xmin,vertex.x());
            xmax = Math.max(xmax,vertex.x());
        }

//        System.out.println("xmin: " + xmin);
//        System.out.println("xmax: " + xmax);
//
//
//        System.out.println("# strips: " +   (int)Math.ceil((xmax-xmin)/getPitch(0)) ) ;

        setNStrips(  (int)Math.ceil((xmax-xmin)/getPitch(0)) ) ;
    }

    private void setNStrips(int nstrips)
    {
        _nstrips = nstrips;
        setStripOffset();
//        _strip_offset = (_nstrips-1)*_pitch/2.;
    }

    private void setStripOffset()
    {
        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        for (Point3D vertex : _geometry.getVertices())
        {
            xmin = Math.min(xmin,vertex.x());
            xmax = Math.max(xmax,vertex.x());
        }

        double strips_center = (xmin+xmax)/2;

        _strip_offset = ((_nstrips-1)*_pitch)/2 - strips_center;

    }

    private void setPitch(double pitch)
    {
        _pitch = pitch;
    }

    private void setDetectorElement(IDetectorElement detector)
    {
        _detector = detector;
    }

    private void setParentToLocal(ITransform3D parent_to_local)
    {
        _parent_to_local = parent_to_local;
    }

    private void setLocalToGlobal(ITransform3D local_to_global)
    {
        _local_to_global = local_to_global;
    }

    private void setGlobalToLocal(ITransform3D global_to_local)
    {
        _global_to_local = global_to_local;
    }
    
}


