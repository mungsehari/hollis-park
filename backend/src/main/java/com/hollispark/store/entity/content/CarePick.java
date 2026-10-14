package com.hollispark.store.entity.content;

import com.hollispark.store.entity.base.BaseEntity;
import com.hollispark.store.enums.Marketplace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Something that goes with the shoes (socks, a cleaning kit, insoles), bought on a marketplace through an affiliate link. */
@Entity
@Table(name = "care_picks")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CarePick extends BaseEntity {
    private String title;
    private String blurb;
    /** Photo URL (an uploaded image), optional. */
    private String image;
    @Enumerated(EnumType.STRING)
    private Marketplace marketplace;
    /** The affiliate link; only ever reached through the store's tracked redirect. */
    private String url;
    @Column(name = "sort_order")
    private int sortOrder;
    @Builder.Default
    private boolean active = true;
}
