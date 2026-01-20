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
@Table(name = "question_matching_left")
public class QuestionMatchingLeft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leftid")
    private Long leftId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionid", referencedColumnName = "questionid")
    private Question question;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "content_html")
    private String contentHtml;
}

