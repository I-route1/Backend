package com.i_route.backend.board.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {

    private Long id;
    private String author;
    private String content;
    private LocalDateTime createdAt;

    private long likeCount;
    private boolean likedByMe;
}