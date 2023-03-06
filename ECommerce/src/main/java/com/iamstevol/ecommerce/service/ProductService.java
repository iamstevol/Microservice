package com.iamstevol.ecommerce.service;

import com.iamstevol.ecommerce.dto.ProductRequest;
import com.iamstevol.ecommerce.dto.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ProductService {

    void createProduct(ProductRequest productRequest);

    List<ProductResponse> getAllProducts();
}
