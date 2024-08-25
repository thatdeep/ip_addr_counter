import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class UniqueIPCounterInt {

    public static void main(String[] args) {
        String filePath = "ip_addresses.txt";
        HashSet<Integer> uniqueIPs = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String ipAddress;
            while ((ipAddress = br.readLine()) != null) {
                int ipAsInt = ipToInt(ipAddress);
                uniqueIPs.add(ipAsInt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Number of unique IP addresses: " + uniqueIPs.size());
    }

    private static int ipToInt(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Integer.parseInt(octets[i]) << (8 * (3 - i)));
        }
        return result;
    }
}