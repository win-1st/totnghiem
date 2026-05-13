package thang.bida.payos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

@Component
public class PayOSClient {

    private final String clientId;
    private final String apiKey;
    private final String checksumKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    public PayOSClient(
            @Value("${payos.clientId}") String clientId,
            @Value("${payos.apiKey}") String apiKey,
            @Value("${payos.checksumKey}") String checksumKey) {
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.checksumKey = checksumKey;
    }

    public Map<String, Object> createPaymentLink(Map<String, Object> payload) {
        try {
            // Tạo signature
            String signature = createSignature(payload);
            payload.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            System.out.println(">>> PAYLOAD WITH SIGNATURE: " + new ObjectMapper().writeValueAsString(payload));

            ResponseEntity<Map> res = restTemplate.postForEntity(BASE_URL, entity, Map.class);
            return res.getBody();

        } catch (HttpStatusCodeException ex) {
            System.err.println(">>> PayOS Error: " + ex.getResponseBodyAsString());
            throw new RuntimeException(ex.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error creating payment", e);
        }
    }

    // THÊM METHOD NÀY
    public Map<String, Object> getPaymentStatus(String orderCode) {
        try {
            String url = BASE_URL + "/" + orderCode;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            System.out.println(">>> Checking payment status for order: " + orderCode);

            ResponseEntity<Map> res = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            return res.getBody();

        } catch (HttpStatusCodeException ex) {
            System.err.println(">>> PayOS Check Status Error: " + ex.getResponseBodyAsString());
            throw new RuntimeException(ex.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error checking payment status", e);
        }
    }

    private String createSignature(Map<String, Object> payload) throws Exception {
        // Lấy các giá trị cần thiết
        int amount = (int) payload.get("amount");
        String cancelUrl = (String) payload.get("cancelUrl");
        String description = (String) payload.get("description");
        int orderCode = (int) payload.get("orderCode");
        String returnUrl = (String) payload.get("returnUrl");

        // Tạo chuỗi data theo đúng format PayOS yêu cầu
        // Format:
        // amount={amount}&cancelUrl={cancelUrl}&description={description}&orderCode={orderCode}&returnUrl={returnUrl}
        String data = "amount=" + amount +
                "&cancelUrl=" + cancelUrl +
                "&description=" + description +
                "&orderCode=" + orderCode +
                "&returnUrl=" + returnUrl;

        System.out.println(">>> Data to sign: " + data);
        System.out.println(">>> Checksum key length: " + checksumKey.length());

        // Tính HMAC SHA256
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret = new SecretKeySpec(checksumKey.getBytes("UTF-8"), "HmacSHA256");
        hmac.init(secret);
        byte[] hash = hmac.doFinal(data.getBytes("UTF-8"));

        // Convert sang hex string
        String signature = bytesToHex(hash);
        System.out.println(">>> Generated signature: " + signature);

        return signature;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}