package com.hollispark.store.service.marketing;

import com.hollispark.store.dto.base.PaginationDto;
import com.hollispark.store.dto.marketing.ContactRequest;
import com.hollispark.store.dto.marketing.MessageDto;
import com.hollispark.store.entity.marketing.ContactMessage;
import com.hollispark.store.exception.BadRequestException;
import com.hollispark.store.exception.ResourceNotFoundException;
import com.hollispark.store.repository.marketing.ContactMessageRepository;
import com.hollispark.store.util.PageRequests;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Messages from the contact form: received from customers, worked by staff. */
@Service
@RequiredArgsConstructor
public class MessageService {
    private final ContactMessageRepository messages;

    @Transactional
    public void receive(ContactRequest req, String orderPrefix) {
        if ("order".equals(req.topic()) && (req.order() == null || !req.order().matches(java.util.regex.Pattern.quote(orderPrefix) + "[0-9]{5}"))) {
            throw new BadRequestException("Order numbers look like " + orderPrefix + "24817.");
        }
        messages.save(ContactMessage.builder()
                .name(req.name().trim())
                .email(req.email().trim().toLowerCase())
                .topic(req.topic())
                .orderNumber(req.order())
                .slot(req.slot())
                .message(req.message().trim())
                .build());
    }

    @Transactional(readOnly = true)
    public PaginationDto<MessageDto> list(String status, int page, int size) {
        var pageable = PageRequests.newestFirst(page, size, 100);
        var result = status == null || status.isBlank() ? messages.findAll(pageable) : messages.findByStatus(status.toUpperCase(), pageable);
        return PaginationDto.of(result, MessageDto::of);
    }

    @Transactional
    public MessageDto setStatus(UUID id, String status) {
        ContactMessage m = messages.findById(id).orElseThrow(() -> new ResourceNotFoundException("Message not found."));
        m.setStatus(status);
        return MessageDto.of(m);
    }
}
