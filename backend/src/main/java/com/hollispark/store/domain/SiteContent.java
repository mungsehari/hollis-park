package com.hollispark.store.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Editable page content stored as one JSON document per page. */
@Entity
@Table(name = "site_content")
public class SiteContent {
    @Id
    public String id;
    @JdbcTypeCode(SqlTypes.JSON)
    public JsonNode data;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
