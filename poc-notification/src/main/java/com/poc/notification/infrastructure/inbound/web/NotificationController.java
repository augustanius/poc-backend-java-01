package com.poc.notification.infrastructure.inbound.web;

import com.poc.common.context.AuthenticatedUserId;
import com.poc.common.context.RequirePermission;
import com.poc.common.context.UserContext;
import com.poc.common.domain.PageResult;
import com.poc.notification.application.api.GetNotificationUseCase;
import com.poc.notification.application.api.SendNotificationUseCase;
import com.poc.notification.domain.model.Notification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final GetNotificationUseCase getNotificationUseCase;

    public NotificationController(SendNotificationUseCase sendNotificationUseCase,
                                  GetNotificationUseCase getNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.getNotificationUseCase = getNotificationUseCase;
    }

    @PostMapping
    @RequirePermission("notification:write")
    public ResponseEntity<NotificationDto.NotificationResponse> send(
            @RequestBody NotificationDto.SendNotificationRequest request,
            @AuthenticatedUserId String userId) {

        var notification = sendNotificationUseCase.send(
                userId, request.templateId(), request.subject(), request.body(), request.channel());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(notification));
    }

    @GetMapping("/{id}")
    @RequirePermission("notification:read")
    public ResponseEntity<NotificationDto.NotificationResponse> findById(@PathVariable String id) {
        return getNotificationUseCase.findById(id)
                .map(n -> ResponseEntity.ok(toResponse(n)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @RequirePermission("notification:read")
    public ResponseEntity<PageResult<NotificationDto.NotificationResponse>> findAll(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        PageResult<Notification> result;
        if (userId != null) {
            result = getNotificationUseCase.findByUserId(userId, page, size);
        } else if (!UserContext.hasRole("ADMIN")) {
            result = getNotificationUseCase.findByUserId(UserContext.getUserId(), page, size);
        } else {
            result = getNotificationUseCase.findAll(page, size);
        }

        return ResponseEntity.ok(result.map(this::toResponse));
    }

    private NotificationDto.NotificationResponse toResponse(Notification n) {
        return new NotificationDto.NotificationResponse(
                n.getId(), n.getUserId(), n.getTemplateId(),
                n.getSubject(), n.getBody(), n.getChannel(),
                n.getStatus().name(), n.getCreatedAt(), n.getSentAt());
    }
}
