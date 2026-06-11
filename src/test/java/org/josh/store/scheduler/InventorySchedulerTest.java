package org.josh.store.scheduler;

import org.josh.store.model.Product;
import org.josh.store.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventorySchedulerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @InjectMocks
    private InventoryScheduler inventoryScheduler;

    @Test
    void testCheckLowStock_Empty() {
        // Arrange
        when(productService.getLowStockProducts(10)).thenReturn(Collections.emptyList());

        // Act
        inventoryScheduler.checkLowStock();

        // Assert
        verify(productService, times(1)).getLowStockProducts(10);
    }

    @Test
    void testCheckLowStock_WithProducts() {
        // Arrange
        Product p1 = Product.builder()
                .name("Low Stock Product")
                .price(BigDecimal.TEN)
                .stockQuantity(5)
                .category("Electronics")
                .build();
        p1.setId(1L);
        when(productService.getLowStockProducts(10)).thenReturn(List.of(p1));

        // Act
        inventoryScheduler.checkLowStock();

        // Assert
        verify(productService, times(1)).getLowStockProducts(10);
    }
}
