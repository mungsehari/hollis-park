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
        String store = mvc.perform(get("/api/v1/store")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.picks.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))  // 4 seeded
                .andExpect(jsonPath("$.data.picks[0].marketplaceLabel").value("Amazon"))
                .andExpect(jsonPath("$.data.picks[0].url").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(store).get("data").at("/picks/0/id").asText();

        mvc.perform(get("/api/v1/go/pick/" + id))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://www.amazon.in/")))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void unknownTargetsGoBackToTheStoreInsteadOfAnywhereElse() throws Exception {
        mvc.perform(get("/api/v1/go/product/tide-one/amazon")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/catalog/tide-one"));          // no Amazon link set
        mvc.perform(get("/api/v1/go/product/tide-one/ebay")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/catalog/tide-one"));
        mvc.perform(get("/api/v1/go/pick/00000000-0000-0000-0000-000000000000")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/care"));
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void adminSetsProductLinksOnlyOnTheRealMarketplaceAndTheDashboardCountsClicks() throws Exception {
        ObjectNode p = (ObjectNode) json.readTree(mvc.perform(get("/api/v1/admin/products/rally-mid"))
                .andReturn().getResponse().getContentAsString()).get("data").get("product");

        p.put("amazonUrl", "https://amazon.in.evil.example/dp/1");
        mvc.perform(put("/api/v1/admin/products/rally-mid").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
                .andExpect(status().isBadRequest());

        p.put("amazonUrl", "https://www.amazon.in/dp/B0HOLLIS?tag=hollispark-21").put("flipkartUrl", "https://www.flipkart.com/p/itmhollis");
        mvc.perform(put("/api/v1/admin/products/rally-mid").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.amazonUrl").value("https://www.amazon.in/dp/B0HOLLIS?tag=hollispark-21"));

        mvc.perform(get("/api/v1/store")).andExpect(jsonPath("$.data.products[?(@.id == 'rally-mid')].flipkartUrl").value("https://www.flipkart.com/p/itmhollis"));
        mvc.perform(get("/api/v1/go/product/rally-mid/flipkart")).andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.flipkart.com/p/itmhollis"));
        mvc.perform(get("/api/v1/go/product/rally-mid/amazon")).andExpect(status().isFound());

        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketplaceClicks30d.FLIPKART").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.topOutbound30d[?(@.name == 'Rally Mid')]").exists());
    }

    @Test
    @WithMockUser(username = "admin@hollispark.local", roles = "ADMIN")
    void carePickLinksMustMatchTheirMarketplace() throws Exception {
        String bad = "{\"title\":\"Laces\",\"marketplace\":\"FLIPKART\",\"url\":\"https://www.amazon.in/s?k=laces\",\"sortOrder\":9,\"active\":true}";
        mvc.perform(post("/api/v1/admin/care-picks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
        String good = "{\"title\":\"Laces\",\"marketplace\":\"AMAZON\",\"url\":\"https://www.amazon.in/s?k=laces\",\"sortOrder\":9,\"active\":true}";
        mvc.perform(post("/api/v1/admin/care-picks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(good))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").exists());
        mvc.perform(get("/api/v1/store")).andExpect(jsonPath("$.data.picks[?(@.title == 'Laces')]").exists());
    }

    @Test
    @WithMockUser(username = "staff@hollispark.local", roles = "STAFF")
    void staffCanReadButNotChangePicks() throws Exception {
        mvc.perform(get("/api/v1/admin/care-picks")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/care-picks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
