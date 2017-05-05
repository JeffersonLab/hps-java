/**
 * 
 */
package org.hps.recon.filtering;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Filter class to create a smaller beamspot based on a sample with fixed, larger, beamspot. 
 * Uses sampling-rejection MC technique.  
 */
public class BeamspotTransformFilter extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private IPlotter plotter;
    private IPlotter plotter2;
    private IPlotter plotter3;
    private IHistogram2D h_bs_2d;
    private IHistogram1D h_bs_z;
    private IHistogram1D h_bs_y;
    private IHistogram1D h_bs_x;
    private IHistogram2D h_bs_2d_proposal;
    private IHistogram2D h_bs_2d_desired;
    private IHistogram1D h_bs_z_desired;
    private IHistogram1D h_bs_y_desired;
    private IHistogram1D h_bs_x_desired;
    private String MCParticleCollectionName = "MCParticle";
    private final Logger logger = Logger.getLogger(BeamspotTransformFilter.class.getSimpleName());
    private double mu1 = 0.0;
    private double mu2 = 0.0;
    private double s1 = 0.2;
    private double s2 = 0.04;
    private double rho = 0.0;
    private double mu1_desired = 0.0;
    private double mu2_desired = 0.0;
    private double s1_desired = 0.2;
    private double s2_desired = 0.04;
    private double rho_desired = 0.0;
    private Random random = new Random();
    private boolean show_plots = true; 
    
    
    /**
     * 
     */
    public BeamspotTransformFilter() {
    }

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        IAnalysisFactory af = aida.analysisFactory();
        IPlotterFactory pf = af.createPlotterFactory();
        plotter = pf.create("Beamspot");
        plotter.createRegions(1, 4);
        h_bs_2d = aida.histogram2D("h_bs_2d", 50, -0.5 , 0.5, 50,-0.5, 0.5);
        h_bs_z = aida.histogram1D("h_bs_z", 50, -0.5 , 0.5);
        h_bs_y = aida.histogram1D("h_bs_y", 50, -0.5 , 0.5);
        h_bs_x = aida.histogram1D("h_bs_x", 50, -0.5 , 0.5);
        plotter.region(0).plot(h_bs_2d);
        plotter.region(0).style().setParameter("hist2DStyle", "colorMap");
        plotter.region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter.region(1).plot(h_bs_x);
        plotter.region(2).plot(h_bs_y);
        plotter.region(3).plot(h_bs_z);
        if(show_plots) plotter.show();

        plotter2 = pf.create("Beamspot2");
        plotter2.createRegions(1, 4);
        h_bs_2d_proposal = aida.histogram2D("h_bs_2d_proposal", 50, -0.5 , 0.5, 50,-0.5, 0.5);
        plotter2.region(0).plot(h_bs_2d_proposal);
        plotter2.region(0).setStyle(plotter.region(0).style());
        if(show_plots) plotter2.show();
        
        plotter3 = pf.create("Beamspot_desired");
        plotter3.createRegions(1, 4);
        h_bs_2d_desired = aida.histogram2D("h_bs_2d_desired", 50, -0.5 , 0.5, 50,-0.5, 0.5);
        h_bs_z_desired = aida.histogram1D("h_bs_z_desired", 50, -0.5 , 0.5);
        h_bs_y_desired = aida.histogram1D("h_bs_y_desired", 50, -0.5 , 0.5);
        h_bs_x_desired = aida.histogram1D("h_bs_x_desired", 50, -0.5 , 0.5);
        plotter3.region(0).plot(h_bs_2d_desired);
        plotter3.region(0).setStyle(plotter.region(0).style());
        plotter3.region(1).plot(h_bs_x_desired);
        plotter3.region(2).plot(h_bs_y_desired);
        plotter3.region(3).plot(h_bs_z_desired);
        if(show_plots) plotter3.show();
        
    }
    
    protected void process(EventHeader event) {

        if ( !event.hasCollection(MCParticle.class, this.MCParticleCollectionName ) ) {
            logger.info("No MC particle collection found.");
            throw new Driver.NextEventException();
        }
        
        List<MCParticle> mcParticles = event.get(MCParticle.class, this.MCParticleCollectionName );        
        if( mcParticles.size() == 0) {
            logger.info("MC particle collection is empty.");
            throw new Driver.NextEventException();
        }
        
        logger.fine("found " + mcParticles.size() + " MC particles.");
        
        MCParticle aprime = getAprime(mcParticles);
        
        if( aprime == null) {
            logger.info("No A' found in particle collection.");
            throw new Driver.NextEventException();
        }
        
        // Find origin of the A'
        double origin[] = new double[3];
        origin[0] = aprime.getOriginX();
        origin[1] = aprime.getOriginY();
        origin[2] = aprime.getOriginZ();

        // Fill origin to histograms
        logger.fine("A' origin (" + origin[0] + "," + origin[1] + "," + origin[2]);
        h_bs_2d.fill(origin[0], origin[1]);
        h_bs_x.fill(origin[0]);
        h_bs_y.fill(origin[1]);
        h_bs_z.fill(origin[2]);
        

        // Calculate the PDF density for the proposal distribution
        double dens = density(origin[0], origin[1],mu1,s1,mu2,s2,rho);
        h_bs_2d_proposal.fill(origin[0],origin[1],dens);

        // Throw a random number and scale it to the range of the proposal distribution
        double r = random.nextDouble()*dens;

        // Calculate the PDF density for the desired distribution
        double dens_desired = density(origin[0], origin[1],mu1_desired,s1_desired,mu2_desired,s2_desired,rho_desired);        
        logger.fine("point (" + origin[0] + "," + origin[1] + ") for r " + r + " dens " + dens + " dens_desired " + dens_desired );

        // Reject the event if the density is larger than the desired PDF
        if( r > dens_desired) {
            logger.fine("reject point");
            throw new Driver.NextEventException();
        }
        logger.fine("accept point");

        
        // Fill some histograms
        h_bs_2d_desired.fill(origin[0],origin[1]);
        h_bs_x_desired.fill(origin[0]);
        h_bs_y_desired.fill(origin[1]);
        h_bs_z_desired.fill(origin[2]);

        return;
    }
    
    /**
     * Calculate the density for a bivariate normal distribution
     * @param x1 
     * @param x2
     * @param mu1 - mean
     * @param s1 - sigma
     * @param mu2 - mean 
     * @param s2 - sigma
     * @param rho - correlation coefficient between the variables
     * @return density for this point
     */
    private double density(double x1, double x2, double mu1, double s1, double mu2, double s2, double rho) {
        double z = (x1-mu1)*(x1-mu1)/(s1*s1) - 2*rho*(x1-mu1)*(x2-mu2)/(s1*s2) + (x2-mu2)*(x2-mu2)/(s2*s2);
        double C = 1/(2*Math.PI *s1*s2*Math.sqrt(1-rho*rho));
        double e = Math.exp(-z/(2*(1-rho*rho)));
        return C*e;
    }
    
