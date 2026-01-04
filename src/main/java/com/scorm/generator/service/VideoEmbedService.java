package com.scorm.generator.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class VideoEmbedService {

    private static final List<String> ALLOWED_HOSTS = Arrays.asList(
            "youtube.com",
            "www.youtube.com",
            "youtu.be",
            "player.vimeo.com",
            "vimeo.com"
    );

    /**
     * Validate + normalize link video sang dạng iframe-safe embed URL.
     *
     * Hỗ trợ:
     * - YouTube: watch?v=, youtu.be/, /embed/
     * - Vimeo: vimeo.com/{id}, player.vimeo.com/video/{id}
     *
     * Trả về null nếu input null/blank.
     */
    public String normalizeEmbedUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.isBlank()) {
            return null;
        }

        String trimmed = inputUrl.trim();

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid video url");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Video url must be http/https");
        }

        String host = uri.getHost();
        if (host == null || ALLOWED_HOSTS.stream().noneMatch(h -> h.equalsIgnoreCase(host))) {
            throw new IllegalArgumentException("Unsupported video host");
        }

        // YouTube
        if (host.equalsIgnoreCase("youtu.be")) {
            // https://youtu.be/{id}
            String path = uri.getPath();
            String id = (path != null && path.length() > 1) ? path.substring(1) : null;
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Invalid YouTube url");
            }
            return "https://www.youtube.com/embed/" + stripUnsafe(id);
        }

        if (host.equalsIgnoreCase("youtube.com") || host.equalsIgnoreCase("www.youtube.com")) {
            String path = uri.getPath() != null ? uri.getPath() : "";

            // already embed
            if (path.startsWith("/embed/")) {
                String id = path.substring("/embed/".length());
                if (id.isBlank()) throw new IllegalArgumentException("Invalid YouTube embed url");
                return "https://www.youtube.com/embed/" + stripUnsafe(id);
            }

            // watch?v=
            if (path.equals("/watch")) {
                String v = getQueryParam(uri.getRawQuery(), "v");
                if (v == null || v.isBlank()) {
                    throw new IllegalArgumentException("Invalid YouTube watch url (missing v)");
                }
                return "https://www.youtube.com/embed/" + stripUnsafe(v);
            }

            // fallback: try last segment
            String last = lastPathSegment(path);
            if (last != null && !last.isBlank()) {
                return "https://www.youtube.com/embed/" + stripUnsafe(last);
            }

            throw new IllegalArgumentException("Invalid YouTube url");
        }

        // Vimeo
        if (host.equalsIgnoreCase("player.vimeo.com")) {
            // https://player.vimeo.com/video/{id}
            String path = uri.getPath() != null ? uri.getPath() : "";
            if (!path.startsWith("/video/")) {
                throw new IllegalArgumentException("Invalid Vimeo embed url");
            }
            String id = path.substring("/video/".length());
            if (id.isBlank()) throw new IllegalArgumentException("Invalid Vimeo embed url");
            return "https://player.vimeo.com/video/" + stripUnsafe(id);
        }

        if (host.equalsIgnoreCase("vimeo.com")) {
            // https://vimeo.com/{id}
            String id = lastPathSegment(uri.getPath());
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Invalid Vimeo url");
            }
            return "https://player.vimeo.com/video/" + stripUnsafe(id);
        }

        // should never reach here due to allowed hosts
        throw new IllegalArgumentException("Unsupported video host");
    }

    private String getQueryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isBlank()) return null;
        String[] parts = rawQuery.split("&");
        for (String p : parts) {
            int idx = p.indexOf('=');
            if (idx <= 0) continue;
            String k = URLDecoder.decode(p.substring(0, idx), StandardCharsets.UTF_8);
            if (!k.equals(key)) continue;
            return URLDecoder.decode(p.substring(idx + 1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private String lastPathSegment(String path) {
        if (path == null || path.isBlank()) return null;
        String p = path;
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        int idx = p.lastIndexOf('/');
        return idx >= 0 ? p.substring(idx + 1) : p;
    }

    /**
     * Very small hardening: keep only common safe chars for IDs.
     */
    private String stripUnsafe(String s) {
        return s.replaceAll("[^a-zA-Z0-9_-]", "");
    }
}

