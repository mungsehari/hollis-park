package com.hollispark.store.dto.marketing;

import java.math.BigDecimal;

public record NewsletterResponse(String status, String promoCode, BigDecimal promoRate) {}
