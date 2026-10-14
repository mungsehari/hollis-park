package com.hollispark.store.dto.catalog;

import java.time.Instant;

public record ProductAdmin(ProductDto product, boolean active, Instant updatedAt) {}
