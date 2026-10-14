package com.hollispark.store.entity.content;

import com.hollispark.store.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Media extends BaseEntity {
    @Column(name = "storage_key")
    private String storageKey;
    private String url;
    @Column(name = "original_name")
    private String originalName;
    @Column(name = "content_type")
    private String contentType;
    @Column(name = "size_bytes")
    private long sizeBytes;
    private String alt;
    @Column(name = "created_by")
    private String createdBy;
}
