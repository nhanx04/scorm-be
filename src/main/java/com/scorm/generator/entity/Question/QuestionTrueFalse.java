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
@Table(name = "question_true_false")
public class QuestionTrueFalse {

    @Id
    @Column(name = "questionid")
    private Long questionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "questionid")
    private Question question;

    @Column(name = "correct_value", nullable = false)
    private Boolean correctValue;
}

