package org.jlab.coda.jevio;


/**
 * This an enum used to convert data type numerical values to a more meaningful name. For example, the data type with
 * value 0xe corresponds to the enum BANK. Mostly this is used for printing. In this version of evio, the
 * ALSOTAGSEGMENT (0x40) value was removed from this enum because the upper 2 bits of a byte containing
 * the datatype are now used to store padding data.
 * 
 * @author heddle
 * @author timmer
 *
 */
public enum DataType {

	UNKNOWN32      (0x0), 
	UINT32         (0x1), 
	FLOAT32        (0x2), 
	CHARSTAR8      (0x3), 
	SHORT16        (0x4), 
	USHORT16       (0x5), 
	CHAR8          (0x6), 
	UCHAR8         (0x7), 
	DOUBLE64       (0x8), 
	LONG64         (0x9), 
	ULONG64        (0xa), 
	INT32          (0xb), 
	TAGSEGMENT     (0xc), 
	SEGMENT        (0xd), 
    BANK           (0xe),
    COMPOSITE      (0xf),
	ALSOBANK       (0x10),
	ALSOSEGMENT    (0x20),

    //  Remove ALSOTAGSEGMENT (0x40) since it was never
    //  used and we now need that to store padding data.
    //	ALSOTAGSEGMENT (0x40),

    // These 2 types are only used when dealing with COMPOSITE data.
    // They are never transported independently and are stored in integers.
    HOLLERIT       (0x41),
    NVALUE         (0x42);

	private int value;

	private DataType(int value) {
		this.value = value;
	}

	/**
	 * Get the enum's value.
	 * 
	 * @return the value, e.g., 0xe for a BANK
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Obtain the name from the value.
	 * 
	 * @param value the value to match.
	 * @return the name, or "UNKNOWN".
	 */
	public static String getName(int value) {
		DataType datatypes[] = DataType.values();
		for (DataType dt : datatypes) {
			if (dt.value == value) {
				return dt.name();
			}
		}

//        // for historical, backwards-compatibility reasons
//        if (value == 0x40) {
//            return "TAGSEGMENT";
//        }

		return "UNKNOWN";
	}

	/**
	 * Obtain the enum from the value.
     * 
	 * 
	 * @param value the value to match.
	 * @return the matching enum, or <code>null</code>.
	 */
	public static DataType getDataType(int value) {
		DataType datatypes[] = DataType.values();
		for (DataType dt : datatypes) {
			if (dt.value == value) {
				return dt;
			}
		}

//        // for historical, backwards-compatibility reasons
//        if (value == 0x40) {
//            return TAGSEGMENT;
//        }

		return null;
	}
	
	/**
	 * Convenience routine to see if "this" data type is a structure (a container.)
	 * @return <code>true</code> if the data type corresponds to one of the structure
	 * types: BANK, SEGMENT, or TAGSEGMENT.
	 */
	public boolean isStructure() {
		switch (this) {
		case BANK:
		case SEGMENT:
		case TAGSEGMENT:
			return true;
		case ALSOBANK:
		case ALSOSEGMENT:
//		case ALSOTAGSEGMENT:
			return true;
		default:
			return false;
		}
	}
}
