package com.devhunt.controller;

import com.devhunt.dto.AuthRequest;
import com.devhunt.dto.AuthResponse;
import com.devhunt.model.User;
import com.devhunt.repository.UserRepository;
import com.devhunt.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody AuthRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

        String jwt = jwtUtils.generateJwtToken(loginRequest.email());
        return ResponseEntity.ok(new AuthResponse(jwt, loginRequest.email()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.email())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = User.builder()
                .email(signUpRequest.email())
                .password(encoder.encode(signUpRequest.password()))
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }
}