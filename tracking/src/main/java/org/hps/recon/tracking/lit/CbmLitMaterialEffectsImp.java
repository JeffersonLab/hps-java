package org.hps.recon.tracking.lit;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.Math.log;
import static java.lang.Math.exp;
import static java.lang.Math.pow;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitMaterialEffectsImp implements CbmLitMaterialEffects {

    boolean fDownstream = true; // Propagation direction
    double fMass = 0.1396; // Hypothesis on particle mass
    boolean fIsElectron = false; // True if particle is an electron or positron
    boolean fIsMuon = true; // True if particle is muon

    public LitStatus Update(CbmLitTrackParam par, CbmLitMaterialInfo mat, int pdg, boolean downstream) {
        if (mat.GetLength() * mat.GetRho() < 1e-10) {
            return LitStatus.kLITSUCCESS;
        }

        fDownstream = downstream;
//   TDatabasePDG* db = TDatabasePDG::Instance();
//   TParticlePDG* particle = db.GetParticle(pdg);
//   assert(particle != NULL);
//   fMass = particle.Mass();
        fIsElectron = (abs(pdg) == 11) ? true : false;
        fIsMuon = (abs(pdg) == 13) ? true : false;

        if (fIsElectron) {
            fMass = 0.000511;
        }
        if (fIsMuon) {
            fMass = 0.105;
        }
        AddEnergyLoss(par, mat);

//   AddThinScatter(par, mat);
        AddThickScatter(par, mat);

        return LitStatus.kLITSUCCESS;
    }

    void AddEnergyLoss(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        if (fIsElectron) {
            double radLength = mat.GetLength() / mat.GetRL();
            double t;

            if (!fDownstream) {
                t = radLength;
            } else {
                t = -radLength;
            }

            double qp = par.GetQp();
            qp *= exp(-t);
            par.SetQp(qp);

            double cov = par.GetCovariance(14);
            cov += CalcSigmaSqQpElectron(par, mat);
            par.SetCovariance(14, cov);
        } else {
            double Eloss = EnergyLoss(par, mat);
            par.SetQp(CalcQpAfterEloss(par.GetQp(), Eloss));

            double cov = par.GetCovariance(14);
            cov += CalcSigmaSqQp(par, mat);
            par.SetCovariance(14, cov);
        }
    }

    void AddThickScatter(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        if (mat.GetLength() < 1e-10) {
            return;
        }

        double tx = par.GetTx();
        double ty = par.GetTy();
        double thickness = mat.GetLength(); //cm
        double thetaSq = CalcThetaSq(par, mat);

        double t = 1 + tx * tx + ty * ty;

        double Q33 = (1 + tx * tx) * t * thetaSq;
        double Q44 = (1 + ty * ty) * t * thetaSq;
        double Q34 = tx * ty * t * thetaSq;

        double T23 = (thickness * thickness) / 3.0;
        double T2 = thickness / 2.0;

        double D = (fDownstream) ? 1. : -1.;

        double[] C = par.GetCovMatrix();

        C[0] += Q33 * T23;
        C[1] += Q34 * T23;
        C[2] += Q33 * D * T2;
        C[3] += Q34 * D * T2;

        C[5] += Q44 * T23;
        C[6] += Q34 * D * T2;
        C[7] += Q44 * D * T2;

        C[9] += Q33;
        C[10] += Q34;

        C[12] += Q44;

        par.SetCovMatrix(C);
    }

    void AddThinScatter(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        if (mat.GetLength() < 1e-10) {
            return;
        }
        double tx = par.GetTx();
        double ty = par.GetTy();
        double thetaSq = CalcThetaSq(par, mat);

        double t = 1 + tx * tx + ty * ty;

        double Q33 = (1 + tx * tx) * t * thetaSq;
        double Q44 = (1 + ty * ty) * t * thetaSq;
        double Q34 = tx * ty * t * thetaSq;

        double[] C = par.GetCovMatrix();
        C[9] += Q33;
        C[12] += Q44;
        C[10] += Q34;
        par.SetCovMatrix(C);
    }

    double CalcThetaSq(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double p = abs(1. / par.GetQp()); //GeV
        double E = sqrt(fMass * fMass + p * p);
        double beta = p / E;
        double x = mat.GetLength(); //cm
        double X0 = mat.GetRL(); //cm
        double bcp = beta * p;
        double z = 1.;

        double theta = 0.0136 * (1. / bcp) * z * sqrt(x / X0)
                * (1. + 0.038 * log(x / X0));
        return theta * theta;
    }

    double EnergyLoss(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double length = mat.GetRho() * mat.GetLength();
        return dEdx(par, mat) * length;
        //return MPVEnergyLoss(par, mat);
    }

    double dEdx(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double dedx = BetheBloch(par, mat);
// dedx += BetheHeitler(par, mat);
// if (fIsMuon) dedx += PairProduction(par, mat);
        return dedx;
    }

    double BetheBloch(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double K = 0.000307075; // GeV * g^-1 * cm^2
        double z = (par.GetQp() > 0.) ? 1 : -1.;
        double Z = mat.GetZ();
        double A = mat.GetA();

        double M = fMass;
        double p = abs(1. / par.GetQp()); //GeV
        double E = sqrt(M * M + p * p);
        double beta = p / E;
        double betaSq = beta * beta;
        double gamma = E / M;
        double gammaSq = gamma * gamma;

        double I = CalcI(Z) * 1e-9; // GeV

        double me = 0.000511; // GeV
        double ratio = me / M;
        double Tmax = (2 * me * betaSq * gammaSq) / (1 + 2 * gamma * ratio + ratio * ratio);

        // density correction
        double dc = 0.;
        if (p > 0.5) { // for particles above 1 Gev
            double rho = mat.GetRho();
            double hwp = 28.816 * sqrt(rho * Z / A) * 1e-9; // GeV
            dc = log(hwp / I) + log(beta * gamma) - 0.5;
        }

        return K * z * z * (Z / A) * (1. / betaSq) * (0.5 * log(2 * me * betaSq * gammaSq * Tmax / (I * I)) - betaSq - dc);
    }

    double BetheBlochElectron(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double K = 0.000307075; // GeV * g^-1 * cm^2
        //myf z = (par.GetQp() > 0.) ? 1 : -1.;
        double Z = mat.GetZ();
        double A = mat.GetA();

        double me = 0.000511; // GeV;
        double p = abs(1. / par.GetQp()); //GeV
        double E = sqrt(me * me + p * p);
        double gamma = E / me;

        double I = CalcI(Z) * 1e-9; // GeV

        if (par.GetQp() > 0) { // electrons
            return K * (Z / A) * (log(2 * me / I) + 1.5 * log(gamma) - 0.975);
        } else { //positrons
            return K * (Z / A) * (log(2 * me / I) + 2. * log(gamma) - 1.);
        }
    }

    double CalcQpAfterEloss(
            double qp,
            double eloss) {
        double massSq = fMass * fMass;
        double p = abs(1. / qp);
        double E = sqrt(p * p + massSq);
        double q = (qp > 0) ? 1. : -1.;
        if (!fDownstream) {
            eloss *= -1.0;
        } // TODO check this
        double Enew = E - eloss;
        double pnew = (Enew > fMass) ? sqrt(Enew * Enew - massSq) : 0.;
        if (pnew != 0) {
            return q / pnew;
        } else {
            return 1e5;
        }

        //if (!fDownstream) eloss *= -1.0;
        //if (p > 0.) p -= eloss;
        //else p += eloss;
        //return 1./p;
    }

    double CalcSigmaSqQp(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double P = abs(1. / par.GetQp()); // GeV
        double XMASS = fMass; // GeV
        double E = sqrt(P * P + XMASS * XMASS);
        double Z = mat.GetZ();
        double A = mat.GetA();
        double RHO = mat.GetRho();
        double STEP = mat.GetLength();
        double EMASS = 0.511 * 1e-3; // GeV

        double BETA = P / E;
        double GAMMA = E / XMASS;

        // Calculate xi factor (KeV).
        double XI = (153.5 * Z * STEP * RHO) / (A * BETA * BETA);

        // Maximum energy transfer to atomic electron (KeV).
        double ETA = BETA * GAMMA;
        double ETASQ = ETA * ETA;
        double RATIO = EMASS / XMASS;
        double F1 = 2. * EMASS * ETASQ;
        double F2 = 1. + 2. * RATIO * GAMMA + RATIO * RATIO;
        double EMAX = 1e6 * F1 / F2;

        double DEDX2 = XI * EMAX * (1. - (BETA * BETA / 2.)) * 1e-12;

        double SDEDX = (E * E * DEDX2) / pow(P, 6);

        return abs(SDEDX);
    }

    double CalcSigmaSqQpElectron(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double x = mat.GetLength(); //cm
        double X0 = mat.GetRL(); //cm
        return par.GetQp() * par.GetQp()
                * (exp(-x / X0 * log(3.0) / log(2.0))
                - exp(-2.0 * x / X0));
    }

    double CalcI(
            double Z) {
        // mean excitation energy in eV
        if (Z > 16.) {
            return 10 * Z;
        } else {
            return 16 * pow(Z, 0.9);
        }
    }

    double BetheHeitler(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double M = fMass; //GeV
        double p = abs(1. / par.GetQp());  // GeV
        double rho = mat.GetRho();
        double X0 = mat.GetRL();
        double me = 0.000511; // GeV
        double E = sqrt(M * M + p * p);
        double ratio = me / M;

        return (E * ratio * ratio) / (X0 * rho);
    }

    double PairProduction(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double p = abs(1. / par.GetQp());  // GeV
        double M = fMass; //GeV
        double rho = mat.GetRho();
        double X0 = mat.GetRL();
        double E = sqrt(M * M + p * p);

        return 7e-5 * E / (X0 * rho);
    }

    double BetheBlochSimple(
            CbmLitMaterialInfo mat) {
        return CbmLitDefaultSettings.ENERGY_LOSS_CONST * mat.GetZ() / mat.GetA();
    }

    double MPVEnergyLoss(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat) {
        double M = fMass * 1e3; //MeV
        double p = abs(1. / par.GetQp()) * 1e3;  // MeV

//   myf rho = mat.GetRho();
        double Z = mat.GetZ();
        double A = mat.GetA();
        double x = mat.GetRho() * mat.GetLength();

        double I = CalcI(Z) * 1e-6; // MeV

        double K = 0.307075; // MeV g^-1 cm^2
        double j = 0.200;

        double E = sqrt(M * M + p * p);
        double beta = p / E;
        double betaSq = beta * beta;
        double gamma = E / M;
        double gammaSq = gamma * gamma;

        double ksi = (K / 2.) * (Z / A) * (x / betaSq); // MeV

//   myf hwp = 28.816 * sqrt(rho*Z/A) * 1e-6 ; // MeV
//   myf dc = log(hwp/I) + log(beta*gamma) - 0.5;
//   dc *= 2;
        double dc = 0.;

        double eloss = ksi * (log(2 * M * betaSq * gammaSq / I) + log(ksi / I) + j - betaSq - dc);

        return eloss * 1e-3; //GeV
    }
}
