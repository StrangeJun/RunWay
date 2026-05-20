package com.runway.run.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PersonalRecordsResponse {

    private PersonalRecordItemResponse longestRun;
    private PersonalRecordItemResponse fastestPace;
    private PersonalRecordItemResponse mostCalories;
    private PersonalRecordItemResponse best5k;
    private PersonalRecordItemResponse best10k;
    private long totalCompletedRuns;
    private double totalDistanceMeters;
}
