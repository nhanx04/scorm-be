package com.scorm.generator.controller;

import com.scorm.generator.dto.UpdateProfileRequest;
import com.scorm.generator.dto.UserProfileResponse;
import com.scorm.generator.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // Inject UserService thay vì UserRepository
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Lấy thông tin user đang đăng nhập
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse response = userService.getCurrentUserProfile(email);
        return ResponseEntity.ok(response);
    }

    // Cập nhật thông tin user
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) { // Đã thêm @Valid ở đây

        String email = authentication.getName();
        UserProfileResponse response = userService.updateUserProfile(email, request);
        return ResponseEntity.ok(response);
    }
}