package com.scorm.generator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "scorm_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScormPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "welcome_video_url")
    private String welcomeVideoUrl;

    @Column(name = "theme_json", columnDefinition = "TEXT")
    private String themeJson;

    @Column(name = "passing_score")
    private Integer passingScore;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "package_url")
    private String packageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_mode")
    private com.scorm.generator.model.ReviewMode reviewMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "scormPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (passingScore == null) {
            passingScore = 70;
        }
        if (maxAttempts == null) {
            maxAttempts = 3;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
