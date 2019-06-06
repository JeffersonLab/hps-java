package org.hps.recon.tracking.lit;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitTrack {

    List<CbmLitHit> fHits; // Array of hits
    List<CbmLitFitNode> fFitNodes; // Array of fit nodes
    CbmLitTrackParam fParamFirst; // First track parameter
    CbmLitTrackParam fParamLast; // Last track parameter
    LitTrackQa fQuality; // Track quality
    double fChi2; // Chi-square
    int fNDF; // Number of degrees of freedom
    int fPreviousTrackId; // Index of the previous track, i.e. STS
    int fLastPlaneId; // Last detector plane where track has a hit
    int fPDG; // PDG code
    int fNofMissingHits; // Number of missing hits
    int fRefId; // Reference to MC
    double fLength; // Track length    

    public CbmLitTrack() {
        fHits = new ArrayList<CbmLitHit>();
        fFitNodes = new ArrayList<CbmLitFitNode>();
        fQuality = LitTrackQa.kLITGOOD;
        fParamFirst = new CbmLitTrackParam();
        fParamLast = new CbmLitTrackParam();
        fPDG = 11;

        fLength = -1.;
    }


    /*
     * Getters
     */
    public int GetNofHits() {
        return fHits.size();
    }

    public LitTrackQa GetQuality() {
        return fQuality;
    }

    public double GetChi2() {
        return fChi2;
    }

    public int GetNDF() {
        return fNDF;
    }

    public int GetPreviousTrackId() {
        return fPreviousTrackId;
    }

    public int GetPDG() {
        return fPDG;
    }

    public CbmLitTrackParam GetParamFirst() {
        return fParamFirst;
    }

    public CbmLitTrackParam GetParamLast() {
        return fParamLast;
    }

    public int GetLastPlaneId() {
        return fLastPlaneId;
    }

    public CbmLitHit GetHit(int index) {
        return fHits.get(index);
    }

    public List< CbmLitHit> GetHits() {
        return fHits;
    }

    public CbmLitFitNode GetFitNode(int index) {
        return fFitNodes.get(index);
    }

    public CbmLitFitNode[] GetFitNodes() {
        return fFitNodes.toArray(new CbmLitFitNode[fFitNodes.size()]);
    }

    public int GetNofMissingHits() {
        return fNofMissingHits;
    }

    public int GetRefId() {
        return fRefId;
    }

    public double GetLength() {
        return fLength;
    }

    /*
     * Setters
     */
    public void SetQuality(LitTrackQa quality) {
        fQuality = quality;
    }

    public void SetChi2(double chi2) {
        fChi2 = chi2;
    }

    public void SetNDF(int ndf) {
        fNDF = ndf;
    }

    public void SetPreviousTrackId(int id) {
        fPreviousTrackId = id;
    }

    public void SetPDG(int pdg) {
        fPDG = pdg;
    }

    public void SetParamFirst(CbmLitTrackParam par) {
        fParamFirst.copyFrom(par);
    }

    public void SetParamLast(CbmLitTrackParam par) {
        fParamLast.copyFrom(par);
        //fParamLast = par;
    }

    /*
     * TODO temporarily needed for equal_range algorithm
     */

    public void SetNofHits(int nofHits) {
    }

    public void SetLastPlaneId(int lastPlaneId) {
        fLastPlaneId = lastPlaneId;
    }

    public void SetFitNodes(CbmLitFitNode[] nodes) {
        fFitNodes = Arrays.asList(nodes);
    }

    public void SetNofMissingHits(int nofMissingHits) {
        fNofMissingHits = nofMissingHits;
    }

    public void SetRefId(int refId) {
        fRefId = refId;
    }

    public void SetLength(double length) {
        fLength = length;
    }

    /**
     * \brief Add hit to track. No additional memory is allocated for hit.
     */
    public void AddHit(CbmLitHit hit) {
        fHits.add(hit);
    }

    public void AddHits(List<CbmLitHit> hits) {
        fHits.addAll(hits);
    }

    /**
     * \brief Remove all hits from track. Do not delete memory.
     */
    public void ClearHits() {
        fHits.clear();
    }

    /**
     * \brief Remove hit and corresponding fit node.
     */
    public void RemoveHit(int index) {
        fHits.remove(index);
    }

    /**
     * \brief Sort hits by Z position. \param[in] downstream If downstream is
     * true than hits are sorted in downstream direction otherwise in upstream
     * direction.
     */
    public void SortHits(boolean downstream) {
        Collections.sort(fHits, new HitZSort(downstream));
    }

    /**
     * \brief Return true if track parameters are correct. \return True if track
     * parameters are correct.
     */
    public boolean CheckParams() {
        double[] covFirst = fParamFirst.GetCovMatrix();
        double[] covLast = fParamLast.GetCovMatrix();
        for (int i = 0; i < 15; i++) {
            if (abs(covFirst[i]) > 10000.
                    || abs(covLast[i]) > 10000.) {
                return false;
            }
        }
        if (GetNofHits() < 1) {
            return false;
        }
        return true;
    }

    /**
     * \brief Return string representation of class. \return String
     * representation of class.
     */
    public String toString() {
        StringBuffer ss = new StringBuffer();
        ss.append("Track: quality=" + fQuality + ", chi2=" + fChi2
                + ", ndf=" + fNDF + ", previousTrackId=" + fPreviousTrackId
                + ", lastPlaneId=" + fLastPlaneId + ", pdg=" + fPDG
                + ", nofHits=" + fHits.size() + ", nofFitNodes=" + fFitNodes.size() + "\n");
        return ss.toString();
    }
}
