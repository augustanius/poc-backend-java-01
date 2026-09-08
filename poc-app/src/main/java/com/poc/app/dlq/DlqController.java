package com.poc.app.dlq;

import com.poc.common.context.RequirePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dlq")
// @RequirePermission("admin:dlq")
public class DlqController {

    private static final Logger log = LoggerFactory.getLogger(DlqController.class);

    private final DlqService dlqService;

    public DlqController(DlqService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping
    public ResponseEntity<List<DlqMessageEntity>> listAll(
            @RequestParam(required = false) String topic) {

        List<DlqMessageEntity> messages;
        if (topic != null && !topic.isBlank()) {
            messages = dlqService.listByTopic(topic);
        } else {
            messages = dlqService.listAll();
        }
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DlqMessageEntity> getById(@PathVariable String id) {
        DlqMessageEntity entity = dlqService.getById(id);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entity);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retry(@PathVariable String id) {
        boolean success = dlqService.retry(id);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Message retried successfully",
                    "id", id
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retry message",
                "id", id
        ));
    }

    @PostMapping("/retry-all")
    public ResponseEntity<Map<String, Object>> retryAll(
            @RequestParam(required = false) String topic) {

        int retried;
        int total;

        if (topic != null && !topic.isBlank()) {
            total = (int) dlqService.countByTopic(topic);
            retried = dlqService.retryByTopic(topic);
        } else {
            total = dlqService.listAll().size();
            retried = dlqService.retryAll();
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", total,
                "retried", retried,
                "message", "Retried " + retried + " out of " + total + " messages"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> remove(@PathVariable String id) {
        boolean success = dlqService.remove(id);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Message removed from DLQ",
                    "id", id
            ));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(dlqService.getStats());
    }
}
