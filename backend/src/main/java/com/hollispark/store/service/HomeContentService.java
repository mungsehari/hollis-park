package com.hollispark.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.domain.SiteContent;
import com.hollispark.store.domain.SiteContentRepository;
import com.hollispark.store.dto.ContentDtos.HomeContent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeContentService {
    private static final String ID = "home";
    private final SiteContentRepository repo;
    private final ObjectMapper mapper;

    public HomeContentService(SiteContentRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public static HomeContent defaults(String heroImage) {
        return new HomeContent(
                new HomeContent.Hero("New season", "Made to move.", "Considered sneakers for running, court, training and every day. Wear them outside for 60 days before you decide.",
                        heroImage, "A woman in an off-white windbreaker sits on a concrete block, wearing grey sneakers",
                        "Shop all pairs", "/catalog", "Find your size", "/size-guide"),
                new HomeContent.Banner(true, "Free delivery over ₹12,000 · 60-day wear test on every pair", "/delivery"),
                "tide-one");
    }

    public boolean exists() {
        return repo.existsByPageKey(ID);
    }

    public HomeContent get() {
        return repo.findByPageKey(ID).map(c -> mapper.convertValue(c.getData(), HomeContent.class))
                .orElseGet(() -> defaults(null));
    }

    @Transactional
    public HomeContent save(HomeContent home) {
        SiteContent c = repo.findByPageKey(ID).orElseGet(() -> SiteContent.builder().pageKey(ID).build());
        c.setData(mapper.valueToTree(home));
        repo.save(c);
        return home;
    }
}
