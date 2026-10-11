package com.hollispark.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Content that changes with a release, not in the admin panel: lines, FAQ, size chart, colours. */
@Component
public class StaticContent {
    public final JsonNode lines;
    public final JsonNode faq;
    public final JsonNode faqCats;
    public final JsonNode sizeChart;
    public final JsonNode sizes;
    public final JsonNode colours;

    public StaticContent(ObjectMapper mapper) throws IOException {
        this.lines = load(mapper, "lines");
        this.faq = load(mapper, "faq");
        this.faqCats = load(mapper, "faqCats");
        this.sizeChart = load(mapper, "sizeChart");
        this.sizes = load(mapper, "sizes");
        this.colours = load(mapper, "colours");
    }

    private static JsonNode load(ObjectMapper mapper, String name) throws IOException {
        try (InputStream in = new ClassPathResource("seed/" + name + ".json").getInputStream()) {
            return mapper.readTree(in);
        }
    }
}
