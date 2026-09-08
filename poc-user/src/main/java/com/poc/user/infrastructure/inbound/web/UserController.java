package com.poc.user.infrastructure.inbound.web;

import com.poc.common.context.RequirePermission;
import com.poc.common.domain.PageResult;
import com.poc.user.application.api.GetUserUseCase;
import com.poc.user.domain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequirePermission("user:read")
public class UserController {

    private final GetUserUseCase getUserUseCase;

    public UserController(GetUserUseCase getUserUseCase) {
        this.getUserUseCase = getUserUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto.UserResponse> findById(@PathVariable String id) {
        return getUserUseCase.findById(id)
                .map(u -> ResponseEntity.ok(new UserDto.UserResponse(u.getId(), u.getUsername(), u.getEmail())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<PageResult<UserDto.UserResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);
        PageResult<User> result = getUserUseCase.findAll(page, size);
        return ResponseEntity.ok(result.map(u -> new UserDto.UserResponse(u.getId(), u.getUsername(), u.getEmail())));
    }
}
