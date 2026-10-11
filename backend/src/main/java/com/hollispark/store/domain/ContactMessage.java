package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "contact_messages")
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name;
    public String email;
    public String topic;
    @Column(name = "order_number")
    public String orderNumber;
    @Column(name = "fitting_slot")
    public String slot;
    @Column(name = "body")
    public String message;
    /** NEW until a staff member marks it HANDLED. */
    public String status = "NEW";
    @Column(name = "created_at")
    public Instant createdAt;
}
