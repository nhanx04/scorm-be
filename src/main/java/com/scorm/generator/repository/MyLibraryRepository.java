package com.scorm.generator.repository;

import com.scorm.generator.entity.MyLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyLibraryRepository extends JpaRepository<MyLibrary, Long> {
}

