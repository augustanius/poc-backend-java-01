package com.poc.notification.interfaceadapters.inbound.rest;

import com.poc.common.context.RequirePermission;
import com.poc.common.domain.PageResult;
import com.poc.notification.contracts.usecases.CreateNotificationTemplateUseCase;
import com.poc.notification.contracts.usecases.GetNotificationTemplateUseCase;
import com.poc.notification.domain.model.NotificationTemplate;
import com.poc.notification.interfaceadapters.inbound.rest.dto.NotificationTemplateDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notification-templates")
public class NotificationTemplateController {

    private final CreateNotificationTemplateUseCase createTemplateUseCase;
    private final GetNotificationTemplateUseCase getTemplateUseCase;

    public NotificationTemplateController(CreateNotificationTemplateUseCase createTemplateUseCase,
                                          GetNotificationTemplateUseCase getTemplateUseCase) {
        this.createTemplateUseCase = createTemplateUseCase;
        this.getTemplateUseCase = getTemplateUseCase;
    }

    @PostMapping
    @RequirePermission("notification:write")
    public ResponseEntity<NotificationTemplateDto.TemplateResponse> create(
            @RequestBody NotificationTemplateDto.CreateTemplateRequest request) {

        NotificationTemplate template = new NotificationTemplate(
                UUID.randomUUID().toString(), request.name(),
                request.subject(), request.body(), request.channel());

        NotificationTemplate saved = createTemplateUseCase.create(template);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    @GetMapping("/{id}")
    @RequirePermission("notification:read")
    public ResponseEntity<NotificationTemplateDto.TemplateResponse> findById(@PathVariable String id) {
        return getTemplateUseCase.findById(id)
                .map(t -> ResponseEntity.ok(toResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @RequirePermission("notification:read")
    public ResponseEntity<PageResult<NotificationTemplateDto.TemplateResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);
        PageResult<NotificationTemplate> result = getTemplateUseCase.findAll(page, size);
        return ResponseEntity.ok(result.map(this::toResponse));
    }

    private NotificationTemplateDto.TemplateResponse toResponse(NotificationTemplate t) {
        return new NotificationTemplateDto.TemplateResponse(
                t.getId(), t.getName(), t.getSubject(), t.getBody(), t.getChannel());
    }
}
