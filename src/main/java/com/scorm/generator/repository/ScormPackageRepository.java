package com.scorm.generator.repository;

import com.scorm.generator.entity.ScormPackage;
import com.scorm.generator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScormPackageRepository extends JpaRepository<ScormPackage, Long> {
    List<ScormPackage> findByUser(User user);
    Optional<ScormPackage> findByIdAndUser(Long id, User user);
}

