/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.lit;

import junit.framework.TestCase;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;

/**
 *
 * @author ngraf
 */
public class TwoBodyDecayProviderTest extends TestCase
{

    public void testIt() throws Exception
    {
        AIDA aida = AIDA.defaultInstance();

        double mass = .040;
        double energy = 2.3;
        TwoBodyDecayProvider aprime = new TwoBodyDecayProvider(mass, energy);
        System.out.println(aprime);

        // generate some decays
        int nEvents = 10000;
        for (int i = 0; i < nEvents; ++i) {
            Lorentz4Vector[] decays = aprime.decayIt();
            for (int j = 0; j < decays.length; ++j) {
                Lorentz4Vector fourVec = decays[j];
//                System.out.println(decays[j]);
                aida.cloud1D("electron energy").fill(fourVec.E());
                aida.cloud2D("electron energy vs theta").fill(fourVec.E(), fourVec.theta());

            }
            aida.cloud2D("e1 vs e2").fill(decays[0].E(), decays[1].E());
            aida.cloud2D("theta1 vs theta2").fill(decays[0].theta(), decays[1].theta());
        }
        aida.saveAs(this.getClass().getSimpleName() + ".aida");
    }
}
