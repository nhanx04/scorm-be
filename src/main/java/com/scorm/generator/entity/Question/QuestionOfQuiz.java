package com.scorm.generator.entity.Question;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_of_quiz")
public class QuestionOfQuiz {

    @EmbeddedId
    private QuestionOfQuizId id;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class QuestionOfQuizId implements Serializable {
        @Column(name = "quiz_questionid")
        private Long quizQuestionId;

        @Column(name = "quiz_pageid")
        private Long quizPageId;
    }
}

