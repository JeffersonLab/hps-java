/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.lit;

import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class CbmLitTrackFitterImpTest extends TestCase
{
 public void testIt()
 {
     // some z plane locations...
	double zpos[]  = {10., 20., 30., 40., 50., 60., 70.};
        // a constant magnetic field...
	ConstantMagneticField field = new ConstantMagneticField(0.,1.,0.);
// create an extrapolator...
        CbmLitRK4TrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);
        
 }
}
