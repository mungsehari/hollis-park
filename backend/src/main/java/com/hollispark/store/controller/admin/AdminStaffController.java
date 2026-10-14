package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.user.PasswordReset;
import com.hollispark.store.dto.user.StaffCreate;
import com.hollispark.store.dto.user.StaffDto;
import com.hollispark.store.dto.user.StaffUpdate;
import com.hollispark.store.enums.StaffRole;
import com.hollispark.store.service.user.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: staff")
@RestController
@RequestMapping("/api/v1/admin/staff")
@RequiredArgsConstructor
@PreAuthorize(Access.ADMIN)
public class AdminStaffController {
    private final StaffService staff;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(staff.list().stream().map(StaffDto::of).toList()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffDto>> create(@Valid @RequestBody StaffCreate req) {
        StaffDto created = StaffDto.of(staff.create(req.email(), req.name(), StaffRole.valueOf(req.role()), req.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, HttpStatus.CREATED.value(), "Staff member added"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffDto>> update(@PathVariable UUID id, @Valid @RequestBody StaffUpdate req, Principal who) {
        StaffRole role = req.role() == null ? null : StaffRole.valueOf(req.role());
        return ResponseEntity.ok(ApiResponse.success(StaffDto.of(staff.update(id, req.name(), role, req.active(), who.getName()))));
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<ApiResponse<StaffDto>> resetPassword(@PathVariable UUID id, @Valid @RequestBody PasswordReset req) {
        return ResponseEntity.ok(ApiResponse.success(StaffDto.of(staff.resetPassword(id, req.password())), "Password reset"));
    }
}
