package com.hollispark.store.entity.order;

import java.time.Instant;

/** One entry in an order's history: who moved it to which status, when, and why. */
public record StatusEvent(String status, Instant at, String by, String note) {}
