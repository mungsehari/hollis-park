package com.hollispark.store.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Editable page content stored as one JSON document per page. */
@Entity
@Table(name = "site_content")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class SiteContent extends BaseEntity {
    /** Which page this document is, e.g. "home". */
    @Column(name = "page_key", nullable = false, unique = true, updatable = false)
    private String pageKey;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode data;
}
