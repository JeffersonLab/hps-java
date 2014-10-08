package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.base.BaseCluster;

/**
 * Cluster with addition to include shared hits and set position
 * as calculated in full cluster code. 
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Holly Szumila <hvanc001@odu.edu>
 * 
 */
public class HPSEcalClusterIC extends BaseCluster {

    private CalorimeterHit seedHit = null;
    private long cellID;
    private ArrayList<CalorimeterHit> sharedHitList = new ArrayList<CalorimeterHit>(); 
    private double[] rawPosition = new double[2];

    
    
    static final double eCriticalW = 800.0*ECalUtils.MeV/(74+1);
    static final double radLenW = 8.8; //mm
    double[] electronPosAtDepth = new double[3];
    private boolean needsElectronPosCalculation = true;
    double[] photonPosAtDepth = new double[3];
    private boolean needsPhotonPosCalculation = true;
    
    public HPSEcalClusterIC(Long cellID) {
        this.cellID = cellID;
    }

    public HPSEcalClusterIC(CalorimeterHit seedHit) {
        this.seedHit = seedHit;
        this.cellID = seedHit.getCellID();
    }

    public CalorimeterHit getSeedHit() {
        if (seedHit == null) {
            CalorimeterHit hit = hits.get(0);
            if (hit == null) {
                throw new RuntimeException("HPSEcalCluster has no hits");
            }
            seedHit = new HPSCalorimeterHit(0.0, 0.0, cellID, hit.getType());
            seedHit.setMetaData(hit.getMetaData());
        }
        return seedHit;
    }
    /**
     * Input shared hits between two clusters. 
     */
    public void addSharedHit(CalorimeterHit sharedHit) {
    	sharedHitList.add(sharedHit);
    }
    /**
     * Return shared hit list between two clusters. 
     */
    public List<CalorimeterHit> getSharedHits() {
    	return sharedHitList;
    }  
    /**
     * Inputs the uncorrected x,y,z position of the cluster.
     */
    public void setRawPosition(double[] Position) {
    	rawPosition = Position;
    }  
    /**
     * Returns the uncorrected x,y,z position of the cluster.
     */
    @Override
    public double[] getPosition(){
    	return this.rawPosition;
    }   
    /**
     * Do an external calculation of the raw energy and set it. Includes shared hit distribution.
     */
    public void setRawEnergy(double rawEnergy){
    	raw_energy = rawEnergy;
    }
    /**
     * Inputs the corrected position of the cluster, see HPS Note 2014-001.
     */
    public void setCorrPosition(double[] Position) {
    	position = Position;
    }    
    /**
     * Returns the corrected position of the cluster. 
     */
    public double[] getCorrPosition(){
    	return this.position;
    }
    
    /**
     * Calculates energy correction based on cluster raw energy and particle type as per HPS Note 2014-001
     * @param pdg Particle id as per PDG
     * @param rawEnergy Raw Energy of the cluster (sum of hits with shared hit distribution)
     * @return Corrected Energy
     */    
    public double energyCorrection(int pdg, double rawEnergy){
  	   if (pdg == 11) { // Particle is electron
  		   double corrEnergy = rawEnergy / (-0.0027 * rawEnergy - 0.06 / (Math.sqrt(rawEnergy)) + 0.95);
  		  return corrEnergy;}
  	   else if (pdg == -11) { //Particle is positron
  		   double corrEnergy = rawEnergy / (-0.0096 * rawEnergy - 0.042 / (Math.sqrt(rawEnergy)) + 0.94);
  		  return corrEnergy;}
  	   else if (pdg == 22) { //Particle is photon
  		   double corrEnergy = rawEnergy / (0.0015 * rawEnergy - 0.047 / (Math.sqrt(rawEnergy)) + 0.94);
  		  return corrEnergy;}
  	   else { //Unknown 
  		   double corrEnergy = rawEnergy;
  		  return corrEnergy;}
  	   
     }   
    
