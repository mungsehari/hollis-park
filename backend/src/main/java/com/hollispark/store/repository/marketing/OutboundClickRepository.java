package com.hollispark.store.repository.marketing;

import com.hollispark.store.entity.marketing.OutboundClick;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboundClickRepository extends JpaRepository<OutboundClick, UUID> {
    /** Rows are {marketplace, count}. */
    @Query("select c.marketplace, count(c) from OutboundClick c where c.createdAt >= :from group by c.marketplace")
    List<Object[]> countByMarketplaceSince(@Param("from") Instant from);

    /** Rows are {name, marketplace, count}, most clicked first. */
    @Query("select c.targetName, c.marketplace, count(c) from OutboundClick c where c.createdAt >= :from "
            + "group by c.targetName, c.marketplace order by count(c) desc")
    List<Object[]> topSince(@Param("from") Instant from, Pageable page);
}
