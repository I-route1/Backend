package com.i_route.backend.board.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequestDto {
    private String content;
    private String author;
}