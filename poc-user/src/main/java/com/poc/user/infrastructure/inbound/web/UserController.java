package com.poc.user.infrastructure.inbound.web;

import com.poc.common.domain.PageResult;
import com.poc.user.application.api.CreateUserUseCase;
import com.poc.user.application.api.GetUserUseCase;
import com.poc.user.domain.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase, GetUserUseCase getUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
    }

    @PostMapping
    public ResponseEntity<UserDto.UserResponse> create(@RequestBody UserDto.CreateUserRequest request) {
        User user = new User(UUID.randomUUID().toString(), request.username(), request.email());
        User saved = createUserUseCase.create(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserDto.UserResponse(saved.getId(), saved.getUsername(), saved.getEmail()));
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
