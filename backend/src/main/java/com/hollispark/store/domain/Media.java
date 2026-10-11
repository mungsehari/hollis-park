package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "media")
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "storage_key")
    public String storageKey;
    public String url;
    @Column(name = "original_name")
    public String originalName;
    @Column(name = "content_type")
    public String contentType;
    @Column(name = "size_bytes")
    public long sizeBytes;
    public String alt;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "created_by")
    public String createdBy;
}
