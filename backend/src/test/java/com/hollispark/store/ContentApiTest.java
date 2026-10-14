package com.hollispark.store;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"store.media.local-dir=target/test-media", "store.razorpay.key-id=", "store.razorpay.key-secret="})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES, provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class ContentApiTest {
    /** Smallest valid PNG header bytes; the server checks the signature, not the file name. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void seedImagesAreServedAsMedia() throws Exception {
        String store = mvc.perform(get("/api/v1/store")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products[0].img", startsWith("/media/seed/")))
                .andExpect(jsonPath("$.data.products[0].gallery.length()").value(2))
                .andExpect(jsonPath("$.data.collections[0].image", startsWith("/media/seed/")))
                .andExpect(jsonPath("$.data.home.hero.image", startsWith("/media/seed/")))
                .andReturn().getResponse().getContentAsString();
        String img = json.readTree(store).get("data").at("/products/0/img").asText();
        mvc.perform(get(img)).andExpect(status().isOk()).andExpect(content().contentType("image/webp"));
    }

    @Test
    void storeHidesNothingWithoutSignIn() throws Exception {
        mvc.perform(multipart("/api/v1/admin/media").file(new MockMultipartFile("file", "x.png", "image/png", PNG)).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void uploadChecksTheRealFileType() throws Exception {
        mvc.perform(multipart("/api/v1/admin/media").file(new MockMultipartFile("file", "evil.png", "image/png", "<svg onload=alert(1)>".getBytes())).with(csrf()))
                .andExpect(status().isBadRequest());
        String res = mvc.perform(multipart("/api/v1/admin/media").file(new MockMultipartFile("file", "shoe.jpg", "image/jpeg", PNG)).param("alt", "Side view").with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.url", startsWith("/media/uploads/")))
                .andReturn().getResponse().getContentAsString();
        var uploaded = json.readTree(res).get("data");
        mvc.perform(get(uploaded.get("url").asText())).andExpect(status().isOk());

        // Used on the homepage: cannot be deleted until it is replaced.
        String home = mvc.perform(get("/api/v1/admin/home")).andReturn().getResponse().getContentAsString();
        ObjectNode h = (ObjectNode) json.readTree(home).get("data");
        String oldImage = h.at("/hero/image").asText();
        ((ObjectNode) h.get("hero")).put("image", uploaded.get("url").asText()).put("title", "Run your own way.");
        mvc.perform(put("/api/v1/admin/home").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(h.toString()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/store")).andExpect(jsonPath("$.data.home.hero.title").value("Run your own way."));
        mvc.perform(delete("/api/v1/admin/media/" + uploaded.get("id").asText()).with(csrf())).andExpect(status().isConflict());

        ((ObjectNode) h.get("hero")).put("image", oldImage);
        mvc.perform(put("/api/v1/admin/home").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(h.toString())).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/media/" + uploaded.get("id").asText()).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(uploaded.get("url").asText())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void collectionsAreEditable() throws Exception {
        String c = mvc.perform(get("/api/v1/admin/collections/running")).andReturn().getResponse().getContentAsString();
        ObjectNode n = (ObjectNode) json.readTree(c).get("data");
        n.put("tagline", "Daily miles, softer landings.");
        mvc.perform(put("/api/v1/admin/collections/running").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(n.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tagline").value("Daily miles, softer landings."));
        mvc.perform(patch("/api/v1/admin/collections/lifestyle").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/collections")).andExpect(jsonPath("$.data.length()").value(3));
        mvc.perform(patch("/api/v1/admin/collections/lifestyle").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff@hollispark.local", roles = "STAFF")
    void staffCannotChangeContent() throws Exception {
        mvc.perform(get("/api/v1/admin/collections")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/media")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/home").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/admin/media").file(new MockMultipartFile("file", "x.png", "image/png", PNG)).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
