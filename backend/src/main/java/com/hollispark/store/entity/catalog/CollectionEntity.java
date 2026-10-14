package com.hollispark.store.entity.catalog;

import com.hollispark.store.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "collections")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CollectionEntity extends BaseEntity {
    /** Web address, e.g. "running". */
    @Column(nullable = false, unique = true, updatable = false)
    private String slug;
    private String name;
    private String tagline;
    private String intro;
    /** Photo URL (an uploaded image). */
    private String image;
    private String alt;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> choose;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<GuideItem> guide;
    @Column(name = "sort_order")
    private int sortOrder;
    @Builder.Default
    private boolean active = true;
}
