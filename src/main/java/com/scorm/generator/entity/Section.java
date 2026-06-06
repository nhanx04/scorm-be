package com.scorm.generator.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "section")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sectionid")
    private Long sectionId;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "learning_objective", columnDefinition = "TEXT")
    private String learningObjective;

    @Column(name = "text_html", columnDefinition = "TEXT")
    private String textHtml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_override", columnDefinition = "jsonb")
    private JsonNode themeOverride;

    @Column(name = "layout_mode")
    private String layoutMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_meta", columnDefinition = "jsonb")
    private JsonNode layoutMeta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_courseid", referencedColumnName = "courseid")
    private Course course;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Page> pages;
}
