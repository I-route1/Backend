package com.i_route.backend.domain.board.dto;

import com.i_route.backend.domain.board.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class CommentDto {

    @Getter @Setter
    public static class Request {
        @NotBlank private String content;
    }

    @Getter @Builder
    public static class Response {
        private Long id;
        private String author;
        private String content;
        private LocalDateTime createdAt;

        public static Response from(Comment comment) {
            return Response.builder()
                    .id(comment.getId())
                    .author(comment.getAuthor().getNickname())
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .build();
        }
    }
}
