package com.i_route.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class DuplicateCheckResponse {
    private boolean isDuplicate;
    private List<String> duplicates;
    private String message;
}
