package com.i_route.backend.board.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostResponseDto {

    private Long id;
    private Long boardId;
    private String category;

    private String title;
    private String author;
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private int viewCount;

    private long likeCount;
    private long commentCount;

    private boolean likedByMe;
    private boolean bookmarked;

    private boolean pinned;
}