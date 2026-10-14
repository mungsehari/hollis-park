package com.hollispark.store.dto.catalog;

import com.hollispark.store.dto.ProductDto;
import java.time.Instant;

public record ProductAdmin(ProductDto product, boolean active, Instant updatedAt) {}
