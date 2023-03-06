package com.iamstevol.orderservice.service;

import com.iamstevol.orderservice.dto.OrderLineItemsDto;
import com.iamstevol.orderservice.dto.OrderRequest;
import com.iamstevol.orderservice.model.OrderLineItems;

public interface OrderService {
    void placeOrder(OrderRequest orderRequest);

    OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItemsDto);
}
