package org.josh.store.controller;

import jakarta.validation.Valid;
import org.josh.store.model.Product;
import org.josh.store.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ─── GET all products ────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            org.springframework.data.domain.Pageable pageable
    ) {

        List<Product> products;

        if (search != null && !search.isBlank()) {
            products = productService.searchProducts(search, pageable);
        } else if (category != null && !category.isBlank()) {
            products = productService.getProductsByCategory(category, pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        return ResponseEntity.ok(products);
    }

    // ─── GET single product ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ─── GET categories ───────────────────────────────────────────────────────
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    // ─── GET stats ────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalProducts", productService.getTotalProducts());
        stats.put("totalCategories", productService.getTotalCategories());
        stats.put("lowStockCount", productService.getLowStockCount());
        return ResponseEntity.ok(stats);
    }

    // ─── POST create product ──────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── PUT update product ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    // ─── DELETE product ───────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Product deleted successfully");
        return ResponseEntity.ok(response);
    }

    // ─── Advanced Queries ─────────────────────────────────────────────────────

    @GetMapping("/price-range")
    public ResponseEntity<List<Product>> getByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(min, max));
    }

    @GetMapping("/inventory-value")
    public ResponseEntity<Map<String, Object>> getInventoryValue(@RequestParam String category) {
        BigDecimal value = productService.getInventoryValueByCategory(category);
        Map<String, Object> response = new HashMap<>();
        response.put("category", category);
        response.put("totalInventoryValue", value);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update-prices")
    public ResponseEntity<Map<String, Object>> updatePrices(
            @RequestParam String category,
            @RequestParam double percentage) {
        int updatedCount = productService.updatePricesByCategory(category, percentage);
        Map<String, Object> response = new HashMap<>();
        response.put("category", category);
        response.put("updatedCount", updatedCount);
        response.put("message", "Prices updated by " + percentage + "%");
        return ResponseEntity.ok(response);
    }

    // ─── Global exception handler ─────────────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
