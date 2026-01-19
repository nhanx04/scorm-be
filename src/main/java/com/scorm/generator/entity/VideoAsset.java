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
@Table(name = "video_asset")
public class VideoAsset {

    @Id
    @Column(name = "mediaid")
    private Long mediaId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mediaid", referencedColumnName = "mediaid")
    private MediaAsset mediaAsset;

    @Column(name = "youtube_url", nullable = false)
    private String youtubeUrl;
}

