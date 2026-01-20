package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.ImageOfChoiceOption;
import com.scorm.generator.entity.Question.ImageOfChoiceOption.ImageOfChoiceOptionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageOfChoiceOptionRepository extends JpaRepository<ImageOfChoiceOption, ImageOfChoiceOptionId> {
    List<ImageOfChoiceOption> findById_OptionId(Long optionId);

    List<ImageOfChoiceOption> findById_OptionIdIn(List<Long> optionIds);
}

