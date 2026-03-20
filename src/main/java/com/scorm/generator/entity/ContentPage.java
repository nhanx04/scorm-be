package com.scorm.generator.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_page")
public class ContentPage {

    @Id
    @Column(name = "pageid")
    private Long pageId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "pageid")
    private Page page;

    @Column(name = "layout_mode")
    private String layoutMode;

    @Column(name = "layout_type")
    private String layoutType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_meta", columnDefinition = "jsonb")
    private JsonNode layoutMeta;
}
