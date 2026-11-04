package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@NoArgsConstructor
public class FeedbackRequest {
    @NotNull @Size(max = 1000)
    private String instructorFeedback;
}
