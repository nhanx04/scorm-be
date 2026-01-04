package com.scorm.generator.repository;

import com.scorm.generator.entity.VideoEmbed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoEmbedRepository extends JpaRepository<VideoEmbed, Long> {
    java.util.List<VideoEmbed> findAllByOrderByCreatedAtDesc();
}
