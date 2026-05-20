package com.runway.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AchievementsResponse {

    private List<AchievementItemResponse> items;
}
