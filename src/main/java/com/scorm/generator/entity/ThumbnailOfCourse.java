package com.scorm.generator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "thumbnail_of_course")
public class ThumbnailOfCourse {

    @Id
    @Column(name = "thumbnail_courseid")
    private Long courseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "thumbnail_courseid")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thumbnail_mediaid", nullable = false)
    private ImageAsset imageAsset;
}

