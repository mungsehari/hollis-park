package com.hollispark.store.service;

import com.hollispark.store.domain.ContactRepository;
import com.hollispark.store.domain.CustomerOrder;
import com.hollispark.store.domain.Marketplace;
import com.hollispark.store.domain.OrderLine;
import com.hollispark.store.domain.OrderRepository;
import com.hollispark.store.domain.OrderStatus;
import com.hollispark.store.domain.OutboundClickRepository;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.domain.SubscriberRepository;
import com.hollispark.store.dto.order.OrderSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The numbers on the admin dashboard: sales, open work and best sellers. Cancelled and unpaid orders are not sales. */
@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final OrderRepository orders;
    private final ProductRepository products;
    private final ContactRepository messages;
    private final SubscriberRepository subscribers;
    private final OutboundClickRepository clicks;

    public record DayPoint(String date, int orders, BigDecimal revenue) {}

    public record TopProduct(String id, String name, int qty, BigDecimal revenue) {}

    /** Something people followed out to a marketplace, and how often. */
    public record OutboundTop(String name, String marketplace, long clicks) {}

    public record Dashboard(int ordersToday, BigDecimal revenueToday, int orders7d, BigDecimal revenue7d,
                            BigDecimal revenue30d, BigDecimal averageOrder30d, long openOrders, long newMessages,
                            long subscribers, long activeProducts, Map<String, Long> statusCounts,
                            List<DayPoint> salesByDay, List<TopProduct> topProducts, List<OrderSummary> recentOrders,
                            Map<String, Long> marketplaceClicks30d, List<OutboundTop> topOutbound30d) {}

    @Transactional(readOnly = true)
    public Dashboard summary() {
        LocalDate today = LocalDate.now(ZONE);
        Instant from30 = today.minusDays(29).atStartOfDay(ZONE).toInstant();
        List<CustomerOrder> sales = orders.findByCreatedAtAfterOrderByCreatedAtDesc(from30).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.PENDING_PAYMENT)
                .toList();

        Map<LocalDate, Integer> countByDay = new TreeMap<>();
        Map<LocalDate, BigDecimal> revenueByDay = new TreeMap<>();
        for (int i = 13; i >= 0; i--) {
            countByDay.put(today.minusDays(i), 0);
            revenueByDay.put(today.minusDays(i), BigDecimal.ZERO);
        }
        Map<String, TopProduct> top = new LinkedHashMap<>();
        int ordersToday = 0;
        int orders7d = 0;
        BigDecimal revenueToday = BigDecimal.ZERO;
        BigDecimal revenue7d = BigDecimal.ZERO;
        BigDecimal revenue30d = BigDecimal.ZERO;
        for (CustomerOrder o : sales) {
            LocalDate day = o.getCreatedAt().atZone(ZONE).toLocalDate();
            long age = ChronoUnit.DAYS.between(day, today);
            BigDecimal total = o.getTotal();
            revenue30d = revenue30d.add(total);
            if (age < 7) {
                orders7d++;
                revenue7d = revenue7d.add(total);
            }
            if (age == 0) {
                ordersToday++;
                revenueToday = revenueToday.add(total);
            }
            if (countByDay.containsKey(day)) {
                countByDay.merge(day, 1, Integer::sum);
                revenueByDay.merge(day, total, BigDecimal::add);
            }
            for (OrderLine line : o.getItems()) {
                top.merge(line.productId(), new TopProduct(line.productId(), line.name(), line.qty(), line.lineTotal()),
                        (a, b) -> new TopProduct(a.id(), a.name(), a.qty() + b.qty(), a.revenue().add(b.revenue())));
            }
        }
        List<DayPoint> salesByDay = new ArrayList<>();
        countByDay.forEach((day, n) -> salesByDay.add(new DayPoint(day.toString(), n, revenueByDay.get(day))));

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) statusCounts.put(s.name(), 0L);
        for (Object[] row : orders.countByStatusGrouped()) statusCounts.put(((OrderStatus) row[0]).name(), (Long) row[1]);

        Map<String, Long> marketplaceClicks = new LinkedHashMap<>();
        for (Marketplace m : Marketplace.values()) marketplaceClicks.put(m.name(), 0L);
        for (Object[] row : clicks.countByMarketplaceSince(from30)) marketplaceClicks.put(((Marketplace) row[0]).name(), (Long) row[1]);
        List<OutboundTop> topOutbound = clicks.topSince(from30, PageRequest.of(0, 5)).stream()
                .map(r -> new OutboundTop((String) r[0], ((Marketplace) r[1]).label(), (Long) r[2])).toList();

        BigDecimal average = sales.isEmpty() ? BigDecimal.ZERO
                : revenue30d.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);
        return new Dashboard(ordersToday, revenueToday, orders7d, revenue7d, revenue30d, average,
                statusCounts.get("RECEIVED") + statusCounts.get("CONFIRMED"),
                messages.countByStatus("NEW"), subscribers.count(), products.countByActiveTrue(),
                statusCounts, salesByDay,
                top.values().stream().sorted(Comparator.comparingInt(TopProduct::qty).reversed()).limit(5).toList(),
                orders.findTop5ByOrderByCreatedAtDesc().stream().map(OrderSummary::of).toList(),
                marketplaceClicks, topOutbound);
    }
}
