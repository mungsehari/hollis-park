package com.hollispark.store.dto.order;

import com.hollispark.store.enums.ShipMethod;
import com.hollispark.store.enums.Width;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 80) String first,
        @NotBlank @Size(max = 80) String last,
        @NotBlank @Size(max = 300) String address,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "Use a 6-digit PIN code.") String pin,
        @NotNull ShipMethod ship,
        @Size(max = 40) String promo,
        @NotEmpty @Size(max = 50) List<@Valid Item> items) {

    public record Item(
            @NotBlank String id,
            @NotNull Double size,
            @NotNull Width width,
            @Min(1) @Max(9) int qty) {}
}
