package com.poc.user.infrastructure.inbound.web;

import com.poc.user.application.api.RegisterUserUseCase;
import com.poc.user.domain.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.RegisterResponse> register(@RequestBody AuthDto.RegisterRequest request) {
        User user = registerUserUseCase.register(request.username(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthDto.RegisterResponse(user.getId(), user.getUsername(), user.getEmail()));
    }
}
