package com.scorm.generator.service;

import com.scorm.generator.dto.AuthRequest;
import com.scorm.generator.dto.AuthResponse;
import com.scorm.generator.dto.RegisterRequest;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.UserRepository;
import com.scorm.generator.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Set the full name directly to match the database schema
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName()) // Set the full name
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String token = tokenProvider.generateTokenFromUserId(user.getId());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        // Cập nhật thời gian login
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }
}