package com.iamstevol.ecommerce.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(value ="products")
public class Product {

    @Id
    private String id;

    private String name;

    private String description;

    private BigDecimal price;
}
