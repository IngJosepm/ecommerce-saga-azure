package com.bigdavi.ecommerce.inventory.web;

import com.bigdavi.ecommerce.inventory.application.InventoryService;
import com.bigdavi.ecommerce.inventory.domain.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<Product> catalog() {
        return inventoryService.catalog();
    }
}