    /**
     * Calculates position correction based on cluster raw energy, x calculated position, 
     * and particle type as per HPS Note 2014-001
     * @param pdg Particle id as per PDG
     * @param xCl Calculated x centroid position of the cluster, uncorrected, at face
     * @param rawEnergy Raw energy of the cluster (sum of hits with shared hit distribution)
     * @return Corrected x position
     */
    public double positionCorrection(int pdg, double xPos, double rawEnergy){
    	double xCl = xPos/10.0;//convert to mm
    	if (pdg == 11) { //Particle is electron
    		double xCorr = xCl-(0.0066/Math.sqrt(rawEnergy)-0.03)*xCl-
    				(0.028*rawEnergy-0.45/Math.sqrt(rawEnergy)+0.465);
    		return xCorr*10.0;}
    	else if (pdg == -11) {// Particle is positron
    		double xCorr = xCl-(0.0072/Math.sqrt(rawEnergy)-0.031)*xCl-
    				(0.007*rawEnergy+0.342/Math.sqrt(rawEnergy)+0.108);
    		return xCorr*10.0;}
    	else if (pdg == 22) {// Particle is photon
    		double xCorr = xCl-(0.005/Math.sqrt(rawEnergy)-0.032)*xCl-
    				(0.011*rawEnergy-0.037/Math.sqrt(rawEnergy)+0.294);
    		return xCorr*10.0;}
    	else { //Unknown 
    		double xCorr = xCl;
    		return xCorr*10.0;}
    	}
    
    
 /*   @Override
    public double[] getPosition() {
        //Electron by default!?
        return this.getPositionAtShowerMax(true);
    }
        
    public double[] getPositionAtShowerMax(boolean isElectron) {
        if( isElectron) {
            if(needsElectronPosCalculation) {
                this.calcPositionAtShowerMax(true);
            }
            return this.electronPosAtDepth;
        }
        else {
            if(needsPhotonPosCalculation) {
                this.calcPositionAtShowerMax(false);
            }
            return this.photonPosAtDepth;
        }  
    }
        
    public void calcPositionAtShowerMax(boolean isElectron) {
        double E = this.getEnergy();
        double y = E/eCriticalW;
        double Cj = isElectron ? -0.5 : 0.5;
        double tmax = Math.log(y) + Cj; //Maximum of dE/dt profile in units of rad. len. 
//        double dmax = tmax*radLenW; //mm
        double dmax = 0.0; //Changed this to readout crystal centroid at face
        if(isElectron) {
            electronPosAtDepth =  calculatePositionAtDepth(dmax);
        } else {
            photonPosAtDepth =  calculatePositionAtDepth(dmax);
        }
            
    }
    
    
    
    public double[] calculatePositionAtDepth(double dmax) 
    {
        return this.calculatePositionAtDepth(this.getCalorimeterHits(), dmax);
    }    
    
    public double[] calculatePositionAtDepth(List<CalorimeterHit> hits, double dmax)
    {
        //copy from package org.lcsim.recon.cluster.util.TensorClusterPropertyCalculator;

        double positionErrorLocal[] = new double[6];
        double directionErrorLocal[] = new double[6];
        double shapeParametersLocal[] = new double[6];
        double positionLocal[] = new double[3];
        double ithetaLocal;
        double iphiLocal;
        
        
        double[] mm_NE = new double[3];
        double[] mm_CE = new double[3];
        double[][] mm_PA = new double[3][3];
        for(int i=0;i<3;++i)
        {
            mm_NE[i] = 0.;
            mm_CE[i] = 0.;
            for(int j=0;j<3;++j)
            {mm_PA[i][j]= 0.;}
        }
        double Etot = 0.0;
        double Exx = 0.0;
        double Eyy = 0.0;
        double Ezz = 0.0;
        double Exy = 0.0;
        double Eyz = 0.0;
        double Exz = 0.0;
        double CEx = 0.0;
        double CEy = 0.0;
        double CEz = 0.0;
        double CEr = 0.0;
        double E1 = 0.0;
        double E2 = 0.0;
        double E3 = 0.0;
        double NE1 = 0.0;
        double NE2 = 0.0;
        double NE3 = 0.0;
        double Tr = 0.0;
        double M = 0.0;
        double Det = 0.0;
        int nhits = hits.size();
        for(int i=0;i<hits.size();i++)
        {
            CalorimeterHit hit = hits.get(i);
            //	CalorimeterIDDecoder decoder = hit.getDecoder();
            //	decoder.setID(hit.getCellID());
            //	double[] pos = new double[3];
            //	pos[0] = decoder.getX();
            //	pos[1] = decoder.getY();
            //	pos[2] = decoder.getZ();
            //double[] pos = hit.getPosition();
            //Find position at shower max
            IGeometryInfo geom = hit.getDetectorElement().getGeometry();
            double[] pos = geom.transformLocalToGlobal(VecOp.add(geom.transformGlobalToLocal(geom.getPosition()),(Hep3Vector)new BasicHep3Vector(0,0,dmax-1*((Trd)geom.getLogicalVolume().getSolid()).getZHalfLength()))).v();


            
            
//            System.out.println("global pos " + global_pos.toString());
//            System.out.println("local pos " + local_pos.toString());
//            System.out.println("local pos tmax " + local_pos_tmax.toString());
//            System.out.println("global pos tmax " + global_pos_tmax.toString());
//            
            //pos = global_pos_tmax.v();
            
            double E = hit.getCorrectedEnergy();
            Etot += E;
            CEx += E*pos[0];
            CEy += E*pos[1];
            CEz += E*pos[2];
            Exx += E*(pos[1]*pos[1] + pos[2]*pos[2]);
            Eyy += E*(pos[0]*pos[0] + pos[2]*pos[2]);
            Ezz += E*(pos[1]*pos[1] + pos[0]*pos[0]);
            Exy -= E*pos[0]*pos[1];
            Eyz -= E*pos[1]*pos[2];
            Exz -= E*pos[0]*pos[2];
        }
        CEx = CEx/Etot;
        CEy = CEy/Etot;
        CEz = CEz/Etot;
        double CErSq = CEx*CEx + CEy*CEy + CEz*CEz;
        CEr = Math.sqrt(CErSq);
        // now go to center of energy coords.
        if (nhits > 3 )
        {
            Exx = Exx - Etot*(CErSq - CEx*CEx);
            Eyy = Eyy - Etot*(CErSq - CEy*CEy);
            Ezz = Ezz - Etot*(CErSq - CEz*CEz);
            Exy = Exy + Etot*CEx*CEy;
            Eyz = Eyz + Etot*CEy*CEz;
            Exz = Exz + Etot*CEz*CEx;

            //
            Tr = Exx + Eyy + Ezz;
            double Dxx = Eyy*Ezz - Eyz*Eyz;
            double Dyy = Ezz*Exx - Exz*Exz;
            double Dzz = Exx*Eyy - Exy*Exy;
            M = Dxx + Dyy + Dzz;
            double Dxy = Exy*Ezz - Exz*Eyz;
            double Dxz = Exy*Eyz - Exz*Eyy;
            Det = Exx*Dxx - Exy*Dxy + Exz*Dxz;
            double xt = Tr*Tr - 3*M;
            double sqrtxt = Math.sqrt(xt);
            // eqn to solve for eigenvalues is x**3 - x**2*Tr + x*M - Det = 0
            // crosses y axis at -Det and inflection points are

            double mE1 = 0.;
            double mE2 = 0.;
            double mE3 = 0.;
            double a = (3*M - Tr*Tr)/3.;
            double b = (-2*Tr*Tr*Tr + 9*Tr*M -27*Det)/27.;
            double test = b*b/4. + a*a*a/27.;
            if(test >= 0.01)
            {
                //System.out.println("AbstractCluster: Only 1 real root!!!");
                //System.out.println("  nhits = " + nhits + "\n");
                //System.out.println(" a,b,test = " + a + " " + b + " " + test + "\n");
            }
            else
            {
                double temp;
                if(test >= 0.)temp = 1.;
                else temp = Math.sqrt(b*b*27./(-a*a*a*4.));
                if(b > 0.)temp = -temp;
                double phi = Math.acos(temp);
                double temp1 = 2.*Math.sqrt(-a/3.);
                mE1 = Tr/3. + 2.*Math.sqrt(-a/3.)*Math.cos(phi/3.);
                mE2 = Tr/3. + 2.*Math.sqrt(-a/3.)*Math.cos(phi/3. + 2.*Math.PI/3.);
                mE3 = Tr/3. + 2.*Math.sqrt(-a/3.)*Math.cos(phi/3. + 4.*Math.PI/3.);
            }
            if(mE1 < mE2)
            {
                if(mE1 < mE3)
                {
                    E1 = mE1;
                    if(mE2 < mE3)
                    {
                        E2 = mE2;
                        E3 = mE3;
                    }
                    else
                    {
                        E2 = mE3;
                        E3 = mE2;
                    }
                }
                else
                {
                    E1 = mE3;
                    E2 = mE1;
                    E3 = mE2;
                }
            }
            else
            {
                if(mE2 < mE3)
                {
                    E1 = mE2;
                    if(mE1 < mE3)
                    {
                        E2 = mE1;
                        E3 = mE3;
                    }
                    else
                    {
                        E2 = mE3;
                        E3 = mE1;
                    }
                }
                else
                {
                    E1 = mE3;
                    E2 = mE2;
                    E3 = mE1;
                }
            }

            NE1 = E1/Etot;
            NE2 = E2/Etot;
            NE3 = E3/Etot;
            double[] EV = new double[3];
            EV[0] = E1;
            EV[1] = E2;
            EV[2] = E3;
            // Now calculate principal axes
	    // For eigenvalue EV, the axis is (nx, ny, nz) where:
	    //    (Exx - EV)nx + (Exy)ny + (Exz)nz = 0
	    //    (Eyx)nx + (Eyy - EV)ny + (Eyz)nz = 0
	    //    (Ezx)nx + (Ezy)ny + (Ezz - EV)nz = 0
	    // Setting nx = 1, we have:
	    //    (Exx - EV) + (Exy)ny + (Exz)nz = 0
	    //    (Eyx) + (Eyy - EV)ny + (Eyz)nz = 0
	    //    (Ezx) + (Ezy)ny + (Ezz - EV)nz = 0
	    // and so
	    //    (Exy)ny = EV - Exx - (Exz)nz  =>  ny = (EV - Exx - Exz*nz)/Exy
	    // What if Exy = 0? Then provided Eyz is non-zero we can write:
	    //    (Ezx) + (Ezy)ny + (Ezz - EV)nz = 0
	    //    ny = (Exz - (Ezz-EV)*nz)/Eyz
	    // What if Exy = 0 and Eyz = 0 but (Eyy - EV) is non-zero?
	    //    (Eyy - EV)ny + (Eyz)nz = 0
	    //    ny = -(Eyz*nz)/(Eyy-EV)

	    // In the pathological case where Exz = Eyz = Ezz = 0:
	    //    (Exx - EV)nx + (Exy)ny = 0  =>  ny/nx = -(Exx-EV)/Exy
	    //    (Eyx)nx + (Eyy - EV)ny = 0  =>  ny/nx = -Eyx/(Eyy-EV)
	    //    (EV)nz = 0
	    // so
	    //     -ny/nx = (EV-Exx)/Exy = Eyx/(EV-Eyy)
	    // But watch out for order! Recalculate eigenvalues for this pathological case.
	    //     (EV-Exx)(EV-Eyy) = Eyx*Exy
	    //     EV^2 - EV(Exx+Eyy) + Exx*Eyy - Eyx*Exy = 0
	    // 
	    // In another pathological case, Exz = Exy = 0:
	    //    (Exx - EV)nx = 0
	    //    (Eyy - EV)ny + (Eyz)nz = 0 => ny/nz = -(Eyz)/(Eyy-EV)
	    //    (Ezy)ny + (Ezz - EV)nz = 0 => ny/nz = -(Ezz-EV)/(Ezy)
	    // so we cannot set nx = 1. Instead, write:
	    //    -ny/nz = (Eyz)/(Eyy-EV) = (Ezz-EV)/(Ezy)
	    // Then
	    //    (Eyz)(Ezy) = (Eyy-EV)(Ezz-EV)
	    //    (Eyz)^2 = (Eyy)(Ezz) - (Eyy)(EV) - (Ezz)(EV) + (EV)^2
	    //    EV^2 - EV(Eyy+Ezz) + Eyy*Ezz - Eyz*Eyz = 0

	    // Handle pathological case
	    if (Exz == 0.0 && Eyz == 0.0) {
		// Recompute eigenvectors.
		EV[0] = 0.5*(Exx+Eyy) + 0.5*Math.sqrt((Exx+Eyy)*(Exx+Eyy) + 4.0*Exy*Exy);
		EV[1] = 0.5*(Exx+Eyy) - 0.5*Math.sqrt((Exx+Eyy)*(Exx+Eyy) + 4.0*Exy*Exy);
		EV[2] = 0.0;
		for( int i = 0 ; i < 2 ; i++ ) {
		    double nx_over_ny = Exy / (Exx-EV[i]);
		    double nx_unnormalized = nx_over_ny;
		    double ny_unnormalized = 1.0;
		    double norm = Math.sqrt(nx_unnormalized*nx_unnormalized + ny_unnormalized*ny_unnormalized);
		    mm_PA[i][0] = ny_unnormalized/norm;
		    mm_PA[i][1] = nx_unnormalized/norm;
		    mm_PA[i][2] = 0.0;
		}
		// ... and now set third eigenvector to the z direction:
		mm_PA[2][0] = 0.0;
		mm_PA[2][1] = 0.0;
		mm_PA[2][2] = 1.0;
	    } else if (Exz == 0.0 && Exy == 0.0) {
		// Another pathological case
		EV[0] = 0.5*(Eyy+Ezz) + 0.5*Math.sqrt((Eyy+Ezz)*(Eyy+Ezz) + 4.0*Eyz*Eyz);
		EV[1] = 0.5*(Eyy+Ezz) - 0.5*Math.sqrt((Eyy+Ezz)*(Eyy+Ezz) + 4.0*Eyz*Eyz);
		EV[2] = 0.0;
		for( int i = 0 ; i < 2 ; i++ ) {
		    double ny_over_nz = Eyz / (Eyy-EV[i]);
		    double ny_unnormalized = ny_over_nz;
		    double nz_unnormalized = 1.0;
		    double norm = Math.sqrt(ny_unnormalized*ny_unnormalized + nz_unnormalized*nz_unnormalized);
		    mm_PA[i][0] = nz_unnormalized/norm;
		    mm_PA[i][1] = ny_unnormalized/norm;
		    mm_PA[i][2] = 0.0;
		}
		mm_PA[2][0] = 0.0;
		mm_PA[2][1] = 0.0;
		mm_PA[2][2] = 1.0;
	    } else {
		for( int i = 0 ; i < 3 ; i++ )
		    {
			double[] C = new double[3];
			C[0] = 1.0;
			C[2] = (Exy*Exy + (Eyy - EV[i])*(EV[i] - Exx))/
			    ((Eyy - EV[i])*Exz - Eyz*Exy);
			C[1] = (EV[i] - Exx - Exz*C[2])/Exy;
			if (Exy == 0.0) {
			    // Recompute
			    if (Eyz != 0.0) {
				// ny = (Exz - (Ezz-EV)*nz)/Eyz
				C[1] = (Exz - (Ezz-EV[i])*C[2])/Eyz;
			    } else {
				// ny = -(Eyz*nz)/(Eyy-EV)
				C[1] = -(Eyz*C[2])/(Eyy-EV[i]);
			    }
			}
			double norm = Math.sqrt(C[0]*C[0] + C[1]*C[1] + C[2]*C[2]);
			mm_PA[i][0] = C[0]/norm;
			mm_PA[i][1] = C[1]/norm;
			mm_PA[i][2] = C[2]/norm;
		    }
	    }
        }
        mm_NE[0] = NE1;
        mm_NE[1] = NE2;
        mm_NE[2] = NE3;
        mm_CE[0] = CEx;
        mm_CE[1] = CEy;
        mm_CE[2] = CEz;
        for(int i=0;i<6;i++)
        {
            positionErrorLocal[i] = 0.;
            directionErrorLocal[i] = 0.;
            shapeParametersLocal[i] = 0.;
        }
        for(int i=0;i<3;i++)
        {
            positionLocal[i] = mm_CE[i];
            shapeParametersLocal[i] = mm_NE[i];
        }
        if(nhits > 3)
        {
            double dr = Math.sqrt(  (position[0]+mm_PA[0][0])*(position[0]+mm_PA[0][0]) +
                    (position[1]+mm_PA[0][1])*(position[1]+mm_PA[0][1]) +
                    (position[2]+mm_PA[0][2])*(position[2]+mm_PA[0][2]) ) -
                    Math.sqrt(	(position[0])*(position[0]) +
                    (position[1])*(position[1]) +
                    (position[2])*(position[2]) ) ;
            double sign = 1.;
            if(dr < 0.)sign = -1.;
            itheta = Math.acos(sign*mm_PA[0][2]);
            iphi = Math.atan2(sign*mm_PA[0][1],sign*mm_PA[0][0]);
        }
        else
        {
            itheta = 999.;
            iphi = 999.;
        }
    
        return positionLocal;
    }
    
  */  
    
    
}
