package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.base.PaginationDto;
import com.hollispark.store.dto.content.MediaDto;
import com.hollispark.store.service.content.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin: media")
@RestController
@RequestMapping("/api/v1/admin/media")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminMediaController {
    private final MediaService media;

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationDto<MediaDto>>> list(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "48") int size) {
        return ResponseEntity.ok(ApiResponse.success(media.list(page, size)));
    }

    @Operation(summary = "Upload an image (JPG, PNG, WebP or AVIF, checked by content, up to 8 MB)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<MediaDto>> upload(@RequestPart("file") MultipartFile file,
                                                        @RequestParam(required = false) String alt, Principal who) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MediaDto.of(media.upload(file, alt, who.getName())), HttpStatus.CREATED.value(), "Image uploaded"));
    }

    @Operation(summary = "Delete an image that nothing uses")
    @DeleteMapping("/{id}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        media.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted"));
    }
}
