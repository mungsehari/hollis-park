package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "collections")
public class CollectionEntity {
    @Id
    public String id;
    public String name;
    public String tagline;
    public String intro;
    /** Photo URL (an uploaded image). */
    public String image;
    public String alt;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> choose;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<GuideItem> guide;
    @Column(name = "sort_order")
    public int sortOrder;
    public boolean active = true;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
