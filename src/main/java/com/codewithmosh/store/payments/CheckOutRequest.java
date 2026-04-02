package com.codewithmosh.store.payments;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CheckOutRequest {
    @NotNull(message = "cart ID is required")
    private UUID cartId;
}
