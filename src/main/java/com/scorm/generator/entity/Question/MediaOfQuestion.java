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
@Table(name = "media_of_question")
public class MediaOfQuestion {

    @EmbeddedId
    private MediaOfQuestionId id;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class MediaOfQuestionId implements Serializable {
        @Column(name = "questionid")
        private Long questionId;

        @Column(name = "mediaid")
        private Long mediaId;
    }
}

