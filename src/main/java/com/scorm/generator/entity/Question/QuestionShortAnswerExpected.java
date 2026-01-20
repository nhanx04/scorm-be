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
@Table(name = "question_short_answer_expected")
public class QuestionShortAnswerExpected {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expectedid")
    private Long expectedId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionid", referencedColumnName = "questionid")
    private Question question;

    @Column(name = "answer_text", nullable = false)
    private String answerText;

    @Column(name = "match_rule")
    private String matchRule;
}

