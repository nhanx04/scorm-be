package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveVideoEmbedRequest {
    /**
     * Link video (có thể là watch url/share url). Server sẽ chuẩn hoá về embed url.
     */
    private String url;

    private String title;
}
