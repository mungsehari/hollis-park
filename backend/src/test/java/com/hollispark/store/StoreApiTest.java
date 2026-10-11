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
        return mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void storeBootstrapHasCatalogAndConfig() throws Exception {
        mvc.perform(get("/api/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.site.name").value("Hollis Park"))
                .andExpect(jsonPath("$.config.currency").value("INR"))
                .andExpect(jsonPath("$.products.length()").value(8))
                .andExpect(jsonPath("$.products[0].id").value("tide-one"))
                .andExpect(jsonPath("$.products[0].wide.extra").value(799))
                .andExpect(jsonPath("$.reviews.length()").value(16))
                .andExpect(jsonPath("$.collections.length()").value(4))
                .andExpect(jsonPath("$.config.promoCodes").doesNotExist());
    }

    @Test
    void unknownProductIs404() throws Exception {
        mvc.perform(get("/api/products/nope")).andExpect(status().isNotFound());
    }

    @Test
    void placesOrderWithServerSidePrices() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"standard\",\"promo\":\"hollis10\",\"items\":["
                + "{\"id\":\"tide-one\",\"size\":9,\"width\":\"wide\",\"qty\":1}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("HP-")))
                .andExpect(jsonPath("$.subtotal").value(11798.0))
                .andExpect(jsonPath("$.discount").value(1179.8))
                .andExpect(jsonPath("$.shipping").value(149.0))
                .andExpect(jsonPath("$.total").value(10767.2))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void rejectsSoldOutSize() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"pickup\",\"items\":[{\"id\":\"tide-one\",\"size\":13,\"width\":\"standard\",\"qty\":1}]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Tide One in US 13 is sold out."));
    }

    @Test
    void rejectsWideOnStandardOnlyModel() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"pickup\",\"items\":[{\"id\":\"rally-mid\",\"size\":9,\"width\":\"wide\",\"qty\":1}]}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsBadPromoAndBadPin() throws Exception {
        order("{" + ADDRESS + ",\"ship\":\"pickup\",\"promo\":\"FREE\",\"items\":[{\"id\":\"tide-one\",\"size\":9,\"width\":\"standard\",\"qty\":1}]}")
                .andExpect(status().isUnprocessableEntity());
        order("{" + ADDRESS.replace("411001", "4110") + ",\"ship\":\"pickup\",\"items\":[{\"id\":\"tide-one\",\"size\":9,\"width\":\"standard\",\"qty\":1}]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields.pin").exists());
    }

    @Test
    void promoValidation() throws Exception {
        mvc.perform(post("/api/promo/validate").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"hollis10\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("HOLLIS10")).andExpect(jsonPath("$.rate").value(0.1));
        mvc.perform(post("/api/promo/validate").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"NOPE\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void contactAndNewsletter() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"topic\":\"sizing\",\"message\":\"Which size for Tide One?\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"topic\":\"order\",\"order\":\"X1\",\"message\":\"Where is my order?\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/newsletter").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Asha@Example.com\",\"topics\":[\"running\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.promoCode").value("HOLLIS10"));
        mvc.perform(post("/api/newsletter").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"asha@example.com\"}"))
                .andExpect(status().isCreated());
    }
}
