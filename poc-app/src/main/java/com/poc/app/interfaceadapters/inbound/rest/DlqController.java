package com.poc.app.interfaceadapters.inbound.rest;

import com.poc.app.contracts.usecases.ManageDlqUseCase;
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

    private final ManageDlqUseCase manageDlqUseCase;

    public DlqController(ManageDlqUseCase manageDlqUseCase) {
        this.manageDlqUseCase = manageDlqUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ManageDlqUseCase.DlqMessage>> listAll(
            @RequestParam(required = false) String topic) {

        List<ManageDlqUseCase.DlqMessage> messages;
        if (topic != null && !topic.isBlank()) {
            messages = manageDlqUseCase.listByTopic(topic);
        } else {
            messages = manageDlqUseCase.listAll();
        }
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManageDlqUseCase.DlqMessage> getById(@PathVariable String id) {
        ManageDlqUseCase.DlqMessage message = manageDlqUseCase.getById(id);
        if (message == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(message);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retry(@PathVariable String id) {
        boolean success = manageDlqUseCase.retry(id);
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
            total = (int) manageDlqUseCase.countByTopic(topic);
            retried = manageDlqUseCase.retryByTopic(topic);
        } else {
            total = manageDlqUseCase.listAll().size();
            retried = manageDlqUseCase.retryAll();
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
        boolean success = manageDlqUseCase.remove(id);
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
        return ResponseEntity.ok(manageDlqUseCase.getStats());
    }
}
