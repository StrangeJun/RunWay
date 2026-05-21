package com.runway.course.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class CourseFavoriteId implements Serializable {
    private UUID userId;
    private UUID courseId;
}
