package com.scorm.generator.service;

import com.scorm.generator.dto.AuthLoginRequest;
import com.scorm.generator.dto.AuthRegisterRequest;
import com.scorm.generator.dto.AuthResponse;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.UserRepository;
import com.scorm.generator.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // --- REGISTER ---
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

    // --- LOGIN THƯỜNG ---
    public AuthResponse login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = (User) authentication.getPrincipal();

        // Cập nhật thời gian login gần nhất
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .user(toDto(user))
                .build();
    }

    // --- LOGIN GOOGLE (Đã chỉnh sửa) ---
    public AuthResponse loginGoogle(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> googleUser;

        try {
            // 1. Gọi Google API lấy thông tin User bằng Access Token
            String url = "https://www.googleapis.com/oauth2/v3/userinfo";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>("", headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            googleUser = response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Token Google không hợp lệ hoặc đã hết hạn.");
        }

        if (googleUser == null || !googleUser.containsKey("email")) {
            throw new RuntimeException("Không thể lấy email từ Google Token.");
        }

        // 2. Trích xuất thông tin
        String email = (String) googleUser.get("email");
        String picture = (String) googleUser.get("picture");
        String firstName = (String) googleUser.get("given_name");
        String lastName = (String) googleUser.get("family_name");

        // Xử lý tên nếu bị thiếu
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

        // 3. Tìm hoặc Tạo mới User (Find or Create)
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFname(fNameFinal);
            newUser.setLname(lNameFinal != null ? lNameFinal : "");
            newUser.setAvatarUrl(picture);

            // Set password ngẫu nhiên
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setActive(true);

            return userRepository.save(newUser);
        });

        // ---------------------------------------------------------
        // [FIX] CẬP NHẬT LAST LOGIN VÀ THÔNG TIN MỚI NHẤT
        // ---------------------------------------------------------
        user.setLastLoginAt(OffsetDateTime.now());

        // Cập nhật lại avatar để luôn đồng bộ với Google
        if (picture != null) {
            user.setAvatarUrl(picture);
        }

        // Lưu lại thay đổi vào DB
        userRepository.save(user);
        // ---------------------------------------------------------

        // 4. Tạo JWT Token của hệ thống
        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .user(toDto(user))
                .build();
    }

    // Helper Method
    private AuthResponse.UserDto toDto(User user) {
        return AuthResponse.UserDto.builder()
                .userId(user.getUserId())
                .fname(user.getFname())
                .lname(user.getLname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}