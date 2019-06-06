package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitHit {

    /* Getters */
    int GetRefId() {
        return fRefId;
    }

    LitHitType GetType() {
        return fHitType;
    }

    public double GetZ() {
        return fZ;
    }

    public double GetDz() {
        return fDz;
    }
//   LitSystemId GetSystem()   {
//       return LitSystemId((fDetectorId & (WL_SYSTEM << SB_SYSTEM)) >> SB_SYSTEM);
//   }

    int GetStationGroup() {
        return (fDetectorId & (WL_STATION_GROUP << SB_STATION_GROUP)) >> SB_STATION_GROUP;
    }

    int GetStation() {
        return (fDetectorId & (WL_STATION << SB_STATION)) >> SB_STATION;
    }

    int GetSubstation() {
        return (fDetectorId & (WL_SUBSTATION << SB_SUBSTATION)) >> SB_SUBSTATION;
    }

    int GetModule() {
        return (fDetectorId & (WL_MODULE << SB_MODULE)) >> SB_MODULE;
    }

    /* Setters */
    void SetRefId(int refId) {
        fRefId = refId;
    }

    void SetHitType(LitHitType hitType) {
        fHitType = hitType;
    }

    void SetZ(double z) {
        fZ = z;
    }

    void SetDz(double dz) {
        fDz = dz;
    }
//   void SetDetectorId(LitSystemId sysId, int stationGroup, int station, int substation, int module) {
//       fDetectorId = (sysId << SB_SYSTEM) | (stationGroup << SB_STATION_GROUP)
//               | (station << SB_STATION) | (substation << SB_SUBSTATION) | (module << SB_MODULE);
//   }

    /**
     * \brief Return string representation of class. \return String
     * representation of class.
     */
    public String toString() {
        return "";
    }

    int fRefId; // reference to MC
    LitHitType fHitType; // type of the hit (strip, pixel, etc). Used to safely cast to the proper type.
    double fZ; // Z position of the hit [cm]
    double fDz; // Z position error of the hit [cm]

    // The detector ID consists of:
    // system ID            (0-15),    bits 0-3
    // station group number (0-15),    bits 4-7
    // station number       (0-15),    bits 8-11
    // substation number    (0-7),     bits 12-15
    // module number        (0-65535), bits 16-31
    int fDetectorId; // Unique detector identificator

    // Length of the index of the corresponding volume
//   static  final int WL_SYSTEM = 15;
    static final int WL_STATION_GROUP = 15;
    static final int WL_STATION = 15;
    static final int WL_SUBSTATION = 7;
    static final int WL_MODULE = 65535;
    // Start bit for each volume
    static final int SB_SYSTEM = 0;
    static final int SB_STATION_GROUP = 4;
    static final int SB_STATION = 8;
    static final int SB_SUBSTATION = 12;
    static final int SB_MODULE = 16;
}
