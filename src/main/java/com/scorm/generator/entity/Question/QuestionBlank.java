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
@Table(name = "question_blank")
public class QuestionBlank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blankid")
    private Long blankId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionid", referencedColumnName = "questionid")
    private Question question;

    @Column(name = "blank_key")
    private String blankKey;

    @Column(name = "order_index")
    private Integer orderIndex;
}

