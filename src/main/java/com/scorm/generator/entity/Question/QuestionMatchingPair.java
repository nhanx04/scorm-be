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
@Table(name = "question_matching_pair")
public class QuestionMatchingPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pairid")
    private Long pairId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionid", referencedColumnName = "questionid")
    private Question question;

    @Column(name = "leftid", nullable = false)
    private Long leftId;

    @Column(name = "rightid", nullable = false)
    private Long rightId;
}

