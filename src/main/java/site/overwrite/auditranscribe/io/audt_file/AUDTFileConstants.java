/*
 * AUDTFileConstants.java
 *
 * Created on 2022-05-01
 * Updated on 2022-06-28
 *
 * Description: Constants that are needed when processing the AudiTranscribe file format.
 */

package site.overwrite.auditranscribe.io.audt_file;

import site.overwrite.auditranscribe.io.LZ4;

/**
 * Constants that are needed when processing the AudiTranscribe file format.
 */
public final class AUDTFileConstants {
    // Constants
    public static final byte[] AUDT_SECTION_DELIMITER = new byte[]{
            (byte) 0xe0, (byte) 0x5e, (byte) 0x05, (byte) 0xe5
    };
    public static final byte[] AUDT_END_OF_FILE_DELIMITER = new byte[]{
            (byte) 0xe0, (byte) 0xfe, (byte) 0x0f, (byte) 0xef,
            (byte) 0xe0, (byte) 0xfe, (byte) 0x0f, (byte) 0xef
    };

    public static final byte[] AUDT_FILE_HEADER = new byte[]{
            (byte) 0x41, (byte) 0x55, (byte) 0x44, (byte) 0x49,
            (byte) 0x54, (byte) 0x52, (byte) 0x41, (byte) 0x4e,
            (byte) 0x53, (byte) 0x43, (byte) 0x52, (byte) 0x49,
            (byte) 0x42, (byte) 0x45, (byte) 0x0a, (byte) 0x0a,
            (byte) 0xad, (byte) 0x75, (byte) 0xc1, (byte) 0xbe
    };
    public static final int FILE_VERSION_NUMBER = 401;  // Application version 0.4.0, revision 1 -> 0401 -> 401
    public static final int LZ4_VERSION_NUMBER = LZ4.LZ4_VERSION_NUMBER;  // Take from the LZ4 class

    private AUDTFileConstants() {
        // Private constructor to signal this is a utility class
    }
}
