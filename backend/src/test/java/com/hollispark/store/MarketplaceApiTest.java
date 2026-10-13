package com.hollispark.store;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Amazon / Flipkart links on products, care picks, and the counted redirect. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"store.media.local-dir=target/test-media", "store.razorpay.key-id=", "store.razorpay.key-secret="})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES, provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class MarketplaceApiTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void carePicksAreShownWithoutTheirRawLinksAndClicksAreRedirectedAndCounted() throws Exception {
        String store = mvc.perform(get("/api/store")).andExpect(status().isOk())
                .andExpect(jsonPath("$.picks.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))  // 4 seeded
                .andExpect(jsonPath("$.picks[0].marketplaceLabel").value("Amazon"))
                .andExpect(jsonPath("$.picks[0].url").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(store).at("/picks/0/id").asText();

        mvc.perform(get("/api/go/pick/" + id))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://www.amazon.in/")))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void unknownTargetsGoBackToTheStoreInsteadOfAnywhereElse() throws Exception {
        mvc.perform(get("/api/go/product/tide-one/amazon")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/catalog/tide-one"));          // no Amazon link set
        mvc.perform(get("/api/go/product/tide-one/ebay")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/catalog/tide-one"));
        mvc.perform(get("/api/go/pick/00000000-0000-0000-0000-000000000000")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/care"));
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void adminSetsProductLinksOnlyOnTheRealMarketplaceAndTheDashboardCountsClicks() throws Exception {
        ObjectNode p = (ObjectNode) json.readTree(mvc.perform(get("/api/admin/products/rally-mid"))
                .andReturn().getResponse().getContentAsString()).get("product");

        p.put("amazonUrl", "https://amazon.in.evil.example/dp/1");
        mvc.perform(put("/api/admin/products/rally-mid").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
                .andExpect(status().isUnprocessableEntity());

        p.put("amazonUrl", "https://www.amazon.in/dp/B0HOLLIS?tag=hollispark-21").put("flipkartUrl", "https://www.flipkart.com/p/itmhollis");
        mvc.perform(put("/api/admin/products/rally-mid").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.amazonUrl").value("https://www.amazon.in/dp/B0HOLLIS?tag=hollispark-21"));

        mvc.perform(get("/api/store")).andExpect(jsonPath("$.products[?(@.id == 'rally-mid')].flipkartUrl").value("https://www.flipkart.com/p/itmhollis"));
        mvc.perform(get("/api/go/product/rally-mid/flipkart")).andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.flipkart.com/p/itmhollis"));
        mvc.perform(get("/api/go/product/rally-mid/amazon")).andExpect(status().isFound());

        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.marketplaceClicks30d.FLIPKART").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.topOutbound30d[?(@.name == 'Rally Mid')]").exists());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void carePickLinksMustMatchTheirMarketplace() throws Exception {
        String bad = "{\"title\":\"Laces\",\"marketplace\":\"FLIPKART\",\"url\":\"https://www.amazon.in/s?k=laces\",\"sortOrder\":9,\"active\":true}";
        mvc.perform(post("/api/admin/care-picks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isUnprocessableEntity());
        String good = "{\"title\":\"Laces\",\"marketplace\":\"AMAZON\",\"url\":\"https://www.amazon.in/s?k=laces\",\"sortOrder\":9,\"active\":true}";
        mvc.perform(post("/api/admin/care-picks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(good))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists());
        mvc.perform(get("/api/store")).andExpect(jsonPath("$.picks[?(@.title == 'Laces')]").exists());
    }

    @Test
    @WithMockUser(username = "staff@hollispark.local", roles = "STAFF")
    void staffCanReadButNotChangePicks() throws Exception {
        mvc.perform(get("/api/admin/care-picks")).andExpect(status().isOk());
        mvc.perform(post("/api/admin/care-picks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
