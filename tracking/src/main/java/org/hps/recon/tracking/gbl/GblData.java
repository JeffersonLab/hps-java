package org.hps.recon.tracking.gbl;

import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.VVector;

public class GblData {
   
    enum dataBlockType {
        None, InternalMeasurement, InternalKink, ExternalSeed, ExternalMeasurement;
    }
    
    int theLabel; // /< Label (of measurements point)
    int theRow; // /< Row number (of measurement)
    dataBlockType theType; // /<Type of measurement
    double theValue; // /< Value (residual)
    double thePrecision; // /< Precision (1/sigma**2)
    double theDownWeight; // /< Down-weighting factor (0-1)
    double thePrediction; // /< Prediction from fit
    
    List<Integer> theParameters = new ArrayList<Integer>(); // /< List of fit parameters (with non zero derivatives)
    List<Double> theDerivatives = new ArrayList<Double>(); // /< List of derivatives for fit
    List<Integer> globalLabels = new ArrayList<Integer>(); // /< Labels for global derivatives
    List<Double> globalDerivatives = new ArrayList<Double>(); // /< Global derivatives
        

    // / Create data block.
    /**
     * \param [in] aLabel Label of corresponding point \param [in] aValue Value of (scalar) measurement \param [in]
     * aPrec Precision of (scalar) measurement
     */
    GblData(int aLabel, dataBlockType aType, double aValue, double aPrec) {
        
        theType  = aType;
        theLabel = aLabel;
        theValue = aValue;
        thePrecision = aPrec;
        theDownWeight = 1.;
        thePrediction = 0.;
    }

    
    public int getLabel() {
        return theLabel;
    }
                                                
    public dataBlockType getType() {
        return theType;
    }
    
    // / Add derivatives from measurement.
    /**
     * Add (non-zero) derivatives to data block. Fill list of labels of used fit parameters. \param [in] iRow Row index
     * (0-4) in up to 5D measurement \param [in] labDer Labels for derivatives \param [in] matDer Derivatives (matrix)
     * 'measurement vs track fit parameters' \param [in] iOff Offset for row index for additional parameters \param [in]
     * derLocal Derivatives (matrix) for additional local parameters \param [in] labGlobal Labels for additional global
     * (MP-II) parameters \param [in] derGlobal Derivatives (matrix) for additional global (MP-II) parameters \param
     * [in] extOff Offset for external parameters \param [in] extDer Derivatives for external Parameters
     */
    void addDerivatives(int iRow, List<Integer> labDer, Matrix matDer, int iOff, Matrix derLocal,
            List<Integer> labGlobal, Matrix derGlobal, int extOff, Matrix extDer) {
        int nLocal = 0;
        int nExt = 0;
        theRow = iRow - iOff;
        
        if (derLocal != null) {
            nLocal = derLocal.getColumnDimension();
        }
        if (extDer != null) {
            nExt = extDer.getColumnDimension();
        }
        int nParMax = 5 + nLocal + nExt;
        // theParameters.reserve(nParMax); // have to be sorted
        // theDerivatives.reserve(nParMax);
        
        if (derLocal != null) {
            for (int i = 0; i < derLocal.getColumnDimension(); ++i) // local derivatives
            {
                if (derLocal.get(iRow - iOff, i) != 0) {
                    theParameters.add(i + 1);
                    theDerivatives.add(derLocal.get(iRow - iOff, i));
                }
            }
        }
        
        if (extDer != null) {
            for (int i = 0; i < extDer.getColumnDimension(); ++i) // external derivatives
            {
                if (extDer.get(iRow - iOff, i) > 0) {
                    theParameters.add(extOff + i + 1);
                    theDerivatives.add(extDer.get(iRow - iOff, i));
                }
            }
        }
        for (int i = 0; i < 5; ++i) // curvature, offset derivatives
        {
            if (labDer.get(i) != 0 && matDer.get(iRow, i) != 0) {
                theParameters.add(labDer.get(i));
                theDerivatives.add(matDer.get(iRow, i));
            }
        }
        
        globalLabels = labGlobal;
        for (int i = 0; i < derGlobal.getColumnDimension(); ++i) // global derivatives
        {
            globalDerivatives.add(derGlobal.get(iRow - iOff, i));
        }
    }
    
