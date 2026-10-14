package com.hollispark.store.service;

import com.hollispark.store.domain.StaffUser;
import com.hollispark.store.domain.StaffUserRepository;
import com.hollispark.store.enums.StaffRole;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Staff accounts and the rules around them: the store always keeps at least one active admin. */
@Service
@RequiredArgsConstructor
public class StaffService {
    private final StaffUserRepository staff;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public List<StaffUser> list() {
        return staff.findAll(Sort.by("createdAt"));
    }

    @Transactional
    public StaffUser create(String email, String name, StaffRole role, String password) {
        String normalised = email.trim().toLowerCase();
        if (staff.findByEmailIgnoreCase(normalised).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Someone already uses this email.");
        }
        return staff.save(StaffUser.builder()
                .email(normalised)
                .name(name.trim())
                .role(role)
                .passwordHash(encoder.encode(password))
                .build());
    }

    /** {@code actingEmail} is the signed-in person: nobody may take away their own admin access. */
    @Transactional
    public StaffUser update(UUID id, String name, StaffRole role, Boolean active, String actingEmail) {
        StaffUser u = find(id);
        boolean self = u.getEmail().equalsIgnoreCase(actingEmail);
        boolean losesAdmin = u.getRole() == StaffRole.ADMIN && u.isActive()
                && ((role != null && role != StaffRole.ADMIN) || Boolean.FALSE.equals(active));
        if (self && losesAdmin) throw new ApiException(HttpStatus.CONFLICT, "You cannot remove your own admin access.");
        if (losesAdmin && staff.countByRoleAndActiveTrue(StaffRole.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "The store needs at least one active admin.");
        }
        if (name != null && !name.isBlank()) u.setName(name.trim());
        if (role != null) u.setRole(role);
        if (active != null) u.setActive(active);
        return u;
    }

    @Transactional
    public StaffUser resetPassword(UUID id, String password) {
        StaffUser u = find(id);
        u.setPasswordHash(encoder.encode(password));
        return u;
    }

    private StaffUser find(UUID id) {
        return staff.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff member not found."));
    }
}
