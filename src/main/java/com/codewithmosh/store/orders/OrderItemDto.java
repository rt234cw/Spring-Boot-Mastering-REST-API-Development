package com.codewithmosh.store.orders;


import com.codewithmosh.store.products.OrderProductDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private OrderProductDto product;
    private int quantity;
    private BigDecimal totalPrice;
}