    // / Add derivatives from kink.
    /**
     * Add (non-zero) derivatives to data block. Fill list of labels of used fit parameters. \param [in] iRow Row index
     * (0-1) in 2D kink \param [in] labDer Labels for derivatives \param [in] matDer Derivatives (matrix) 'kink vs track
     * fit parameters' \param [in] extOff Offset for external parameters \param [in] extDer Derivatives for external
     * Parameters
     */
    
    void addDerivatives(int iRow, List<Integer> labDer, Matrix matDer, int extOff, Matrix extDer) {
        int nExtDer = 0;
        if (extDer != null) {
            nExtDer = extDer.getColumnDimension();
        }
        theRow = iRow;
        int nParMax = 7 + nExtDer;
        // theParameters.reserve(nParMax); // have to be sorted
        // theDerivatives.reserve(nParMax);
        
        if (extDer != null) {
            for (int i = 0; i < extDer.getColumnDimension(); ++i) // external derivatives
            {
                if (extDer.get(iRow, i) != 0) {
                    theParameters.add(extOff + i + 1);
                    theDerivatives.add(extDer.get(iRow, i));
                }
            }
        }
        for (int i = 0; i < 7; ++i) // curvature, offset derivatives
        {
            if (labDer.get(i) != 0 && matDer.get(iRow, i) != 0) {
                theParameters.add(labDer.get(i));
                theDerivatives.add(matDer.get(iRow, i));
            }
        }
    }

    /// Add derivatives from external seed.
    /**
     * Add (non-zero) derivatives to data block. Fill list of labels of used fit parameters.
     * \param [in] index Labels for derivatives
     * \param [in] derivatives Derivatives (vector)
     */

    void addDerivatives(List<Integer> index, List<Double> derivatives) {
        for (int i=0; i<derivatives.size(); ++i) { //any derivatives
            if (derivatives.get(i) != 0.) { // Not really safe to check for 0. on a Double. TODO::Fix this
                theParameters.add(index.get(i));
                theDerivatives.add(derivatives.get(i));
            }
        }
    }
    
    // void addDerivatives(const std::vector<unsigned int> &index,
    // const std::vector<double> &derivatives) {
    // for (unsigned int i = 0; i < derivatives.size(); ++i) // any derivatives
    // {
    // if (derivatives[i]) {
    // theParameters.push_back(index[i]);
    // theDerivatives.push_back(derivatives[i]);
    // }
    // }
    // }
    // / Calculate prediction for data from fit (by GblTrajectory::fit).
    void setPrediction(VVector aVector) {
        
        thePrediction = 0.;
        for (int i = 0; i < theDerivatives.size(); ++i) {
            thePrediction += theDerivatives.get(i) * aVector.get(theParameters.get(i) - 1);
        }
    }

    // /// Outlier down weighting with M-estimators (by GblTrajectory::fit).
    // /**
    // * \param [in] aMethod M-estimator (1: Tukey, 2:Huber, 3:Cauchy)
    // */
    // double setDownWeighting(unsigned int aMethod) {
    //
    // double aWeight = 1.;
    // double scaledResidual = fabs(theValue - thePrediction) * sqrt(thePrecision);
    // if (aMethod == 1) // Tukey
    // {
    // if (scaledResidual < 4.6851) {
    // aWeight = (1.0 - 0.045558 * scaledResidual * scaledResidual);
    // aWeight *= aWeight;
    // } else {
    // aWeight = 0.;
    // }
    // } else if (aMethod == 2) //Huber
    // {
    // if (scaledResidual >= 1.345) {
    // aWeight = 1.345 / scaledResidual;
    // }
    // } else if (aMethod == 3) //Cauchy
    // {
    // aWeight = 1.0 / (1.0 + (scaledResidual * scaledResidual / 5.6877));
    // }
    // theDownWeight = aWeight;
    // return aWeight;
    // }
    //
    // / Calculate Chi2 contribution.
    /**
     * \return (down-weighted) Chi2
     */
    double getChi2() {
        double aDiff = theValue - thePrediction;
        return aDiff * aDiff * thePrecision * theDownWeight;
    }

