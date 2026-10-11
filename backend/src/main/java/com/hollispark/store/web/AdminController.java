package com.hollispark.store.web;

import com.hollispark.store.domain.ContactMessage;
import com.hollispark.store.domain.ContactRepository;
import com.hollispark.store.domain.CustomerOrder;
import com.hollispark.store.domain.OrderLine;
import com.hollispark.store.domain.OrderRepository;
import com.hollispark.store.domain.OrderStatus;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.domain.PromoCode;
import com.hollispark.store.domain.PromoCodeRepository;
import com.hollispark.store.domain.StaffUser;
import com.hollispark.store.domain.StaffUserRepository;
import com.hollispark.store.domain.SubscriberRepository;
import com.hollispark.store.dto.AdminDtos.MessageDto;
import com.hollispark.store.dto.AdminDtos.MessageUpdate;
import com.hollispark.store.dto.AdminDtos.OrderSummary;
import com.hollispark.store.dto.AdminDtos.PageDto;
import com.hollispark.store.dto.AdminDtos.PasswordReset;
import com.hollispark.store.dto.AdminDtos.PromoCreate;
import com.hollispark.store.dto.AdminDtos.PromoDto;
import com.hollispark.store.dto.AdminDtos.PromoUpdate;
import com.hollispark.store.dto.AdminDtos.SettingsDto;
import com.hollispark.store.dto.AdminDtos.StaffCreate;
import com.hollispark.store.dto.AdminDtos.StaffDto;
import com.hollispark.store.dto.AdminDtos.StaffUpdate;
import com.hollispark.store.dto.AdminDtos.SubscriberDto;
import com.hollispark.store.service.ApiException;
import com.hollispark.store.service.SettingsService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard, messages, subscribers, staff accounts, settings and promo codes. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final OrderRepository orders;
    private final ProductRepository products;
    private final ContactRepository messages;
    private final SubscriberRepository subscribers;
    private final StaffUserRepository staff;
    private final PromoCodeRepository promos;
    private final SettingsService settings;
    private final PasswordEncoder encoder;

    public AdminController(OrderRepository orders, ProductRepository products, ContactRepository messages,
                           SubscriberRepository subscribers, StaffUserRepository staff, PromoCodeRepository promos,
                           SettingsService settings, PasswordEncoder encoder) {
        this.orders = orders;
        this.products = products;
        this.messages = messages;
        this.subscribers = subscribers;
        this.staff = staff;
        this.promos = promos;
        this.settings = settings;
        this.encoder = encoder;
    }

    /* ---------------- dashboard ---------------- */

    public record DayPoint(String date, int orders, BigDecimal revenue) {}

    public record TopProduct(String id, String name, int qty, BigDecimal revenue) {}

    public record Dashboard(int ordersToday, BigDecimal revenueToday, int orders7d, BigDecimal revenue7d,
                            BigDecimal revenue30d, BigDecimal averageOrder30d, long openOrders, long newMessages,
                            long subscribers, long activeProducts, Map<String, Long> statusCounts,
                            List<DayPoint> salesByDay, List<TopProduct> topProducts, List<OrderSummary> recentOrders) {}

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        LocalDate today = LocalDate.now(ZONE);
        Instant from30 = today.minusDays(29).atStartOfDay(ZONE).toInstant();
        List<CustomerOrder> recent = orders.findByCreatedAtAfterOrderByCreatedAtDesc(from30).stream()
                .filter(o -> o.status != OrderStatus.CANCELLED && o.status != OrderStatus.PENDING_PAYMENT).toList();

        Map<LocalDate, int[]> counts = new TreeMap<>();
        Map<LocalDate, BigDecimal> revenue = new TreeMap<>();
        for (int i = 13; i >= 0; i--) { counts.put(today.minusDays(i), new int[1]); revenue.put(today.minusDays(i), BigDecimal.ZERO); }
        Map<String, TopProduct> top = new LinkedHashMap<>();
        int ordersToday = 0, orders7d = 0;
        BigDecimal revToday = BigDecimal.ZERO, rev7 = BigDecimal.ZERO, rev30 = BigDecimal.ZERO;
        for (CustomerOrder o : recent) {
            LocalDate d = o.createdAt.atZone(ZONE).toLocalDate();
            long age = ChronoUnit.DAYS.between(d, today);
            rev30 = rev30.add(o.total);
            if (age < 7) { orders7d++; rev7 = rev7.add(o.total); }
            if (age == 0) { ordersToday++; revToday = revToday.add(o.total); }
            if (counts.containsKey(d)) { counts.get(d)[0]++; revenue.merge(d, o.total, BigDecimal::add); }
            for (OrderLine l : o.items) {
                top.merge(l.productId(), new TopProduct(l.productId(), l.name(), l.qty(), l.lineTotal()),
                        (a, b) -> new TopProduct(a.id(), a.name(), a.qty() + b.qty(), a.revenue().add(b.revenue())));
            }
        }
        List<DayPoint> days = new ArrayList<>();
        counts.forEach((d, n) -> days.add(new DayPoint(d.toString(), n[0], revenue.get(d))));
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) statusCounts.put(s.name(), orders.countByStatus(s));
        BigDecimal avg = recent.isEmpty() ? BigDecimal.ZERO
                : rev30.divide(BigDecimal.valueOf(recent.size()), 2, java.math.RoundingMode.HALF_UP);
        return new Dashboard(ordersToday, revToday, orders7d, rev7, rev30, avg,
                statusCounts.get("RECEIVED") + statusCounts.get("CONFIRMED"),
                messages.countByStatus("NEW"), subscribers.count(), products.findAllByActiveTrueOrderByRankAsc().size(),
                statusCounts, days,
                top.values().stream().sorted((a, b) -> b.qty() - a.qty()).limit(5).toList(),
                orders.findTop5ByOrderByCreatedAtDesc().stream().map(OrderSummary::of).toList());
    }

    /* ---------------- messages ---------------- */

    @GetMapping("/messages")
    public PageDto<MessageDto> messages(@RequestParam(required = false) String status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null || status.isBlank() ? messages.findAll(pageable) : messages.findByStatus(status.toUpperCase(), pageable);
        return PageDto.of(result.map(MessageDto::of));
    }

    @PatchMapping("/messages/{id}")
    @Transactional
    public MessageDto updateMessage(@PathVariable Long id, @Valid @RequestBody MessageUpdate req) {
        ContactMessage m = messages.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found."));
        m.status = req.status();
        return MessageDto.of(m);
    }

    /* ---------------- subscribers ---------------- */

    @GetMapping("/subscribers")
    public PageDto<SubscriberDto> subscribers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageDto.of(subscribers.findAll(pageable).map(SubscriberDto::of));
    }

    @GetMapping("/subscribers.csv")
    public ResponseEntity<String> subscribersCsv() {
        StringBuilder csv = new StringBuilder("email,topics,subscribed_at\n");
        subscribers.findAll(Sort.by("createdAt")).forEach(s -> csv
                .append(csvCell(s.email)).append(',')
                .append(csvCell(s.topics == null ? "" : String.join(" ", s.topics))).append(',')
                .append(s.createdAt).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"subscribers.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv.toString());
    }

    /** Quotes the cell and neutralises spreadsheet formulas (=, +, -, @). */
    private static String csvCell(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /* ---------------- staff (ADMIN only) ---------------- */

    @GetMapping("/staff")
    public List<StaffDto> staff() {
        return staff.findAll(Sort.by("createdAt")).stream().map(StaffDto::of).toList();
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StaffDto createStaff(@Valid @RequestBody StaffCreate req) {
        String email = req.email().trim().toLowerCase();
        if (staff.findByEmailIgnoreCase(email).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Someone already uses this email.");
        StaffUser u = new StaffUser();
        u.email = email;
        u.name = req.name().trim();
        u.role = StaffUser.Role.valueOf(req.role());
        u.passwordHash = encoder.encode(req.password());
        u.active = true;
        u.createdAt = Instant.now();
        return StaffDto.of(staff.save(u));
    }

    @PatchMapping("/staff/{id}")
    @Transactional
    public StaffDto updateStaff(@PathVariable Long id, @Valid @RequestBody StaffUpdate req, Principal who) {
        StaffUser u = staff.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff member not found."));
        boolean self = u.email.equalsIgnoreCase(who.getName());
        boolean losesAdmin = u.role == StaffUser.Role.ADMIN && u.active
                && ((req.role() != null && !"ADMIN".equals(req.role())) || Boolean.FALSE.equals(req.active()));
        if (self && losesAdmin) throw new ApiException(HttpStatus.CONFLICT, "You cannot remove your own admin access.");
        if (losesAdmin && staff.countByRoleAndActiveTrue(StaffUser.Role.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "The store needs at least one active admin.");
        }
        if (req.name() != null && !req.name().isBlank()) u.name = req.name().trim();
        if (req.role() != null) u.role = StaffUser.Role.valueOf(req.role());
        if (req.active() != null) u.active = req.active();
        return StaffDto.of(u);
    }

    @PostMapping("/staff/{id}/password")
    @Transactional
    public StaffDto resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordReset req) {
        StaffUser u = staff.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff member not found."));
        u.passwordHash = encoder.encode(req.password());
        return StaffDto.of(u);
    }

    /* ---------------- settings ---------------- */

    @GetMapping("/settings")
    public SettingsService.Settings settings() {
        return settings.get();
    }

    @PutMapping("/settings")
    public SettingsService.Settings updateSettings(@Valid @RequestBody SettingsDto req) {
        return settings.update(new SettingsService.Settings(req.taxMode(), req.taxRate(), req.taxName(),
                req.freeShipFrom(), req.shipFee(), req.expressFee()));
    }

    /* ---------------- promo codes (ADMIN only) ---------------- */

    @GetMapping("/promo-codes")
    public List<PromoDto> promoCodes() {
        return promos.findAllByOrderByCreatedAtAsc().stream().map(PromoDto::of).toList();
    }

    @PostMapping("/promo-codes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PromoDto createPromo(@Valid @RequestBody PromoCreate req) {
        String code = req.code().trim().toUpperCase();
        if (promos.existsById(code)) throw new ApiException(HttpStatus.CONFLICT, "This code already exists.");
        PromoCode p = new PromoCode();
        p.code = code;
        p.rate = req.rate();
        p.active = true;
        p.createdAt = Instant.now();
        return PromoDto.of(promos.save(p));
    }

    @PatchMapping("/promo-codes/{code}")
    @Transactional
    public PromoDto updatePromo(@PathVariable String code, @Valid @RequestBody PromoUpdate req) {
        PromoCode p = promos.findById(code.toUpperCase()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Code not found."));
        if (req.active() != null) p.active = req.active();
        if (req.rate() != null) p.rate = req.rate();
        return PromoDto.of(p);
    }

    @DeleteMapping("/promo-codes/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePromo(@PathVariable String code) {
        if (!promos.existsById(code.toUpperCase())) throw new ApiException(HttpStatus.NOT_FOUND, "Code not found.");
        promos.deleteById(code.toUpperCase());
    }
}
