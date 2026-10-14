package com.hollispark.store;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"store.media.local-dir=target/test-media", "store.razorpay.key-id=", "store.razorpay.key-secret="})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES, provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class StoreApiTest {
    @Autowired
    MockMvc mvc;

    private static final String ADDRESS = """
            "email":"asha@example.com","first":"Asha","last":"Rao","address":"12 MG Road","city":"Pune","pin":"411001"
            """;

    private org.springframework.test.web.servlet.ResultActions order(String json) throws Exception {
        return mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void storeBootstrapHasCatalogAndConfig() throws Exception {
        mvc.perform(get("/api/v1/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.site.name").value("Hollis Park"))
                .andExpect(jsonPath("$.data.config.currency").value("INR"))
                .andExpect(jsonPath("$.data.products.length()").value(8))
                .andExpect(jsonPath("$.data.products[0].id").value("tide-one"))
                .andExpect(jsonPath("$.data.products[0].wide.extra").value(799))
                .andExpect(jsonPath("$.data.reviews.length()").value(16))
                .andExpect(jsonPath("$.data.collections.length()").value(4))
                .andExpect(jsonPath("$.data.config.promoCodes").doesNotExist());
    }

    @Test
    void unknownProductIs404() throws Exception {
        mvc.perform(get("/api/v1/products/nope")).andExpect(status().isNotFound());
    }

    @Test
    void placesOrderWithServerSidePrices() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"standard\",\"promo\":\"hollis10\",\"items\":["
                + "{\"id\":\"tide-one\",\"size\":9,\"width\":\"wide\",\"qty\":1}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNumber").value(org.hamcrest.Matchers.startsWith("HP-")))
                .andExpect(jsonPath("$.data.subtotal").value(11798.0))
                .andExpect(jsonPath("$.data.discount").value(1179.8))
                .andExpect(jsonPath("$.data.shipping").value(149.0))
                .andExpect(jsonPath("$.data.total").value(10767.2))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void rejectsSoldOutSize() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"pickup\",\"items\":[{\"id\":\"tide-one\",\"size\":13,\"width\":\"standard\",\"qty\":1}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tide One in US 13 is sold out."));
    }

    @Test
    void rejectsWideOnStandardOnlyModel() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"pickup\",\"items\":[{\"id\":\"rally-mid\",\"size\":9,\"width\":\"wide\",\"qty\":1}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBadPromoAndBadPin() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"pickup\",\"promo\":\"FREE\",\"items\":[{\"id\":\"tide-one\",\"size\":9,\"width\":\"standard\",\"qty\":1}]}")
                .andExpect(status().isBadRequest());
        order("{" + ADDRESS.replace("411001", "4110") + ",\"ship\":\"pickup\",\"items\":[{\"id\":\"tide-one\",\"size\":9,\"width\":\"standard\",\"qty\":1}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.pin").exists());
    }

    @Test
    void promoValidation() throws Exception {
        mvc.perform(post("/api/v1/promo/validate").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"hollis10\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("HOLLIS10")).andExpect(jsonPath("$.data.rate").value(0.1));
        mvc.perform(post("/api/v1/promo/validate").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"NOPE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contactAndNewsletter() throws Exception {
        mvc.perform(post("/api/v1/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"topic\":\"sizing\",\"message\":\"Which size for Tide One?\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"topic\":\"order\",\"order\":\"X1\",\"message\":\"Where is my order?\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/newsletter").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Asha@Example.com\",\"topics\":[\"running\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.promoCode").value("HOLLIS10"));
        mvc.perform(post("/api/v1/newsletter").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"asha@example.com\"}"))
                .andExpect(status().isCreated());
    }
}
