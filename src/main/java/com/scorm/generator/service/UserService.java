package com.scorm.generator.service;

import com.scorm.generator.dto.UpdateProfileRequest;
import com.scorm.generator.dto.UserProfileResponse;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return new UserProfileResponse(
                user.getUserId(),
                user.getFname(),
                null, // Truyền null vì Entity User không có trường minit
                user.getLname(),
                user.getEmail(),
                user.getAvatarUrl());
    }

    @Transactional // Thêm Transactional để đảm bảo tính toàn vẹn khi cập nhật DB
    public UserProfileResponse updateUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Cập nhật các trường được phép
        user.setFname(request.getFname());
        user.setLname(request.getLname());
        user.setAvatarUrl(request.getAvatarUrl());

        // Lưu xuống DB
        userRepository.save(user);

        return new UserProfileResponse(
                user.getUserId(),
                user.getFname(),
                null, // Truyền null vì Entity User không có trường minit
                user.getLname(),
                user.getEmail(),
                user.getAvatarUrl());
    }
}