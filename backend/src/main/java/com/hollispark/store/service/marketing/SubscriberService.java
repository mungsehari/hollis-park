package com.hollispark.store.service.marketing;

import com.hollispark.store.dto.base.PaginationDto;
import com.hollispark.store.dto.marketing.NewsletterRequest;
import com.hollispark.store.dto.marketing.NewsletterResponse;
import com.hollispark.store.dto.marketing.SubscriberDto;
import com.hollispark.store.entity.marketing.Subscriber;
import com.hollispark.store.repository.marketing.PromoCodeRepository;
import com.hollispark.store.repository.marketing.SubscriberRepository;
import com.hollispark.store.util.PageRequests;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Newsletter sign-ups and the subscriber list. */
@Service
@RequiredArgsConstructor
public class SubscriberService {
    private final SubscriberRepository subscribers;
    private final PromoCodeRepository promos;

    /** Signing up twice just updates the topics. The reply carries the welcome code, if there is one. */
    @Transactional
    public NewsletterResponse subscribe(NewsletterRequest req) {
        String email = req.email().trim().toLowerCase();
        Subscriber s = subscribers.findByEmail(email).orElseGet(() -> Subscriber.builder().email(email).build());
        s.setTopics(req.topics() == null ? List.of() : req.topics());
        subscribers.save(s);
        var first = promos.findFirstByActiveTrueOrderByCreatedAtAsc().orElse(null);
        return new NewsletterResponse("subscribed", first == null ? null : first.getCode(), first == null ? null : first.getRate());
    }

    @Transactional(readOnly = true)
    public PaginationDto<SubscriberDto> list(int page, int size) {
        return PaginationDto.of(subscribers.findAll(PageRequests.newestFirst(page, size, 200)), SubscriberDto::of);
    }

    /** CSV export. Cells are quoted and spreadsheet formulas (=, +, -, @) are neutralised. */
    @Transactional(readOnly = true)
    public String csv() {
        StringBuilder csv = new StringBuilder("email,topics,subscribed_at\n");
        subscribers.findAll(Sort.by("createdAt")).forEach(s -> csv
                .append(cell(s.getEmail())).append(',')
                .append(cell(s.getTopics() == null ? "" : String.join(" ", s.getTopics()))).append(',')
                .append(s.getCreatedAt()).append('\n'));
        return csv.toString();
    }

    private static String cell(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
