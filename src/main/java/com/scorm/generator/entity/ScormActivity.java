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
@Table(name = "scorm_activity")
public class ScormActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activityid")
    private Long activityId;

    @Column(name = "title")
    private String title;

    @Column(name = "activity_type")
    private String activityType;

    @Column(name = "order_index")
    private Integer orderIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_config", columnDefinition = "jsonb")
    private JsonNode extraConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_activityid", referencedColumnName = "activityid")
    private ScormActivity parentActivity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_packageid", referencedColumnName = "scorm_packageid")
    private ScormPackage scormPackage;
}

