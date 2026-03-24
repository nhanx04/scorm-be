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
@Table(name = "page")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pageid")
    private Long pageId;

    @Column(name = "title")
    private String title;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "page_type")
    private String pageType;

    @Column(name = "text_html", columnDefinition = "TEXT")
    private String textHtml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_override", columnDefinition = "jsonb")
    private JsonNode themeOverride;

    @Column(name = "layout_mode")
    private String layoutMode;

    @Column(name = "layout_type")
    private String layoutType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_meta", columnDefinition = "jsonb")
    private JsonNode layoutMeta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sectionid", referencedColumnName = "sectionid")
    private Section section;

    @OneToOne(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ContentPage contentPage;

    @OneToOne(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private QuizPage quizPage;
}
