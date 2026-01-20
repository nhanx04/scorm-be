package com.scorm.generator.repository;

import com.scorm.generator.entity.QuizPage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizPageRepository extends JpaRepository<QuizPage, Long> {
}

