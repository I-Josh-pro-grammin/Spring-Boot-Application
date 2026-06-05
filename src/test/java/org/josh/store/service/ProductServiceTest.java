package org.josh.store.service;

import org.josh.store.model.Product;
import org.josh.store.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void testGetAllProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Product product = Product.builder().name("Test").category("Test").price(BigDecimal.TEN).stockQuantity(5).build();
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(pageable)).thenReturn(page);

        // Act
        List<Product> result = productService.getAllProducts(pageable);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test");
    }

    @Test
    void testGetProductById_Success() {
        // Arrange
        Product product = Product.builder().name("Test").category("Test").price(BigDecimal.TEN).stockQuantity(5).build();
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        Product result = productService.getProductById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void testGetProductById_NullIdThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product ID cannot be null");
    }

    @Test
    void testGetProductById_NotFoundThrowsException() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 1");
    }

    @Test
    void testSearchProducts_EmptyQueryReturnsAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Product product = Product.builder().name("Test").category("Test").price(BigDecimal.TEN).stockQuantity(5).build();
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(pageable)).thenReturn(page);

        // Act
        List<Product> result = productService.searchProducts("", pageable);

        // Assert
        assertThat(result).hasSize(1);
        verify(productRepository, times(1)).findAll(pageable);
        verify(productRepository, never()).searchProducts(anyString(), any(Pageable.class));
    }

    @Test
    void testSearchProducts_WithQuery() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Product product = Product.builder().name("Test").category("Test").price(BigDecimal.TEN).stockQuantity(5).build();
        when(productRepository.searchProducts("test", pageable)).thenReturn(List.of(product));

        // Act
        List<Product> result = productService.searchProducts(" test ", pageable);

        // Assert
        assertThat(result).hasSize(1);
        verify(productRepository, times(1)).searchProducts("test", pageable);
    }

    @Test
    void testCreateProduct_Success() {
        // Arrange
        Product p = Product.builder().name("Valid Product").price(BigDecimal.TEN).stockQuantity(5).category("Electronics").build();
        when(productRepository.save(p)).thenReturn(p);

        // Act
        Product result = productService.createProduct(p);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Valid Product");
    }

    @Test
    void testCreateProduct_NegativeStockThrowsException() {
        // Arrange
        Product p = Product.builder().name("Invalid Stock").price(BigDecimal.TEN).stockQuantity(-1).category("Electronics").build();

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(p))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock quantity cannot be negative");
    }

    @Test
    void testCreateProduct_NegativePriceThrowsException() {
        // Arrange
        Product p = Product.builder().name("Invalid Price").price(BigDecimal.valueOf(-10)).stockQuantity(5).category("Electronics").build();

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(p))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Price cannot be negative");
    }

    @Test
    void testUpdateProduct() {
        // Arrange
        Product existing = Product.builder().name("Old Name").price(BigDecimal.TEN).stockQuantity(5).category("Electronics").build();
        existing.setId(1L);
        Product updated = Product.builder().name("New Name").price(BigDecimal.valueOf(15)).stockQuantity(10).category("Home").build();
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        // Act
        Product result = productService.updateProduct(1L, updated);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(15));
        assertThat(result.getStockQuantity()).isEqualTo(10);
        assertThat(result.getCategory()).isEqualTo("Home");
    }

    @Test
    void testDeleteProduct_Success() {
        // Arrange
        Product product = Product.builder().name("To Delete").price(BigDecimal.TEN).stockQuantity(5).category("Electronics").build();
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).delete(product);
    }

    @Test
    void testGetStats() {
        // Arrange
        when(productRepository.count()).thenReturn(5L);
        when(productRepository.findAllCategories()).thenReturn(List.of("Cat1", "Cat2"));
        when(productRepository.findByStockQuantityLessThan(10)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThat(productService.getTotalProducts()).isEqualTo(5L);
        assertThat(productService.getTotalCategories()).isEqualTo(2L);
        assertThat(productService.getLowStockCount()).isEqualTo(0L);
    }
}
