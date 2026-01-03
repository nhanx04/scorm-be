package com.scorm.generator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "course")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "courseid") // Map với cột courseid (BIGSERIAL)
    private Long id;

    @Column(name = "title", length = 255)
    private String title;

    // NUMERIC(5,2) nên map sang BigDecimal để chính xác tuyệt đối
    @Column(name = "passing_score", precision = 5, scale = 2)
    private BigDecimal passingScore;

    @Column(name = "attempt_limit")
    private Integer attemptLimit;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "last_published_at")
    private LocalDateTime lastPublishedAt;

    // JSONB: Hibernate 6 có thể map thẳng vào String hoặc object.
    // Để an toàn nhất, ta map vào String và định nghĩa columnDefinition là jsonb
    @Column(name = "extra_infor", columnDefinition = "jsonb")
    private String extraInfo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Khóa ngoại trỏ về bảng users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_userid", nullable = false) // Map với cột course_userid
    private User user;

    // Tự động cập nhật ngày giờ
    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
        if (updatedAt == null)
            updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}