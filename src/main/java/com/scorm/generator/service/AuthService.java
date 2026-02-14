package com.scorm.generator.service;

import com.scorm.generator.dto.AuthLoginRequest;
import com.scorm.generator.dto.AuthRegisterRequest;
import com.scorm.generator.dto.AuthResponse;
import com.scorm.generator.dto.AuthGoogleLoginRequest;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.UserRepository;
import com.scorm.generator.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // Dùng để gọi API Google
import java.util.Map;
import java.util.UUID;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(AuthRegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .fname(request.getFname())
                .lname(request.getLname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .user(toDto(user))
                .build();
    }

    public AuthResponse login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = (User) authentication.getPrincipal();
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .user(toDto(user))
                .build();
    }

    private AuthResponse.UserDto toDto(User user) {
        return AuthResponse.UserDto.builder()
                .userId(user.getUserId())
                .fname(user.getFname())
                .lname(user.getLname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public AuthResponse loginGoogle(AuthGoogleLoginRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> googleUser = null;

        // 1. CÁCH 1: Thử kiểm tra như Access Token
        try {
            String url = "https://www.googleapis.com/oauth2/v3/userinfo";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(request.getToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            googleUser = response.getBody();
        } catch (Exception e) {
            // Ignored
        }

        // 2. CÁCH 2: Thử kiểm tra như ID Token
        if (googleUser == null) {
            try {
                String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getToken();
                googleUser = restTemplate.getForObject(url, Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Google Token không hợp lệ hoặc đã hết hạn!");
            }
        }

        if (googleUser == null || googleUser.containsKey("error")) {
            throw new RuntimeException("Google Token không hợp lệ!");
        }

        // 3. Lấy thông tin user
        String email = (String) googleUser.get("email");
        String picture = (String) googleUser.get("picture");

        String firstName = (String) googleUser.get("given_name");
        String lastName = (String) googleUser.get("family_name");

        if (firstName == null) {
            String fullName = (String) googleUser.get("name");
            if (fullName != null) {
                String[] parts = fullName.split(" ", 2);
                firstName = parts[0];
                lastName = (parts.length > 1) ? parts[1] : "";
            } else {
                firstName = "Google";
                lastName = "User";
            }
        }

        final String fNameFinal = firstName;
        final String lNameFinal = lastName;

        // 4. Logic Find or Create
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFname(fNameFinal);
            newUser.setLname(lNameFinal != null ? lNameFinal : "");
            newUser.setAvatarUrl(picture);
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            // --- SỬA LỖI TẠI ĐÂY (setIsActive -> setActive) ---
            newUser.setActive(true);

            return userRepository.save(newUser);
        });

        // 5. Trả về kết quả
        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .user(toDto(user))
                .build();
    }
}