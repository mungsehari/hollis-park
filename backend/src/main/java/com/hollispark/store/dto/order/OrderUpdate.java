package com.hollispark.store.dto.order;

import jakarta.validation.constraints.Size;

public record OrderUpdate(String status, @Size(max = 500) String note, @Size(max = 2000) String adminNote) {}
