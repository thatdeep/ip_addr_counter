import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class UniqueIPCounterBitArray {

    public static void main(String[] args) {
        String filePath = "ip_addresses_short.txt";

        // Create a byte array to hold 2^32 bits (537 MB)
        byte[] bitArray = new byte[(int) Math.pow(2, 29)];

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String ipAddress;
            while ((ipAddress = br.readLine()) != null) {
                long ipAsInt = ipToInt(ipAddress);
                setBit(bitArray, ipAsInt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long uniqueCount = countSetBits(bitArray);
        System.out.println("Number of unique IP addresses: " + uniqueCount);
    }

    private static long ipToInt(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result |= ((long)Integer.parseInt(octets[i]) << (8 * (3 - i)));
        }
        return result;
    }

    private static void setBit(byte[] bitArray, long bitIndex) {
        long arrayIndex = bitIndex / 8;
        int bitPosition = (int)(bitIndex % 8);
        bitArray[(int)arrayIndex] |= (1 << bitPosition);
    }

    private static long countSetBits(byte[] bitArray) {
        long count = 0;
        for (byte b : bitArray) {
            count += Integer.bitCount(b & 0xFF);
        }
        return count;
    }
}