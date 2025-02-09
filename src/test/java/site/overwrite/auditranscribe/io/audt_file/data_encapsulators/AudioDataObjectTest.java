/*
 * AudioDataObjectTest.java
 *
 * Created on 2022-07-02
 * Updated on 2022-07-02
 *
 * Description: Test `AudioDataObject.java`.
 */

package site.overwrite.auditranscribe.io.audt_file.data_encapsulators;

import org.junit.jupiter.api.Test;
import site.overwrite.auditranscribe.utils.MathUtils;

import static org.junit.jupiter.api.Assertions.*;

class AudioDataObjectTest {
    // Attributes
    byte[] compressedMP3Bytes1 = new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a};
    byte[] compressedMP3Bytes2 = new byte[]{(byte) 0xab, (byte) 0xcd, (byte) 0xef};

    double sampleRate1 = 44100;
    double sampleRate2 = 22050;

    int totalDurationInMS1 = 1234;
    int totalDurationInMS2 = 5678;

    String audioFileName1 = "Test File 1.mp3";
    String audioFileName2 = "Test File 2.mp3";

    // Tests
    @Test
    void numBytesNeeded() {
        // Define the two audio data objects to test the number of bytes needed
        AudioDataObject one = new AudioDataObject(
                compressedMP3Bytes1, sampleRate1, totalDurationInMS1, audioFileName1
        );
        AudioDataObject two = new AudioDataObject(
                compressedMP3Bytes2, sampleRate2, totalDurationInMS2, audioFileName2
        );

        // Tests
        assertEquals(48, one.numBytesNeeded());
        assertEquals(46, two.numBytesNeeded());
    }

    @Test
    void testEquals() {
        // Define temporary data object for testing the initial checks
        AudioDataObject temp = new AudioDataObject(
                compressedMP3Bytes1, sampleRate1, totalDurationInMS1, audioFileName1
        );

        // Define other objects to test comparison
        String otherTypedVar = "hello";

        // Test equality comparisons
        assertEquals(temp, temp);
        assertNotEquals(temp, null);
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(temp, otherTypedVar);  // Not redundant to test the equality method

        // Define arrays to pick the data attributes from
        byte[][] compressedMP3BytesArrays = {compressedMP3Bytes1, compressedMP3Bytes2};
        double[] sampleRates = {sampleRate1, sampleRate2};
        int[] totalDurationInMSs = {totalDurationInMS1, totalDurationInMS2};
        String[] audioFileNames = {audioFileName1, audioFileName2};

        // Generate product of indices
        int[][] indexProduct = MathUtils.selfProduct(2, 4);  // 4 data attributes
        for (int[] indices1 : indexProduct) {
            AudioDataObject one = new AudioDataObject(
                    compressedMP3BytesArrays[indices1[0]],
                    sampleRates[indices1[1]],
                    totalDurationInMSs[indices1[2]],
                    audioFileNames[indices1[3]]
            );

            for (int[] indices2 : indexProduct) {
                AudioDataObject two = new AudioDataObject(
                        compressedMP3BytesArrays[indices2[0]],
                        sampleRates[indices2[1]],
                        totalDurationInMSs[indices2[2]],
                        audioFileNames[indices2[3]]
                );

                // Check equality
                if (indices1 == indices2) {
                    assertEquals(one, two);
                    assertEquals(two, one);
                } else {
                    assertNotEquals(one, two);
                    assertNotEquals(two, one);
                }
            }
        }
    }

    @Test
    void testHashCode() {
        // Define the two audio data objects to test the hash code method
        AudioDataObject one = new AudioDataObject(
                compressedMP3Bytes1, sampleRate1, totalDurationInMS1, audioFileName1
        );
        AudioDataObject two = new AudioDataObject(
                compressedMP3Bytes2, sampleRate2, totalDurationInMS2, audioFileName2
        );

        // Tests
        assertEquals(1981043339, one.hashCode());
        assertEquals(793645213, two.hashCode());
    }
}