package com.iamstevol.orderservice.service.impl;


import com.iamstevol.orderservice.dto.InventoryResponse;
import com.iamstevol.orderservice.dto.OrderLineItemsDto;
import com.iamstevol.orderservice.dto.OrderRequest;
import com.iamstevol.orderservice.event.OrderPlacedEvent;
import com.iamstevol.orderservice.model.Order;
import com.iamstevol.orderservice.model.OrderLineItems;
import com.iamstevol.orderservice.repository.OrderRepository;
import com.iamstevol.orderservice.service.OrderService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Override
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        //Set the order number
        order.setOrderNumber(UUID.randomUUID().toString());

        //Map the orderLineItemsDto to the OrderLineItem of the order model
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToOrderLineItems)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(orderLineItem -> orderLineItem.getSkuCode())
                .toList();

        log.info("Calling the inventory service");

        //Creating my own span Id
        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        //Micrometer will assign the initially created span Id to this piece of code
        //To add the manual span Id setup, was why this code was reformatted to this
        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
            //Call inventory service and place order if stock is available
            //The uriBuilder will help with include skuCodes in the request parameters
            InventoryResponse[] inventoryResponsesArray = webClientBuilder.build()
                    .get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsIsInStock = Arrays.stream(inventoryResponsesArray)
                    .allMatch(inventoryResponse -> inventoryResponse.isInStock());

            if (allProductsIsInStock) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order Placed Successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        } finally {
            inventoryServiceLookup.end();
        }

    }

    //this method name should actually be mapToOrderLineItems
    public OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}

