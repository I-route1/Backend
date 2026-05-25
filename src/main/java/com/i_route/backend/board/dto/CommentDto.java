package com.i_route.backend.board.dto;

import com.i_route.backend.board.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class CommentDto {

    // =========================
    // Request
    // =========================
    @Getter
    @Setter
    public static class Request {
        @NotBlank
        private String content;
    }

    // =========================
    // Response
    // =========================
    @Getter
    @Builder
    public static class Response {

        private Long id;
        private String author;
        private String content;
        private int likeCount;
        private boolean likedByMe;
        private LocalDateTime createdAt;

        public static Response from(Comment comment, boolean likedByMe) {

            String authorName = comment.getAuthor().getNickname();

            return Response.builder()
                    .id(comment.getId())
                    .author(authorName)
                    .content(comment.getContent())
                    .likeCount(comment.getLikes().size()) // ✔ 좋아요 수
                    .likedByMe(likedByMe)                // ✔ 내가 눌렀는지
                    .createdAt(comment.getCreatedAt())
                    .build();
        }
    }
}