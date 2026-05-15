package org.josh.store.service;

import org.josh.store.model.Product;
import org.josh.store.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return getAllProducts();
        }
        return productRepository.searchProducts(query.trim());
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findByStockQuantityLessThan(threshold);
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public Product createProduct(Product product) {
        if (product.getStockQuantity() < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }
        if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Price cannot be negative");
        }
        return productRepository.save(product);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public Product updateProduct(Long id, Product updatedProduct) {
        Product existing = getProductById(id);
        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStockQuantity(updatedProduct.getStockQuantity());
        existing.setCategory(updatedProduct.getCategory());
        existing.setImageUrl(updatedProduct.getImageUrl());
        return productRepository.save(existing);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteProduct(Long id) {
        Product existing = getProductById(id);
        productRepository.delete(existing);
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long getTotalProducts() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public long getTotalCategories() {
        return productRepository.findAllCategories().size();
    }

    @Transactional(readOnly = true)
    public long getLowStockCount() {
        return productRepository.findByStockQuantityLessThan(10).size();
    }

    // ─── Advanced Queries ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getProductsByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceRange(min, max);
    }

    @Transactional(readOnly = true)
    public BigDecimal getInventoryValueByCategory(String category) {
        return productRepository.getInventoryValueByCategory(category);
    }

    public int updatePricesByCategory(String category, double percentage) {
        BigDecimal factor = BigDecimal.valueOf(1 + (percentage / 100));
        return productRepository.updatePricesByCategory(category, factor);
    }
}
