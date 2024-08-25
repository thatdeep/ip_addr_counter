import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class UniqueIPCounter {

    public static void main(String[] args) {
        String filePath = "ip_addresses.txt";
        HashSet<String> uniqueIPs = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String ipAddress;
            while ((ipAddress = br.readLine()) != null) {
                uniqueIPs.add(ipAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Number of unique IP addresses: " + uniqueIPs.size());
    }
}