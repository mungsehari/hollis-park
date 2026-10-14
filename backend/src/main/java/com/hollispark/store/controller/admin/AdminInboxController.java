package com.hollispark.store.controller.admin;

import com.hollispark.store.dto.base.PageDto;
import com.hollispark.store.dto.marketing.MessageDto;
import com.hollispark.store.dto.marketing.MessageUpdate;
import com.hollispark.store.dto.marketing.SubscriberDto;
import com.hollispark.store.entity.marketing.ContactMessage;
import com.hollispark.store.exception.ApiException;
import com.hollispark.store.repository.marketing.ContactMessageRepository;
import com.hollispark.store.repository.marketing.SubscriberRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer messages and newsletter subscribers. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminInboxController {
    private final ContactMessageRepository messages;
    private final SubscriberRepository subscribers;

    @GetMapping("/messages")
    public PageDto<MessageDto> messages(@RequestParam(required = false) String status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null || status.isBlank()
                ? messages.findAll(pageable) : messages.findByStatus(status.toUpperCase(), pageable);
        return PageDto.of(result.map(MessageDto::of));
    }

    @PatchMapping("/messages/{id}")
    @Transactional
    public MessageDto updateMessage(@PathVariable UUID id, @Valid @RequestBody MessageUpdate req) {
        ContactMessage m = messages.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found."));
        m.setStatus(req.status());
        return MessageDto.of(m);
    }

    @GetMapping("/subscribers")
    public PageDto<SubscriberDto> subscribers(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageDto.of(subscribers.findAll(pageable).map(SubscriberDto::of));
    }

    @GetMapping("/subscribers.csv")
    public ResponseEntity<String> subscribersCsv() {
        StringBuilder csv = new StringBuilder("email,topics,subscribed_at\n");
        subscribers.findAll(Sort.by("createdAt")).forEach(s -> csv
                .append(csvCell(s.getEmail())).append(',')
                .append(csvCell(s.getTopics() == null ? "" : String.join(" ", s.getTopics()))).append(',')
                .append(s.getCreatedAt()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"subscribers.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv.toString());
    }

    /** Quotes the cell and neutralises spreadsheet formulas (=, +, -, @). */
    private static String csvCell(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
