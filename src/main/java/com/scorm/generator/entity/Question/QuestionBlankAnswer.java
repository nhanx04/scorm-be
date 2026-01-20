package com.scorm.generator.entity.Question;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_blank_answer")
public class QuestionBlankAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answerid")
    private Long answerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blankid", referencedColumnName = "blankid")
    private QuestionBlank blank;

    @Column(name = "answer_text", nullable = false)
    private String answerText;

    @Column(name = "match_rule")
    private String matchRule;
}

