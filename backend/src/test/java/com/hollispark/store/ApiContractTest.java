package com.hollispark.store;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The response contract every client relies on: one envelope {success, status, message, data[, requestId]},
 * client mistakes are 4xx (never 500), and errors never carry framework or SQL text.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"store.media.local-dir=target/test-media", "store.razorpay.key-id=", "store.razorpay.key-secret="})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES, provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class ApiContractTest {
    @Autowired
    MockMvc mvc;

    private static void failure(ResultActions r, int status) throws Exception {
        r.andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    void successUsesTheEnvelope() throws Exception {
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value("tide-one"))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }

    @Test
    void clientMistakesAreFourHundredsWithTheEnvelope() throws Exception {
        failure(mvc.perform(get("/api/v1/no-such-route")), 404);
        failure(mvc.perform(get("/api/v1/products/no-such-pair")), 404);
        failure(mvc.perform(delete("/api/v1/products").with(csrf())), 405);
        failure(mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{not json")), 400);
        failure(mvc.perform(post("/api/v1/orders").contentType(MediaType.TEXT_PLAIN).content("hello")), 415);
        failure(mvc.perform(get("/api/v1/go/pick/not-a-uuid")), 400);
    }

    @Test
    void validationErrorsListTheFieldsInData() throws Exception {
        failure(mvc.perform(post("/api/v1/contact").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nope\"}")), 400);
        mvc.perform(post("/api/v1/contact").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nope\"}"))
                .andExpect(jsonPath("$.message").value("Some details need attention."))
                .andExpect(jsonPath("$.data.email").isString())
                .andExpect(jsonPath("$.data.name").isString());
    }

    @Test
    void signedOutIs401AndWrongRoleIs403() throws Exception {
        failure(mvc.perform(get("/api/v1/admin/orders")), 401);
        failure(mvc.perform(get("/api/v1/auth/me")), 401);
    }

    @Test
    @WithMockUser(username = "staff@hollispark.local", roles = "STAFF")
    void staffCannotReachAdminOnlyAreas() throws Exception {
        failure(mvc.perform(get("/api/v1/admin/staff")), 403);
        failure(mvc.perform(get("/api/v1/admin/promo-codes")), 403);
        failure(mvc.perform(post("/api/v1/admin/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")), 403);
        mvc.perform(get("/api/v1/admin/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void apiDescriptionIsPublishedOutsideProduction() throws Exception {
        mvc.perform(get("/api/v1/docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("Hollis Park store API"));
    }
}
