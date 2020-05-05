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
public class CbmLitMaterialEffectsImpTest extends TestCase {

    public void testIt() {
        CbmLitMaterialInfo silicon = CbmLitMaterialInfo.getSilicon();
        silicon.SetLength(0.320);
        silicon.SetZ(20.);
        CbmLitTrackParam par = makeTestTrackParam();
        System.out.println("starting par "+par +" momentum "+(1/par.GetQp()));
        CbmLitMaterialEffectsImp matEffects = new CbmLitMaterialEffectsImp();
        // Electron energy loss...
        double energyLossBH = matEffects.BetheHeitler(par, silicon);
        System.out.println("Bethe-Heitler "+energyLossBH);
        
        matEffects.Update(par, silicon, -11, true);
        System.out.println("ending par "+par+" momentum "+(1/par.GetQp()));
        
    }

    private CbmLitTrackParam makeTestTrackParam() {
        CbmLitTrackParam p = new CbmLitTrackParam();
        double x = 0.;
        double y = 0.;
        double z = 0.;
        double dxdz = 0.0;
        double dydz = 0.0;
        double qP = 1./2.3; // 2016 Beam Energy
        p.SetX(x);
        p.SetY(y);
        p.SetZ(z);
        p.SetTx(dxdz);
        p.SetTy(dydz);
        p.SetQp(qP);
        return p;
    }

//    /**
//     * Test of Update method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testUpdate() {
//        System.out.println("Update");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        int pdg = 0;
//        boolean downstream = false;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        LitStatus expResult = null;
//        LitStatus result = instance.Update(par, mat, pdg, downstream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of AddEnergyLoss method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testAddEnergyLoss() {
//        System.out.println("AddEnergyLoss");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        instance.AddEnergyLoss(par, mat);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of AddThickScatter method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testAddThickScatter() {
//        System.out.println("AddThickScatter");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        instance.AddThickScatter(par, mat);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of AddThinScatter method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testAddThinScatter() {
//        System.out.println("AddThinScatter");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        instance.AddThinScatter(par, mat);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of CalcThetaSq method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testCalcThetaSq() {
//        System.out.println("CalcThetaSq");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.CalcThetaSq(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of EnergyLoss method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testEnergyLoss() {
//        System.out.println("EnergyLoss");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.EnergyLoss(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of dEdx method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testDEdx() {
//        System.out.println("dEdx");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.dEdx(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of BetheBloch method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testBetheBloch() {
//        System.out.println("BetheBloch");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.BetheBloch(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of BetheBlochElectron method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testBetheBlochElectron() {
//        System.out.println("BetheBlochElectron");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.BetheBlochElectron(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of CalcQpAfterEloss method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testCalcQpAfterEloss() {
//        System.out.println("CalcQpAfterEloss");
//        double qp = 0.0;
//        double eloss = 0.0;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.CalcQpAfterEloss(qp, eloss);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of CalcSigmaSqQp method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testCalcSigmaSqQp() {
//        System.out.println("CalcSigmaSqQp");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.CalcSigmaSqQp(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of CalcSigmaSqQpElectron method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testCalcSigmaSqQpElectron() {
//        System.out.println("CalcSigmaSqQpElectron");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.CalcSigmaSqQpElectron(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of CalcI method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testCalcI() {
//        System.out.println("CalcI");
//        double Z = 0.0;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.CalcI(Z);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of BetheHeitler method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testBetheHeitler() {
//        System.out.println("BetheHeitler");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.BetheHeitler(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of PairProduction method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testPairProduction() {
//        System.out.println("PairProduction");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.PairProduction(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of BetheBlochSimple method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testBetheBlochSimple() {
//        System.out.println("BetheBlochSimple");
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.BetheBlochSimple(mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of MPVEnergyLoss method, of class CbmLitMaterialEffectsImp.
//     */
//    @Test
//    public void testMPVEnergyLoss() {
//        System.out.println("MPVEnergyLoss");
//        CbmLitTrackParam par = null;
//        CbmLitMaterialInfo mat = null;
//        CbmLitMaterialEffectsImp instance = new CbmLitMaterialEffectsImp();
//        double expResult = 0.0;
//        double result = instance.MPVEnergyLoss(par, mat);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//    
}
