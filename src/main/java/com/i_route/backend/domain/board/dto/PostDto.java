package com.i_route.backend.domain.board.dto;

import com.i_route.backend.domain.board.entity.Post;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    @Getter @Setter
    public static class Request {
        @NotBlank private String title;
        @NotBlank private String content;
    }

    @Getter @Builder
    public static class ListResponse {
        private Long id;
        private String title;
        private String author;
        private int viewCount;
        private int likeCount;
        private int commentCount;
        private LocalDateTime createdAt;

        public static ListResponse from(Post post) {
            return ListResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .author(post.getAuthor().getNickname())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikes().size())
                    .commentCount(post.getComments().size())
                    .createdAt(post.getCreatedAt())
                    .build();
        }
    }

    @Getter @Builder
    public static class DetailResponse {
        private Long id;
        private String title;
        private String content;
        private String author;
        private int viewCount;
        private int likeCount;
        private boolean likedByMe;
        private boolean bookmarkedByMe;
        private List<CommentDto.Response> comments;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
