package thang.bida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

@Service
public class PayOSWebhookService {

    @Value("${payos.checksumKey}")
    private String checksumKey;

    private static final ObjectMapper mapper = new ObjectMapper();

    public boolean verifySignature(Map<String, Object> data, String signature) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(checksumKey.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKey);

            String json = mapper.writeValueAsString(data);
            String hash = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(json.getBytes()));

            return hash.equals(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
