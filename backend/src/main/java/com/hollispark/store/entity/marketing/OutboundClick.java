package com.hollispark.store.entity.marketing;

import com.hollispark.store.entity.base.BaseEntity;
import com.hollispark.store.enums.Marketplace;
import com.hollispark.store.enums.OutboundTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** One click from the store out to a marketplace. Holds no personal data. */
@Entity
@Table(name = "outbound_clicks")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class OutboundClick extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private OutboundTargetType targetType;
    /** Product slug or care pick id. */
    @Column(name = "target_key")
    private String targetKey;
    /** Name at the time of the click, so the report still reads well if the item is renamed or removed. */
    @Column(name = "target_name")
    private String targetName;
    @Enumerated(EnumType.STRING)
    private Marketplace marketplace;
}
