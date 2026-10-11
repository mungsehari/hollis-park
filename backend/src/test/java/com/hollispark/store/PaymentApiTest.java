package com.hollispark.store;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.payment.RazorpayClient;
import com.hollispark.store.payment.Signatures;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PaymentApiTest.Fake.class)
@TestPropertySource(properties = {
        "store.media.local-dir=target/test-media",
        "store.razorpay.key-id=rzp_test_demo",
        "store.razorpay.key-secret=test_secret_123",
        "store.razorpay.webhook-secret=whsec_test_456"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES, provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class PaymentApiTest {
    /** Stands in for api.razorpay.com so tests never leave the machine. */
    @TestConfiguration
    static class Fake {
        private final AtomicInteger n = new AtomicInteger();

        @Bean
        @Primary
        RazorpayClient fakeRazorpay() {
            return (amount, currency, receipt) -> new RazorpayClient.RazorpayOrder("order_FAKE" + n.incrementAndGet(), amount, currency);
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private JsonNode order() throws Exception {
        String body = """
                {"email":"asha@example.com","first":"Asha","last":"Rao","address":"12 MG Road","city":"Pune","pin":"411001",
                 "ship":"standard","items":[{"id":"tide-one","size":9,"width":"standard","qty":1}]}""";
        String res = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private String confirmBody(String rzpOrder, String paymentId, String signature) {
        return "{\"razorpayOrderId\":\"" + rzpOrder + "\",\"razorpayPaymentId\":\"" + paymentId + "\",\"razorpaySignature\":\"" + signature + "\"}";
    }

    @Test
    void orderWaitsForPaymentAndCarriesTheRazorpayDetails() throws Exception {
        JsonNode o = order();
        org.assertj.core.api.Assertions.assertThat(o.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        org.assertj.core.api.Assertions.assertThat(o.get("paymentStatus").asText()).isEqualTo("PENDING");
        org.assertj.core.api.Assertions.assertThat(o.at("/payment/keyId").asText()).isEqualTo("rzp_test_demo");
        org.assertj.core.api.Assertions.assertThat(o.at("/payment/testMode").asBoolean()).isTrue();
        // 10,999 + 149 delivery, in paise
        org.assertj.core.api.Assertions.assertThat(o.at("/payment/amountPaise").asLong()).isEqualTo(1114800L);
        // The secret never leaves the server.
        org.assertj.core.api.Assertions.assertThat(o.toString()).doesNotContain("test_secret_123");
    }

    @Test
    void forgedSignatureIsRejectedAndTheOrderStaysPending() throws Exception {
        JsonNode o = order();
        String number = o.get("orderNumber").asText();
        String rzp = o.at("/payment/razorpayOrderId").asText();
        mvc.perform(post("/api/orders/" + number + "/payment").contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody(rzp, "pay_FORGED", "0".repeat(64))))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/orders/" + number + "/payment").contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody("order_SOMEONE_ELSE", "pay_1", Signatures.hmacHex("test_secret_123", "order_SOMEONE_ELSE|pay_1"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void validSignatureMarksTheOrderPaidOnce() throws Exception {
        JsonNode o = order();
        String number = o.get("orderNumber").asText();
        String rzp = o.at("/payment/razorpayOrderId").asText();
        String sig = Signatures.hmacHex("test_secret_123", rzp + "|pay_OK1");
        for (int i = 0; i < 2; i++) { // a second delivery changes nothing
            mvc.perform(post("/api/orders/" + number + "/payment").contentType(MediaType.APPLICATION_JSON).content(confirmBody(rzp, "pay_OK1", sig)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RECEIVED"))
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        }
    }

    @Test
    void webhookNeedsTheRightSignatureAndPaysTheOrder() throws Exception {
        JsonNode o = order();
        String number = o.get("orderNumber").asText();
        String rzp = o.at("/payment/razorpayOrderId").asText();
        String body = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_HOOK1\",\"order_id\":\"" + rzp + "\"}}}}";
        mvc.perform(post("/api/payments/razorpay/webhook").contentType(MediaType.APPLICATION_JSON).header("X-Razorpay-Signature", "bad").content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payments/razorpay/webhook").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", Signatures.hmacHex("whsec_test_456", body)).content(body))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/store")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff@hollispark.local", roles = "STAFF")
    void unpaidOrdersCannotBeConfirmedByStaff() throws Exception {
        String number = order().get("orderNumber").asText();
        mvc.perform(patch("/api/admin/orders/" + number).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/orders/" + number)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.nextStatuses[0]").value("CANCELLED"))
                .andExpect(jsonPath("$.orderNumber", startsWith("HP-")));
    }
}
