package com.poc.user.interfaceadapters.inbound.rest;

import com.poc.user.contracts.usecases.RegisterUserUseCase;
import com.poc.user.domain.model.User;
import com.poc.user.interfaceadapters.inbound.rest.dto.AuthDto;
import jakarta.validation.Valid;
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
    public ResponseEntity<AuthDto.RegisterResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        User user = registerUserUseCase.register(request.username(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthDto.RegisterResponse(user.getId(), user.getUsername(), user.getEmail()));
    }
}
