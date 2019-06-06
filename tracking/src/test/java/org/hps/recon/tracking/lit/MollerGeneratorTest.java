package org.hps.recon.tracking.lit;

import static java.lang.Math.sqrt;
import junit.framework.TestCase;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 * Simple toy to generate some particle pairs
 * NOT A CORRECT SIMULATION
 * MOLLER SCATTERING IS NOT  THE SAME AS PARTICLE DECAY
 * 
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class MollerGeneratorTest extends TestCase
{

    AIDA aida = AIDA.defaultInstance();

    public void testIt() throws Exception
    {
        double emass = 0.000511;
        double beamE = 1.056;
        double emom = sqrt(beamE * beamE - emass * emass);

        // create the beam electron
        Momentum4Vector vec = new Momentum4Vector(0., 0., emom, beamE);
        // add on the target electron
        vec.plusEquals(0., 0., 0., emass);
        
        Momentum4Vector beamElectron = new Momentum4Vector(0., 0., emom, beamE);
        
        // lets decay it
        Lorentz4Vector[] outgoing = vec.twobodyDecay(emass, emass);
            
        // can we reconstruct one of the decay products?
        Lorentz4Vector one = outgoing[0];
        Lorentz4Vector theOther = outgoing[1];
        Lorentz4Vector ghost = beamElectron.minus(beamElectron, one);
        
        System.out.println("beam " + beamElectron);

        System.out.println("one "+ one);
        System.out.println("theOther "+theOther);
        System.out.println("ghost "+ghost);
        System.out.println("px: "+theOther.px()+" "+ghost.px());
        System.out.println("py: "+theOther.py()+" "+ghost.py());
        System.out.println("pz: "+theOther.pz()+" "+ghost.pz());
        System.out.println("E : "+theOther.E()+" "+ghost.E());
        System.out.println("m2: "+theOther.mass2()+" "+ghost.mass2());
        System.out.println("m : "+theOther.mass()+" "+ghost.mass());
        
        //what's its mass?
        System.out.println("Moller mass " + vec.mass());

        // generate some decays
        int nEvents = 10000;
        for (int i = 0; i < nEvents; ++i) {
            Lorentz4Vector[] decays = vec.twobodyDecay(emass, emass);
            // check on rough HPS acceptance...
            if (decays[0].E() > 0.3 && decays[0].E() < 0.75) {
                for (int j = 0; j < decays.length; ++j) {
                    Lorentz4Vector fourVec = decays[j];
                    //System.out.println(decays[j]);
                    aida.cloud1D("electron energy").fill(fourVec.E());
                    aida.cloud2D("electron energy vs theta").fill(fourVec.E(), fourVec.theta());

                }
                aida.cloud2D("e1 vs e2").fill(decays[0].E(), decays[1].E());
                aida.cloud2D("theta1 vs theta2").fill(decays[0].theta(), decays[1].theta());
            }
        }
        aida.saveAs(this.getClass().getSimpleName() + ".aida");

    }
}
