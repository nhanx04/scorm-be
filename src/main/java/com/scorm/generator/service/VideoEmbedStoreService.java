package com.scorm.generator.service;

import com.scorm.generator.entity.VideoEmbed;
import com.scorm.generator.repository.VideoEmbedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoEmbedStoreService {

    private final VideoEmbedRepository videoEmbedRepository;
    private final VideoEmbedService videoEmbedService;

    @Transactional
    public VideoEmbed create(String originalUrl, String title) {
        String embedUrl = videoEmbedService.normalizeEmbedUrl(originalUrl);

        VideoEmbed entity = VideoEmbed.builder()
                .originalUrl(originalUrl)
                .embedUrl(embedUrl)
                .title(title)
                .thumbnailUrl(extractYoutubeThumbnail(embedUrl))
                .build();

        return videoEmbedRepository.save(entity);
    }

    /**
     * Thumbnail cho YouTube: https://img.youtube.com/vi/<id>/hqdefault.jpg
     */
    private String extractYoutubeThumbnail(String embedUrl) {
        try {
            // Embed dạng: https://www.youtube.com/embed/<id>
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:youtube\\.com/\\s*embed/|youtu\\.be/)([^/?]+)")
                    .matcher(embedUrl);
            if (m.find()) {
                String id = m.group(1);
                return "https://img.youtube.com/vi/" + id + "/hqdefault.jpg";
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Transactional(readOnly = true)
    public VideoEmbed getById(Long id) {
        return videoEmbedRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video embed not found"));
    }

    @Transactional
    public void delete(Long id) {
        if (!videoEmbedRepository.existsById(id)) {
            throw new RuntimeException("Video embed not found");
        }
        videoEmbedRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public java.util.List<VideoEmbed> findAll() {
        return videoEmbedRepository.findAllByOrderByCreatedAtDesc();
    }
}
