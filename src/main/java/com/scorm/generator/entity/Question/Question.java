package com.scorm.generator.entity.Question;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "questionid")
    private Long questionId;

    @Column(name = "title")
    private String title;

    @Column(name = "instruction")
    private String instruction;

    @Column(name = "prompt_html")
    private String promptHtml;

    @Column(name = "text_html", columnDefinition = "TEXT")
    private String textHtml;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_override", columnDefinition = "jsonb")
    private JsonNode themeOverride;

    @Column(name = "layout_mode")
    private String layoutMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_meta", columnDefinition = "jsonb")
    private JsonNode layoutMeta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_data", columnDefinition = "jsonb")
    private JsonNode templateData;

    @Column(name = "points", precision = 8, scale = 2)
    private BigDecimal points;

    @Column(name = "shuffle_options")
    private Boolean shuffleOptions;

    @Column(name = "case_sensitive")
    private Boolean caseSensitive;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_config", columnDefinition = "jsonb")
    private JsonNode extraConfig;
}
