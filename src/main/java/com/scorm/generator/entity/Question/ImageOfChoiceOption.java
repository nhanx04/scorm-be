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
@Table(name = "image_of_choice_option")
public class ImageOfChoiceOption {

    @EmbeddedId
    private ImageOfChoiceOptionId id;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class ImageOfChoiceOptionId implements Serializable {
        @Column(name = "optionid")
        private Long optionId;

        @Column(name = "mediaid")
        private Long mediaId;
    }
}

