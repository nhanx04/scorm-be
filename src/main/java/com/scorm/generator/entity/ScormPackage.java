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
@Table(name = "scorm_package")
public class ScormPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scorm_packageid")
    private Long scormPackageId;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "package_type")
    private String packageType;

    @Column(name = "zip_file_path")
    private String zipFilePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_snapshot", columnDefinition = "jsonb")
    private JsonNode themeSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_courseid", referencedColumnName = "courseid")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_configid", referencedColumnName = "scorm_configid")
    private ScormExportConfig config;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_userid", referencedColumnName = "userid")
    private User user;
}

