package com.hollispark.store.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ShipMethod {
    @JsonProperty("standard") STANDARD,
    @JsonProperty("express") EXPRESS,
    @JsonProperty("pickup") PICKUP
}