    // / Print data block.
    void printData() {
        System.out.println(" measurement at label " + theLabel + ": " + theValue + ", " + thePrecision);
        System.out.println(" measurement type " + theType); 
        System.out.print("  param " + theParameters.size() + ":");
        for (int i = 0; i < theParameters.size(); ++i) {
            System.out.print(" " + theParameters.get(i));
        }
        System.out.println("\n");
        System.out.print("  deriv " + theDerivatives.size() + ":");
        for (int i = 0; i < theDerivatives.size(); ++i) {
            System.out.print(" " + theDerivatives.get(i));
        }
        System.out.println("\n");
        for (int i = 0; i < globalLabels.size(); ++i) {
            System.out.print(" " + globalLabels.get(i));
        }
        System.out.println("\n");
        for (int i = 0; i < globalDerivatives.size(); ++i) {
            System.out.print(" " + globalDerivatives.get(i));
        }
        System.out.println("\n");
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(" measurement at label " + theLabel + ": " + theValue + ", " + thePrecision
                + "\n");
        sb.append("  param " + theParameters.size() + ":");
        for (int i = 0; i < theParameters.size(); ++i) {
            sb.append(" " + theParameters.get(i));
        }
        sb.append("\n");
        for (int i = 0; i < theDerivatives.size(); ++i) {
            sb.append(" " + theDerivatives.get(i));
        }
        sb.append("\n");
        return sb.toString();
    }

    // / Get Data for local fit.
    /**
     * \param [out] aValue Value \param [out] aWeight Weight \param [out] indLocal List of labels of used (local) fit
     * parameters \param [out] derLocal List of derivatives for used (local) fit parameters
     */
    void getLocalData(double[] retVal, List<Integer> indLocal, List<Double> derLocal) {

        retVal[0] = theValue;
        retVal[1] = thePrecision * theDownWeight;
        indLocal = theParameters;
        derLocal = theDerivatives;
    }

    void getLocalData(double[] retVal, int[] indLocal, double[] derLocal) {
        retVal[0] = theValue;
        retVal[1] = thePrecision * theDownWeight;
        for (int i = 0; i < theParameters.size(); ++i) {
            indLocal[i] = theParameters.get(i);
            derLocal[i] = theDerivatives.get(i);
        }
    }

    int getNumParameters() {
        return theParameters.size();
    }

    // / Get all Data for MP-II binary record.
    /**
     * \param [out] fValue Value \param [out] fErr Error \param [out] indLocal List of labels of local parameters \param
     * [out] derLocal List of derivatives for local parameters \param [out] labGlobal List of labels of global
     * parameters \param [out] derGlobal List of derivatives for global parameters
     */
    void getAllData(float[] floats, List<Integer> indLocal, List<Double> derLocal, List<Integer> labGlobal,
            List<Double> derGlobal) {
        floats[0] = (float) theValue;
        floats[1] = (float) (1.0 / sqrt(thePrecision));
        indLocal.addAll(theParameters);
        derLocal.addAll(theDerivatives);
        labGlobal.addAll(globalLabels);
        derGlobal.addAll(globalDerivatives);
    }

    // / Get data for residual (and errors).
    /**
     * \param [out] aResidual Measurement-Prediction \param [out] aVariance Variance (of measurement) \param [out]
     * aDownWeight Down-weighting factor \param [out] indLocal List of labels of used (local) fit parameters \param
     * [out] derLocal List of derivatives for used (local) fit parameters
     */
    void getResidual(double[] doubles, List<Integer> indLocal, List<Double> derLocal) {
        doubles[0] = theValue - thePrediction; // aResidual
        doubles[1] = 1.0 / thePrecision; // aVariance
        doubles[2] = theDownWeight; // aDownWeight
        indLocal.addAll(theParameters);
        derLocal.addAll(theDerivatives);
    }
}
