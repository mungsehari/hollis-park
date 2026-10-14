package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ContactMessage extends BaseEntity {
    private String name;
    private String email;
    private String topic;
    @Column(name = "order_number")
    private String orderNumber;
    @Column(name = "fitting_slot")
    private String slot;
    @Column(name = "body")
    private String message;
    /** NEW until a staff member marks it HANDLED. */
    @Builder.Default
    private String status = "NEW";
}
