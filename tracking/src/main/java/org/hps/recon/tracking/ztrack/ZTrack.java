package org.hps.recon.tracking.ztrack;

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
public class ZTrack {

    List<Hit> fHits; // Array of hits
    List<FitNode> fFitNodes; // Array of fit nodes
    ZTrackParam fParamFirst; // First track parameter
    ZTrackParam fParamLast; // Last track parameter
    ZTrackQa fQuality; // Track quality
    double fChi2; // Chi-square
    int fNDF; // Number of degrees of freedom
    int fPreviousTrackId; // Index of the previous track, i.e. STS
    int fLastPlaneId; // Last detector plane where track has a hit
    int fPDG; // PDG code
    int fNofMissingHits; // Number of missing hits
    int fRefId; // Reference to MC
    double fLength; // Track length    

    public ZTrack() {
        fHits = new ArrayList<Hit>();
        fFitNodes = new ArrayList<FitNode>();
        fQuality = ZTrackQa.GOOD;
        fParamFirst = new ZTrackParam();
        fParamLast = new ZTrackParam();
        fPDG = 11;

        fLength = -1.;
    }


    /*
     * Getters
     */
    public int GetNofHits() {
        return fHits.size();
    }

    public ZTrackQa GetQuality() {
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

    public ZTrackParam GetParamFirst() {
        return fParamFirst;
    }

    public ZTrackParam GetParamLast() {
        return fParamLast;
    }

    public int GetLastPlaneId() {
        return fLastPlaneId;
    }

    public Hit GetHit(int index) {
        return fHits.get(index);
    }

    public List< Hit> GetHits() {
        return fHits;
    }

    public FitNode GetFitNode(int index) {
        return fFitNodes.get(index);
    }

    public FitNode[] GetFitNodes() {
        return fFitNodes.toArray(new FitNode[fFitNodes.size()]);
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
    public void SetQuality(ZTrackQa quality) {
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

    public void SetParamFirst(ZTrackParam par) {
        fParamFirst.copyFrom(par);
    }

    public void SetParamLast(ZTrackParam par) {
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

    public void SetFitNodes(FitNode[] nodes) {
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
    public void AddHit(Hit hit) {
        fHits.add(hit);
    }

    public void AddHits(List<Hit> hits) {
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
        ss.append("ZTrack: quality=" + fQuality + ", chi2=" + fChi2
                + ", ndf=" + fNDF + ", previousTrackId=" + fPreviousTrackId
                + ", lastPlaneId=" + fLastPlaneId + ", pdg=" + fPDG
                + ", nofHits=" + fHits.size() + ", nofFitNodes=" + fFitNodes.size() + "\n");
        return ss.toString();
    }
}
