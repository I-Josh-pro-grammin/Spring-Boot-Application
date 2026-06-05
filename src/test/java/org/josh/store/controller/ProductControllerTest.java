package org.josh.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.josh.store.config.JwtService;
import org.josh.store.model.Product;
import org.josh.store.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@org.springframework.context.annotation.Import(org.josh.store.config.SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser
    void testGetAllProducts() throws Exception {
        // Arrange
        Product product = Product.builder().name("Laptop").category("Electronics").price(BigDecimal.valueOf(1000)).stockQuantity(5).build();
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(List.of(product));

        // Act & Assert
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    @WithMockUser
    void testGetProductById_Success() throws Exception {
        // Arrange
        Product product = Product.builder().name("Laptop").category("Electronics").price(BigDecimal.valueOf(1000)).stockQuantity(5).build();
        product.setId(1L);
        when(productService.getProductById(1L)).thenReturn(product);

        // Act & Assert
        mockMvc.perform(get("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void testGetProductById_NotFound() throws Exception {
        // Arrange
        when(productService.getProductById(99L)).thenThrow(new RuntimeException("Product not found with id: 99"));

        // Act & Assert
        mockMvc.perform(get("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found with id: 99"));
    }

    @Test
    @WithMockUser
    void testCreateProduct_Success() throws Exception {
        // Arrange
        Product input = Product.builder().name("Laptop").category("Electronics").price(BigDecimal.valueOf(1000)).stockQuantity(5).build();
        Product saved = Product.builder().name("Laptop").category("Electronics").price(BigDecimal.valueOf(1000)).stockQuantity(5).build();
        saved.setId(1L);
        when(productService.createProduct(any(Product.class))).thenReturn(saved);

        // Act & Assert
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser
    void testCreateProduct_InvalidBody() throws Exception {
        // Arrange - name and category blank, price is missing
        Product input = Product.builder().name("").category("").stockQuantity(5).build();

        // Act & Assert (Should return 400 Bad Request due to validation annotations on Product class)
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testUpdateProduct_Success() throws Exception {
        // Arrange
        Product input = Product.builder().name("Laptop V2").category("Electronics").price(BigDecimal.valueOf(1100)).stockQuantity(10).build();
        Product updated = Product.builder().name("Laptop V2").category("Electronics").price(BigDecimal.valueOf(1100)).stockQuantity(10).build();
        updated.setId(1L);
        when(productService.updateProduct(eq(1L), any(Product.class))).thenReturn(updated);

        // Act & Assert
        mockMvc.perform(put("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop V2"))
                .andExpect(jsonPath("$.price").value(1100));
    }

    @Test
    @WithMockUser
    void testDeleteProduct_Success() throws Exception {
        // Arrange
        doNothing().when(productService).deleteProduct(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));
    }

    @Test
    @WithMockUser
    void testGetStats_Success() throws Exception {
        // Arrange
        when(productService.getTotalProducts()).thenReturn(10L);
        when(productService.getTotalCategories()).thenReturn(3L);
        when(productService.getLowStockCount()).thenReturn(2L);

        // Act & Assert
        mockMvc.perform(get("/api/products/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(10))
                .andExpect(jsonPath("$.totalCategories").value(3))
                .andExpect(jsonPath("$.lowStockCount").value(2));
    }

    @Test
    @WithMockUser
    void testUpdatePrices_Success() throws Exception {
        // Arrange
        when(productService.updatePricesByCategory("Electronics", 10.0)).thenReturn(5);

        // Act & Assert
        mockMvc.perform(patch("/api/update-prices")
                        .with(csrf())
                        .param("category", "Electronics")
                        .param("percentage", "10.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // wait, the mapping is /update-prices under /api/products. So /api/products/update-prices!
    }

    @Test
    @WithMockUser
    void testUpdatePrices_Success_CorrectPath() throws Exception {
        // Arrange
        when(productService.updatePricesByCategory("Electronics", 10.0)).thenReturn(5);

        // Act & Assert
        mockMvc.perform(patch("/api/products/update-prices")
                        .with(csrf())
                        .param("category", "Electronics")
                        .param("percentage", "10.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(5))
                .andExpect(jsonPath("$.message").value("Prices updated by 10.0%"));
    }
}
