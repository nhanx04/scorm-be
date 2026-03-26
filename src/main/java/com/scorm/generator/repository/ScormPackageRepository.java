package com.scorm.generator.repository;

import com.scorm.generator.entity.ScormPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScormPackageRepository extends JpaRepository<ScormPackage, Long> {
    List<ScormPackage> findByUser_UserIdOrderByScormPackageIdDesc(Long userId);

    void deleteByCourse_CourseId(Long courseId);
}
