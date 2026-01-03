package com.scorm.generator.controller;

import com.scorm.generator.dto.AuthRequest;
import com.scorm.generator.dto.AuthResponse;
import com.scorm.generator.dto.RegisterRequest;
import com.scorm.generator.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    // Sửa kiểu trả về thành ResponseEntity<?> để có thể trả về chuỗi lỗi
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // QUAN TRỌNG: In lỗi ra terminal để bạn biết chính xác là lỗi gì (DB, Code, hay
            // Data)
            e.printStackTrace();
            // Trả về thông báo lỗi cho Frontend hiển thị
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Đăng ký thất bại: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Đăng nhập thất bại: " + e.getMessage());
        }
    }
}