package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.base.PaginationDto;
import com.hollispark.store.dto.marketing.MessageDto;
import com.hollispark.store.dto.marketing.MessageUpdate;
import com.hollispark.store.service.marketing.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: messages")
@RestController
@RequestMapping("/api/v1/admin/messages")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminMessageController {
    private final MessageService messages;

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationDto<MessageDto>>> list(@RequestParam(required = false) String status,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(messages.list(status, page, size)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageDto>> update(@PathVariable UUID id, @Valid @RequestBody MessageUpdate req) {
        return ResponseEntity.ok(ApiResponse.success(messages.setStatus(id, req.status())));
    }
}
