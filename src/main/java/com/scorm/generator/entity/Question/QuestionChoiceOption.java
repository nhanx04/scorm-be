package com.scorm.generator.entity.Question;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_choice_option")
public class QuestionChoiceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "optionid")
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionid", referencedColumnName = "questionid")
    private Question question;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "content_html")
    private String contentHtml;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "score_fraction", precision = 6, scale = 4)
    private BigDecimal scoreFraction;
}

