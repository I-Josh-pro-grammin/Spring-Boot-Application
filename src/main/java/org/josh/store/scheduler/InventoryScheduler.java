package org.josh.store.scheduler;

import org.josh.store.model.Product;
import org.josh.store.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

@Component
public class InventoryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryScheduler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProductService productService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledFuture;

    @Value("${app.scheduling.enabled:true}")
    private boolean enabled;

    @Value("${app.scheduling.inventory-check-cron:0 0 * * * *}")
    private String cronExpression;

    private final List<String> logs = new CopyOnWriteArrayList<>();

    public InventoryScheduler(ProductService productService, ThreadPoolTaskScheduler taskScheduler) {
        this.productService = productService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        addLog("Scheduler initialized. Default Enabled: " + enabled + ", Default Cron: " + cronExpression);
        reSchedule();
    }

    public synchronized void reSchedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
            addLog("Previous schedule cancelled.");
        }

        if (enabled && cronExpression != null && !cronExpression.isBlank()) {
            try {
                scheduledFuture = taskScheduler.schedule(this::checkLowStock, new CronTrigger(cronExpression));
                logger.info("Scheduled low stock check with cron expression: {}", cronExpression);
                addLog("Successfully scheduled check with cron: " + cronExpression);
            } catch (Exception e) {
                logger.error("Failed to schedule low stock check with cron: {}", cronExpression, e);
                addLog("ERROR: Failed to schedule check with cron: " + cronExpression + ". Reason: " + e.getMessage());
            }
        } else {
            logger.info("Inventory check scheduler is disabled.");
            addLog("Scheduler is currently inactive/disabled.");
        }
    }

    public void checkLowStock() {
        addLog("Manual/Scheduled trigger: Starting low stock check...");
        logger.info("Starting scheduled low stock check...");
        try {
            List<Product> lowStockProducts = productService.getLowStockProducts(10);
            if (lowStockProducts.isEmpty()) {
                logger.info("No low stock products found.");
                addLog("SUCCESS: Check finished. No low stock products found (threshold: 10).");
            } else {
                logger.warn("Found {} low stock products:", lowStockProducts.size());
                addLog("WARNING: Check finished. Found " + lowStockProducts.size() + " low stock products:");
                for (Product p : lowStockProducts) {
                    String itemMsg = String.format("- Product ID: %d | Name: %s | Stock: %d", p.getId(), p.getName(), p.getStockQuantity());
                    logger.warn("Product ID: {}, Name: {}, Stock: {}", p.getId(), p.getName(), p.getStockQuantity());
                    addLog(itemMsg);
                }
            }
        } catch (Exception e) {
            logger.error("Low stock check failed", e);
            addLog("ERROR: Low stock check failed. Reason: " + e.getMessage());
        }
    }

    public synchronized void updateConfig(boolean enabled, String cronExpression) {
        this.enabled = enabled;
        this.cronExpression = cronExpression;
        addLog("Configuration updated via API: enabled=" + enabled + ", cron=" + cronExpression);
        reSchedule();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public void clearLogs() {
        this.logs.clear();
        addLog("Logs cleared.");
    }

    private void addLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        logs.add("[" + timestamp + "] " + message);
    }
}
