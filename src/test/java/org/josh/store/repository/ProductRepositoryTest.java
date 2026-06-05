package org.josh.store.repository;

import org.josh.store.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testSearchProducts() {
        // Arrange
        Product p1 = Product.builder().name("Gaming Laptop").description("High performance laptop").price(BigDecimal.valueOf(1200)).stockQuantity(10).category("Electronics").build();
        Product p2 = Product.builder().name("Office Chair").description("Ergonomic office chair").price(BigDecimal.valueOf(150)).stockQuantity(20).category("Furniture").build();
        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.flush();

        // Act
        List<Product> results = productRepository.searchProducts("laptop", PageRequest.of(0, 10));

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    void testFindByCategoryIgnoreCase() {
        // Arrange
        Product p1 = Product.builder().name("Gaming Laptop").description("High performance laptop").price(BigDecimal.valueOf(1200)).stockQuantity(10).category("Electronics").build();
        Product p2 = Product.builder().name("Office Chair").description("Ergonomic office chair").price(BigDecimal.valueOf(150)).stockQuantity(20).category("Furniture").build();
        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.flush();

        // Act
        List<Product> electronics = productRepository.findByCategoryIgnoreCase("electronics", PageRequest.of(0, 10));

        // Assert
        assertThat(electronics).hasSize(1);
        assertThat(electronics.get(0).getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    void testFindAllCategories() {
        // Arrange
        Product p1 = Product.builder().name("Laptop").description("Laptop").price(BigDecimal.valueOf(1000)).stockQuantity(5).category("Electronics").build();
        Product p2 = Product.builder().name("Chair").description("Chair").price(BigDecimal.valueOf(100)).stockQuantity(5).category("Furniture").build();
        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.flush();

        // Act
        List<String> categories = productRepository.findAllCategories();

        // Assert
        assertThat(categories).containsExactly("Electronics", "Furniture");
    }

    @Test
    void testFindByPriceRange() {
        // Arrange
        Product p1 = Product.builder().name("Mouse").description("Mouse").price(BigDecimal.valueOf(20)).stockQuantity(5).category("Electronics").build();
        Product p2 = Product.builder().name("Keyboard").description("Keyboard").price(BigDecimal.valueOf(50)).stockQuantity(5).category("Electronics").build();
        Product p3 = Product.builder().name("Monitor").description("Monitor").price(BigDecimal.valueOf(200)).stockQuantity(5).category("Electronics").build();
        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.flush();

        // Act
        List<Product> range = productRepository.findByPriceRange(BigDecimal.valueOf(15), BigDecimal.valueOf(60));

        // Assert
        assertThat(range).hasSize(2);
        assertThat(range).extracting(Product::getName).containsExactly("Mouse", "Keyboard");
    }

    @Test
    void testGetInventoryValueByCategory() {
        // Arrange
        Product p1 = Product.builder().name("Laptop").description("Laptop").price(BigDecimal.valueOf(1000)).stockQuantity(2).category("Electronics").build();
        Product p2 = Product.builder().name("Phone").description("Phone").price(BigDecimal.valueOf(500)).stockQuantity(3).category("Electronics").build();
        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.flush();

        // Act
        BigDecimal value = productRepository.getInventoryValueByCategory("electronics");

        // Assert
        // (1000 * 2) + (500 * 3) = 3500.00
        assertThat(value).isEqualByComparingTo(BigDecimal.valueOf(3500));
    }

    @Test
    void testUpdatePricesByCategory() {
        // Arrange
        Product p1 = Product.builder().name("Desk").description("Desk").price(BigDecimal.valueOf(100)).stockQuantity(5).category("Furniture").build();
        entityManager.persist(p1);
        entityManager.flush();

        // Act
        int updatedRows = productRepository.updatePricesByCategory("Furniture", BigDecimal.valueOf(1.10)); // +10%
        entityManager.clear(); // Clear context to reload from DB

        // Assert
        assertThat(updatedRows).isEqualTo(1);
        Product updatedProduct = productRepository.findById(p1.getId()).orElseThrow();
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(110));
    }
}
