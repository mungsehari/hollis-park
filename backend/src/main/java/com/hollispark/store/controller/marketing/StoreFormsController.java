package com.hollispark.store.controller.marketing;

import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.marketing.ContactRequest;
import com.hollispark.store.dto.marketing.ContactResponse;
import com.hollispark.store.dto.marketing.NewsletterRequest;
import com.hollispark.store.dto.marketing.NewsletterResponse;
import com.hollispark.store.dto.marketing.PromoRequest;
import com.hollispark.store.dto.marketing.PromoResponse;
import com.hollispark.store.exception.BadRequestException;
import com.hollispark.store.service.marketing.MessageService;
import com.hollispark.store.service.marketing.SubscriberService;
import com.hollispark.store.service.settings.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The storefront's small public forms: discount code check, contact form, newsletter. */
@Tag(name = "Store forms", description = "Promo check, contact form, newsletter")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StoreFormsController {
    private final SettingsService settings;
    private final MessageService messages;
    private final SubscriberService subscribers;
    private final StoreProperties props;

    @Operation(summary = "Check a discount code")
    @PostMapping("/promo/validate")
    public ResponseEntity<ApiResponse<PromoResponse>> validatePromo(@Valid @RequestBody PromoRequest req) {
        BigDecimal rate = settings.promoRate(req.code());
        if (rate == null) throw new BadRequestException("That code is not valid.");
        return ResponseEntity.ok(ApiResponse.success(new PromoResponse(req.code().trim().toUpperCase(), rate)));
    }

    @Operation(summary = "Send a message to the store team")
    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<ContactResponse>> contact(@Valid @RequestBody ContactRequest req) {
        messages.receive(req, props.orderPrefix());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new ContactResponse("received"), HttpStatus.CREATED.value(), "Message received"));
    }

    @Operation(summary = "Subscribe to the newsletter")
    @PostMapping("/newsletter")
    public ResponseEntity<ApiResponse<NewsletterResponse>> subscribe(@Valid @RequestBody NewsletterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(subscribers.subscribe(req), HttpStatus.CREATED.value(), "Subscribed"));
    }
}
