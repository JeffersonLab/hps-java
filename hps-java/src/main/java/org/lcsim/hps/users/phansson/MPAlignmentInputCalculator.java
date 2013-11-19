/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.hps.recon.tracking.TrackerHitUtils;
import org.lcsim.hps.users.mgraham.alignment.RunAlignment;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson
 */
public abstract class MPAlignmentInputCalculator {
    protected final int _nTrackParameters = 5;  //the five track parameters
    protected boolean _DEBUG = false;
    private String _outfile;
    protected List<GlobalParameter> _glp;
    protected BasicMatrix _dfdq;
    protected HelicalTrackFit _trk;
    protected String _type = "LOCAL";
    protected Hep3Vector _bfield;
    protected ResLimit _resLimits;
    protected AlignmentUtils _alignUtils;
    protected AlignmentUtils.OldAlignmentUtils _oldAlignUtils;
    protected AlignmentUtils.NumDerivatives _numDerivatives;
    protected TrackerHitUtils trackerHitUtil;
    protected boolean hideFrame = false;
    protected AIDA aida = AIDA.defaultInstance();
    protected IAnalysisFactory af = aida.analysisFactory();
    
    protected boolean _includeMS = true;
    public abstract void PrintResidualsAndDerivatives(Track track, int itrack);
    protected abstract void makeAlignmentPlots();
    public abstract void updatePlots();
    private FileWriter fWriter;
    private PrintWriter pWriter;
    
    public MPAlignmentInputCalculator(String outfile,String type) {
        _glp = new ArrayList<GlobalParameter>();
        _resLimits = new ResLimit();
        _alignUtils = new AlignmentUtils(_DEBUG);
        _oldAlignUtils = new AlignmentUtils(_DEBUG).new OldAlignmentUtils();
        _numDerivatives = new AlignmentUtils(_DEBUG).new NumDerivatives();
        trackerHitUtil = new TrackerHitUtils(_DEBUG);
        _bfield = new BasicHep3Vector(0., 0., 1.);
        _type = type;
        _outfile = outfile;
        openFile();
    }

    

    public void setResLimits(int l,int d, double low,double high) {
        _resLimits.add(0,l,d, low,high); //top
        _resLimits.add(1,l,d, low,high); //bottom
    }


    public void setResLimits(int s, int l,int d, double low,double high) {
        _resLimits.add(s,l,d, low,high);
    }

     
    public boolean isAllowedResidual(int side,int layer,int d,double res) {
    
        if(res <_resLimits.getMin(side,layer,d)) return false;
        if(res >_resLimits.getMax(side,layer,d)) return false;
        return true;
        
    }
    
    
    public void closeFile() {
        try {
            pWriter.close();
            fWriter.close();
        } catch(IOException ex) {
             Logger.getLogger(RunAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void openFile() {
         try {
            fWriter = new FileWriter(_outfile);
            pWriter = new PrintWriter(fWriter);
        } catch (IOException ex) {
            Logger.getLogger(RunAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addMilleInputLine(String line) {
        this.pWriter.print(line);
    }

    public void setDebug(boolean debug) {
        this._DEBUG = debug;
        _alignUtils.setDebug(debug);
        _numDerivatives.setDebug(debug);
    }

    public void setHideFrame(boolean hide) {
        this.hideFrame = hide;
    }

    public void setIncludeMS(boolean include) {
        this._includeMS = include;
    }

   
    public void setUniformZFieldStrength(double bfield) {
        _bfield = new BasicHep3Vector(0.0, 0.0, bfield);
    }

    
    
}
