package com.scorm.generator.repository;

import com.scorm.generator.entity.MyLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MyLibraryRepository extends JpaRepository<MyLibrary, Long> {
    List<MyLibrary> findByOwner_UserId(Long userId);
}