//    private double density1D(double x1,  double mu1, double s1) {
//        double e = Math.exp(-1*(x1-mu1)*(x1-mu1)/(2*s1*s1));
//        double C = 1/(Math.sqrt(2*Math.PI)*s1);
//        return C*e;
//    }
    
    
    
    /**
     * Find the A' amond the list of MC partiles
     * @param mcParticles - list of particles
     * @return A' particle, else null
     */
    private MCParticle getAprime(List<MCParticle> mcParticles) {
        MCParticle ap = null;
        for (MCParticle mcp : mcParticles) {
            String s = "";
            for (MCParticle parent : mcp.getParents()) s += " <- " + Integer.toString(parent.getPDGID());
            String s1 = "";
            for (MCParticle parent : mcp.getDaughters()) s1 += " <- " + Integer.toString(parent.getPDGID());            
            logger.fine("mcp " + mcp.getPDGID() + " with " + mcp.getParents().size() + " parents " + s + " and with " + mcp.getDaughters().size() + " daughters " + s1);
            
            if( Math.abs(mcp.getPDGID()) == 622) {
                ap = mcp;
                break;
            }
        }
        return ap;
    }
        

   

    public double getMu1() {
        return mu1;
    }

    public void setMu1(double mu1) {
        this.mu1 = mu1;
    }

    public double getMu2() {
        return mu2;
    }

    public void setMu2(double mu2) {
        this.mu2 = mu2;
    }

    public double getS1() {
        return s1;
    }

    public void setS1(double s1) {
        this.s1 = s1;
    }

    public double getS2() {
        return s2;
    }

    public void setS2(double s2) {
        this.s2 = s2;
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    public double getMu1_desired() {
        return mu1_desired;
    }

    public void setMu1_desired(double mu1_desired) {
        this.mu1_desired = mu1_desired;
    }

    public double getMu2_desired() {
        return mu2_desired;
    }

    public void setMu2_desired(double mu2_desired) {
        this.mu2_desired = mu2_desired;
    }

    public double getS1_desired() {
        return s1_desired;
    }

    public void setS1_desired(double s1_desired) {
        this.s1_desired = s1_desired;
    }

    public double getS2_desired() {
        return s2_desired;
    }

    public void setS2_desired(double s2_desired) {
        this.s2_desired = s2_desired;
    }

    public double getRho_desired() {
        return rho_desired;
    }

    public void setRho_desired(double rho_desired) {
        this.rho_desired = rho_desired;
    }

    public boolean isShow_plots() {
        return show_plots;
    }

    public void setShow_plots(boolean show_plots) {
        this.show_plots = show_plots;
    }
    
}


