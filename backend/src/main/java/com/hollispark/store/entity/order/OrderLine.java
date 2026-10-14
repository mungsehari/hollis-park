package com.hollispark.store.entity.order;

import java.math.BigDecimal;

/** Snapshot of one bag line at the moment the order was placed. */
public record OrderLine(String productId, String name, double size, String width, int qty,
                        BigDecimal unitPrice, BigDecimal lineTotal, String image) {}
