package com.i_route.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class ReviewPaperDto {
    private Long paperId;
    private List<String> questions;
    private List<String> weakConcepts;
}
