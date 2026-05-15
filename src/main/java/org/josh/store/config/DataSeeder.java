package org.josh.store.config;

import org.josh.store.model.Product;
import org.josh.store.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {

    @SuppressWarnings("null")
@Bean
    CommandLineRunner seedDatabase(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(Product.builder()
                        .name("Wireless Noise-Cancelling Headphones")
                        .description("Premium over-ear headphones with 30-hour battery life and active noise cancellation.")
                        .price(new BigDecimal("299.99"))
                        .stockQuantity(45)
                        .category("Electronics")
                        .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=200")
                        .build());

                repository.save(Product.builder()
                        .name("Mechanical Keyboard RGB")
                        .description("Compact TKL mechanical keyboard with Cherry MX switches and per-key RGB lighting.")
                        .price(new BigDecimal("129.99"))
                        .stockQuantity(78)
                        .category("Electronics")
                        .imageUrl("https://images.unsplash.com/photo-1541140532154-b024d705b90a?w=200")
                        .build());

                repository.save(Product.builder()
                        .name("Ergonomic Office Chair")
                        .description("Lumbar support office chair with adjustable armrests and breathable mesh back.")
                        .price(new BigDecimal("449.00"))
                        .stockQuantity(12)
                        .category("Furniture")
                        .imageUrl("https://images.unsplash.com/photo-1592078615290-033ee584e267?w=200")
                        .build());

                repository.save(Product.builder()
                        .name("Stainless Steel Water Bottle")
                        .description("Double-walled insulated bottle, keeps drinks cold 24h or hot 12h. 32oz capacity.")
                        .price(new BigDecimal("34.99"))
                        .stockQuantity(200)
                        .category("Lifestyle")
                        .imageUrl("https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=200")
                        .build());

                repository.save(Product.builder()
                        .name("4K Webcam with Ring Light")
                        .description("Ultra HD webcam with built-in ring light, noise-cancelling mic, and auto-focus.")
                        .price(new BigDecimal("89.99"))
                        .stockQuantity(7)
                        .category("Electronics")
                        .imageUrl("https://images.unsplash.com/photo-1587826080692-f439cd0b70da?w=200")
                        .build());

                repository.save(Product.builder()
                        .name("Minimalist Leather Wallet")
                        .description("Slim RFID-blocking bifold wallet crafted from full-grain Italian leather.")
                        .price(new BigDecimal("59.99"))
                        .stockQuantity(150)
                        .category("Accessories")
                        .imageUrl("https://images.unsplash.com/photo-1627123424574-724758594e93?w=200")
                        .build());

                repository.save(Product.builder()
                        .name("Yoga Mat Premium")
                        .description("6mm thick non-slip yoga mat with alignment lines, eco-friendly TPE material.")
                        .price(new BigDecimal("49.99"))
                        .stockQuantity(88)
                        .category("Sports")
                        .imageUrl("https://images.unsplash.com/photo-1601925228028-25cce21e3977?w=200")
                        .build());

                repository.save(Product.builder()
                        .name("Smart LED Desk Lamp")
                        .description("Touch-controlled desk lamp with USB-C charging port, 5 color temperatures.")
                        .price(new BigDecimal("44.99"))
                        .stockQuantity(3)
                        .category("Furniture")
                        .imageUrl("https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=200")
                        .build());

                System.out.println("✅ Sample products seeded successfully!");
            }
        };
    }
}
