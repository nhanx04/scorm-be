package com.scorm.generator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class MembershipId implements Serializable {

    @Column(name = "orgid")
    private Long orgId;

    @Column(name = "userid")
    private Long userId;
}

