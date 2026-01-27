package com.github._manan.featureflags.service;

import com.github._manan.featureflags.dto.AuthResponse;
import com.github._manan.featureflags.dto.LoginRequest;
import com.github._manan.featureflags.dto.RegisterRequest;
import com.github._manan.featureflags.entity.Role;
import com.github._manan.featureflags.entity.User;
import com.github._manan.featureflags.repository.UserRepository;
import com.github._manan.featureflags.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // First user becomes ADMIN, all others are GUEST
        boolean isFirstUser = userRepository.count() == 0;
        Role assignedRole = isFirstUser ? Role.ADMIN : Role.GUEST;

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(assignedRole)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        return AuthResponse.of(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String token = jwtUtil.generateToken(user);

        return AuthResponse.of(token, user);
    }
}
