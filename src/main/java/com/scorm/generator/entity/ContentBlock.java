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
@Table(name = "content_block")
public class ContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blockid")
    private Long blockId;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "block_type")
    private String blockType;

    @Column(name = "text_html", columnDefinition = "TEXT")
    private String textHtml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_override", columnDefinition = "jsonb")
    private JsonNode themeOverride;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_meta", columnDefinition = "jsonb")
    private JsonNode layoutMeta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_pageid", referencedColumnName = "pageid", nullable = false)
    private ContentPage contentPage;
}
