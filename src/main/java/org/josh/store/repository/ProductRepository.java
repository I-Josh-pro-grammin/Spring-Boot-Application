package org.josh.store.repository;

import org.josh.store.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Search by name or description (case-insensitive)
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchProducts(@Param("query") String query, Pageable pageable);

    // Filter by category
    List<Product> findByCategoryIgnoreCase(String category, Pageable page);


    // Find low stock products (below threshold)
    List<Product> findByStockQuantityLessThan(Integer threshold);

    // Distinct categories for filter dropdown
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllCategories();

    // ─── Advanced JPQL / Native SQL ──────────────────────────────────────────

    // 1. JPQL: Price range filtering
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice ORDER BY p.price ASC")
    List<Product> findByPriceRange(@Param("minPrice") java.math.BigDecimal minPrice, @Param("maxPrice") java.math.BigDecimal maxPrice);

    // 2. Native SQL: Total inventory value for a category
    @Query(value = "SELECT COALESCE(SUM(price * stock_quantity), 0) FROM products WHERE LOWER(category) = LOWER(:category)", nativeQuery = true)
    java.math.BigDecimal getInventoryValueByCategory(@Param("category") String category);

    // 3. JPQL Modifying: Bulk price update
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Product p SET p.price = p.price * :factor WHERE p.category = :category")
    int updatePricesByCategory(@Param("category") String category, @Param("factor") java.math.BigDecimal factor);
}
