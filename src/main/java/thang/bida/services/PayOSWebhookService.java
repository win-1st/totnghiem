package thang.bida.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PayOSWebhookService {

    @Value("${payos.checksumKey}")
    private String checksumKey;

    public boolean verifySignature(Map<String, Object> data, String signature) {
        try {
            // Sắp xếp data theo alphabet (giống PayOS)
            Map<String, Object> sortedData = new TreeMap<>(data);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
                if (entry.getValue() != null) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            String dataStr = sb.toString();

            System.out.println(">>> Data to verify: " + dataStr);

            // Tính HMAC SHA256 -> HEX (không phải Base64)
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret = new SecretKeySpec(checksumKey.getBytes("UTF-8"), "HmacSHA256");
            hmac.init(secret);
            byte[] hash = hmac.doFinal(dataStr.getBytes("UTF-8"));

            String calculatedSignature = bytesToHex(hash);
            System.out.println(">>> Calculated: " + calculatedSignature);
            System.out.println(">>> Received:   " + signature);

            return calculatedSignature.equals(signature);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}