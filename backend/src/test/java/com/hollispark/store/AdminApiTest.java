package com.hollispark.store;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"store.media.local-dir=target/test-media", "store.razorpay.key-id=", "store.razorpay.key-secret="})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES, provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class AdminApiTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private String placeOrder() throws Exception {
        String body = """
                {"email":"ravi@example.com","first":"Ravi","last":"K","address":"1 Park St","city":"Kolkata","pin":"700016",
                 "ship":"pickup","items":[{"id":"rally-mid","size":10,"width":"standard","qty":1}]}""";
        String res = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("orderNumber").asText();
    }

    @Test
    void adminApiNeedsSignIn() throws Exception {
        mvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void signInWithBootstrapAdmin() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ADMIN@hollispark.local\",\"password\":\"local-admin-change-me\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginNeedsCsrfToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@hollispark.local\",\"password\":\"local-admin-change-me\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordLocksAfterFiveTries() throws Exception {
        String bad = "{\"email\":\"locked@example.com\",\"password\":\"nope-nope-nope\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bad))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @WithMockUser(username = "staff@hollispark.local", roles = "STAFF")
    void staffCanWorkOrdersButNotEditProductsOrStaff() throws Exception {
        String number = placeOrder();
        mvc.perform(get("/api/admin/orders").param("q", number)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderNumber").value(number));
        mvc.perform(patch("/api/admin/orders/" + number).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\",\"note\":\"Called the customer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.history[1].by").value("staff@hollispark.local"))
                .andExpect(jsonPath("$.nextStatuses", hasItem("SHIPPED")));
        mvc.perform(patch("/api/admin/products/tide-one").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/staff")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void orderStatusFollowsTheLifecycle() throws Exception {
        String number = placeOrder();
        mvc.perform(patch("/api/admin/orders/" + number).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/orders/" + number).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\",\"adminNote\":\"Duplicate\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.adminNote").value("Duplicate"));
        mvc.perform(patch("/api/admin/orders/" + number).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void archivedProductLeavesTheStore() throws Exception {
        mvc.perform(patch("/api/admin/products/everyday-arc").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/products/everyday-arc")).andExpect(status().isNotFound());
        mvc.perform(get("/api/store")).andExpect(jsonPath("$.products[*].id", not(hasItem("everyday-arc"))));
        mvc.perform(patch("/api/admin/products/everyday-arc").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void productEditsAreValidated() throws Exception {
        String product = mvc.perform(get("/api/admin/products/tide-one")).andReturn().getResponse().getContentAsString();
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(product).get("product");
        node.put("price", 9999);
        mvc.perform(put("/api/admin/products/tide-one").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(node.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.product.price").value(9999));
        node.put("collection", "skateboarding");
        mvc.perform(put("/api/admin/products/tide-one").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(node.toString()))
                .andExpect(status().isUnprocessableEntity());
        node.put("collection", "running").put("price", 10999);
        mvc.perform(put("/api/admin/products/tide-one").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(node.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void staffAccountsAndTheLastAdminGuard() throws Exception {
        mvc.perform(post("/api/admin/staff").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"meera@example.com\",\"name\":\"Meera\",\"role\":\"STAFF\",\"password\":\"short\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/admin/staff").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"meera@example.com\",\"name\":\"Meera\",\"role\":\"STAFF\",\"password\":\"long-enough-pass\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STAFF"));
        String list = mvc.perform(get("/api/admin/staff")).andReturn().getResponse().getContentAsString();
        long adminId = 0;
        for (var n : json.readTree(list)) if (n.get("email").asText().equals("admin@hollispark.local")) adminId = n.get("id").asLong();
        mvc.perform(patch("/api/admin/staff/" + adminId).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void settingsAndPromoCodesChangePricing() throws Exception {
        mvc.perform(post("/api/admin/promo-codes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"diwali20\",\"rate\":0.2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("DIWALI20"));
        mvc.perform(post("/api/promo/validate").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"Diwali20\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rate").value(0.2));
        mvc.perform(patch("/api/admin/promo-codes/DIWALI20").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/promo/validate").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"DIWALI20\"}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(put("/api/admin/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxMode\":\"included\",\"taxRate\":0.18,\"taxName\":\"GST\",\"freeShipFrom\":12000,\"shipFee\":149,\"expressFee\":349}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/store")).andExpect(jsonPath("$.config.taxRate").value(0.18));
        mvc.perform(put("/api/admin/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxMode\":\"included\",\"taxRate\":0,\"taxName\":\"GST\",\"freeShipFrom\":12000,\"shipFee\":149,\"expressFee\":349}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void dashboardAndExports() throws Exception {
        placeOrder();
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.salesByDay.length()").value(14))
                .andExpect(jsonPath("$.activeProducts").exists());
        mvc.perform(get("/api/admin/subscribers.csv")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/messages").param("status", "NEW")).andExpect(status().isOk());
    }
}
