package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.base.PaginationDto;
import com.hollispark.store.dto.marketing.SubscriberDto;
import com.hollispark.store.service.marketing.SubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: subscribers")
@RestController
@RequestMapping("/api/v1/admin/subscribers")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminSubscriberController {
    private final SubscriberService subscribers;

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationDto<SubscriberDto>>> list(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(subscribers.list(page, size)));
    }

    @Operation(summary = "All subscribers as CSV (a file download, not the JSON envelope)")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> csv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"subscribers.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(subscribers.csv());
    }
}
