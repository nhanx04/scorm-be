package com.scorm.generator.controller;

import com.scorm.generator.dto.UpdateProfileRequest;
import com.scorm.generator.dto.UserProfileResponse;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Lấy thông tin user đang đăng nhập
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        // authentication.getName() sẽ trả về email (subject) trích xuất từ JWT token
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserProfileResponse response = new UserProfileResponse(
                user.getUserId(),
                user.getFname(),
                null, // SỬA Ở ĐÂY: Truyền null vì Entity User không có trường minit
                user.getLname(),
                user.getEmail(),
                user.getAvatarUrl());

        return ResponseEntity.ok(response);
    }

    // Cập nhật thông tin user
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Cập nhật các trường được phép
        user.setFname(request.getFname());
        user.setLname(request.getLname());
        user.setAvatarUrl(request.getAvatarUrl());

        // Lưu xuống DB
        userRepository.save(user);

        UserProfileResponse response = new UserProfileResponse(
                user.getUserId(),
                user.getFname(),
                null, // SỬA Ở ĐÂY: Truyền null vì Entity User không có trường minit
                user.getLname(),
                user.getEmail(),
                user.getAvatarUrl());

        return ResponseEntity.ok(response);
    }
}