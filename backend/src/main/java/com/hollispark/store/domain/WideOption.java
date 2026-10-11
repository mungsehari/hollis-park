package com.hollispark.store.domain;

import java.util.List;

/** Wide-fit variant: price supplement and the sizes that are sold out in Wide. */
public record WideOption(int extra, List<Double> soldOut) {}
