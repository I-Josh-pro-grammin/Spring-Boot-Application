package org.josh.store.controller;

import org.josh.store.scheduler.InventoryScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulingController {

    private final InventoryScheduler inventoryScheduler;

    public SchedulingController(InventoryScheduler inventoryScheduler) {
        this.inventoryScheduler = inventoryScheduler;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", inventoryScheduler.isEnabled());
        config.put("cronExpression", inventoryScheduler.getCronExpression());
        return ResponseEntity.ok(config);
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> request) {
        boolean enabled = (boolean) request.getOrDefault("enabled", inventoryScheduler.isEnabled());
        String cron = (String) request.getOrDefault("cronExpression", inventoryScheduler.getCronExpression());

        inventoryScheduler.updateConfig(enabled, cron);

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", inventoryScheduler.isEnabled());
        response.put("cronExpression", inventoryScheduler.getCronExpression());
        response.put("message", "Scheduler configuration updated successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerScheduler() {
        inventoryScheduler.checkLowStock();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Low stock check triggered successfully.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<String>> getLogs() {
        return ResponseEntity.ok(inventoryScheduler.getLogs());
    }

    @DeleteMapping("/logs")
    public ResponseEntity<Map<String, String>> clearLogs() {
        inventoryScheduler.clearLogs();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Scheduler logs cleared.");
        return ResponseEntity.ok(response);
    }
}
