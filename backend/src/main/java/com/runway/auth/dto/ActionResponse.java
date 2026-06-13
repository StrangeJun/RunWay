package com.runway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActionResponse {
    private boolean accepted;

    public static ActionResponse accepted() {
        return new ActionResponse(true);
    }
}
