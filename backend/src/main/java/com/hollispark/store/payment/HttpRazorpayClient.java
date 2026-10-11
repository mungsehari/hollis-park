package com.hollispark.store.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.service.ApiException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Razorpay Orders API over HTTPS with the key id and secret as Basic auth. */
@Component
public class HttpRazorpayClient implements RazorpayClient {
    private static final Logger log = LoggerFactory.getLogger(HttpRazorpayClient.class);
    private static final URI ORDERS = URI.create("https://api.razorpay.com/v1/orders");

    private final StoreProperties.Razorpay cfg;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public HttpRazorpayClient(StoreProperties props, ObjectMapper mapper) {
        this.cfg = props.razorpay();
        this.mapper = mapper;
    }

    @Override
    public RazorpayOrder createOrder(long amountPaise, String currency, String receipt) {
        try {
            String body = mapper.writeValueAsString(Map.of("amount", amountPaise, "currency", currency, "receipt", receipt));
            String auth = Base64.getEncoder().encodeToString((cfg.keyId() + ":" + cfg.keySecret()).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder(ORDERS)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.error("Razorpay order create failed: HTTP {} {}", res.statusCode(), res.body());
                throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment service is not available. Please try again in a moment.");
            }
            JsonNode n = mapper.readTree(res.body());
            return new RazorpayOrder(n.get("id").asText(), n.get("amount").asLong(), n.get("currency").asText());
        } catch (IOException e) {
            log.error("Razorpay order create failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment service is not available. Please try again in a moment.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment service is not available. Please try again in a moment.");
        }
    }
}
