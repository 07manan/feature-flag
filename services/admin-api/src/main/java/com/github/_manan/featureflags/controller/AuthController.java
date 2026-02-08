package com.github._manan.featureflags.controller;

import com.github._manan.featureflags.dto.AuthResponse;
import com.github._manan.featureflags.dto.LoginRequest;
import com.github._manan.featureflags.dto.OAuthRequest;
import com.github._manan.featureflags.dto.RegisterRequest;
import com.github._manan.featureflags.entity.AuthProvider;
import com.github._manan.featureflags.service.AuthService;
import com.github._manan.featureflags.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth2/{provider}")
    public ResponseEntity<AuthResponse> oauth2Login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthRequest request) {
        AuthProvider authProvider = parseProvider(provider);
        AuthResponse response = oAuthService.authenticate(authProvider, request.getToken());
        return ResponseEntity.ok(response);
    }

    private AuthProvider parseProvider(String provider) {
        try {
            return AuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
    }
}
