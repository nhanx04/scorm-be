package com.scorm.generator.service;

import com.scorm.generator.dto.AuthLoginRequest;
import com.scorm.generator.dto.AuthRegisterRequest;
import com.scorm.generator.dto.AuthResponse;
import com.scorm.generator.dto.AuthGoogleLoginRequest;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.UserRepository;
import com.scorm.generator.security.JwtService;
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
        // 1. Gọi API của Google để kiểm tra xem Token gửi lên có hợp lệ không
        // (Đây là cách nhanh nhất, không cần cài thêm thư viện Google API Client)
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getToken();
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> googleUser;
        try {
            googleUser = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Google Token không hợp lệ hoặc đã hết hạn!");
        }

        if (googleUser == null || googleUser.containsKey("error")) {
            throw new RuntimeException("Google Token không hợp lệ!");
        }

        // 2. Lấy thông tin quan trọng từ Google
        String email = (String) googleUser.get("email");
        String firstName = (String) googleUser.get("given_name");
        String lastName = (String) googleUser.get("family_name");
        String picture = (String) googleUser.get("picture");

        // 3. Logic "Find or Create" (Tìm hoặc Tạo mới)
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // Nếu chưa có email này trong DB -> Tạo user mới
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFname(firstName != null ? firstName : "Google");
            newUser.setLname(lastName != null ? lastName : "User");
            newUser.setAvatarUrl(picture);

            // Set mật khẩu ngẫu nhiên (vì họ đăng nhập bằng Google nên không cần pass)
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            // Set các giá trị mặc định khác nếu cần (ví dụ: role, active...)
            // newUser.setRole(Role.USER);

            return userRepository.save(newUser);
        });

        // 4. Sinh JWT Token của hệ thống mình (để frontend dùng cho các request sau)
        String jwtToken = jwtService.generateToken(user);

        // 5. Trả về kết quả (Lưu ý: Constructor AuthResponse có thể khác tùy code cũ
        // của bạn)
        // Nếu bạn dùng @Builder:
        return AuthResponse.builder()
                .token(jwtToken)
                // .user(user) // Nếu response có trả về cả user info
                .build();

        // Hoặc nếu dùng Constructor thường:
        // return new AuthResponse(jwtToken);
    }
}
