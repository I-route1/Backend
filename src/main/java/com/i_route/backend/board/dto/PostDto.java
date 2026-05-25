package com.i_route.backend.board.dto;

import com.i_route.backend.board.entity.Post;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    // =========================
    // Request DTO (작성/수정)
    // =========================
    @Getter
    @Setter
    public static class Request {

        @NotBlank
        private String title;

        @NotBlank
        private String content;

        // 익명 여부만 클라이언트가 보냄
        private boolean anonymous;
    }

    // =========================
    // List Response DTO
    // =========================
    @Getter
    @Builder
    public static class ListResponse {

        private Long id;
        private String title;
        private String author;
        private boolean anonymous;
        private int viewCount;
        private int likeCount;
        private int commentCount;
        private LocalDateTime createdAt;

        public static ListResponse from(Post post) {

            String authorName = post.isAnonymous()
                    ? "익명"
                    : post.getAuthor().getNickname();

            return ListResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .author(authorName)
                    .anonymous(post.isAnonymous())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikes().size())
                    .commentCount(post.getComments().size())
                    .createdAt(post.getCreatedAt())
                    .build();
        }
    }

    // =========================
    // Detail Response DTO
    // =========================
    @Getter
    @Builder
    public static class DetailResponse {

        private Long id;
        private String title;
        private String content;
        private String author;
        private boolean anonymous;
        private int viewCount;
        private int likeCount;
        private boolean likedByMe;
        private boolean bookmarkedByMe;
        private List<CommentDto.Response> comments;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}