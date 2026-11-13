package com.i_route.backend.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardUpdateRequestDto {
    private String name;
    private String description;
    private String academyAddress;
}