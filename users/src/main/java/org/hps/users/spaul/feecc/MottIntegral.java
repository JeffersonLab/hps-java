package org.hps.users.spaul.feecc;

public class MottIntegral {
	/**
	 * @param bin
	 * @param scale = Z^2alpha^2/(4*E^2)
	 * @param recoil = 2E/M
	 * @return the integral of 1/sin^4(th/2)*cos^2(th/2)/(1+a*sin^2(th/2)) times dPhi*sin(theta),
	 * which appears in the integral of mott scattering.
	 * 
	 * NOTE the sin(theta) is because dOmega = dTheta*dPhi*sin(theta)
	 */
	static double mottIntegral(double recoil, double scale, int bin, CustomBinning cb){
		double dPhi = 0;
		for(int i = 0; i< cb.phiMax[bin].length; i++)
			dPhi += 2*(cb.phiMax[bin][i] - cb.phiMin[bin][i]); //factor of 2 from top and bottom
		
		double Imax = integral(recoil, cb.thetaMax[bin]);
		       
		double Imin = integral(recoil, cb.thetaMin[bin]);
		double dI = Imax-Imin; 
		
		
		double retval = scale*dPhi*dI;
		return retval;
	}
	
	static double mottIntegral(double recoil, double scale, double thetaMin, double thetaMax){
		double Imax = integral(recoil, thetaMax);
	
		double Imin = integral(recoil, thetaMin);
		double dI = Imax-Imin; 
		
		double dPhi = 2*Math.PI;  //full range in phi
		double retval = scale*dPhi*dI;
		return retval;
	}
	
	static double integral(double a, double th){
		double sinth22 = Math.pow(Math.sin(th/2), 2);
		return 2*(-1/sinth22+(1+a)*(Math.log(2/sinth22+2*a)));
	}
	
	static double mottIntegralWithFormFactor(double recoil, double scale, double thetaMin, double thetaMax, FormFactor ff, int nsteps, double E){
		double sum = 0;
		double prev = -1;

		double dTheta = (thetaMax-thetaMin)/nsteps;
		for(int i = 0; i<nsteps; i++){
			double theta = i*(thetaMax-thetaMin)/nsteps+thetaMin;
			double I = integral(recoil, theta);
			if(i != 0)
			{
				double ts = Math.pow(Math.sin(theta/2),2);
				double q2 = 4*E*E*ts/(1+recoil*ts);
				double f2 = ff.getFormFactorSquared(q2);
				sum+= (I-prev)*f2;
			}
			prev = I;
			
		}
		double dPhi = 2*Math.PI;  //full range in phi
		double retval = scale*dPhi*sum;
		return retval;
	}
}
