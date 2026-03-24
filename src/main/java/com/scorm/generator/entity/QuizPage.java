package com.scorm.generator.entity;

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
@Table(name = "quiz_page")
public class QuizPage {

    @Id
    @Column(name = "pageid")
    private Long pageId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "pageid")
    private Page page;

    @Column(name = "passing_score", precision = 5, scale = 2)
    private BigDecimal passingScore;

    @Column(name = "attempt_allowed")
    private Integer attemptAllowed;

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
}
