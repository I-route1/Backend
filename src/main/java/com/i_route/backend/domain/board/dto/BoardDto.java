package com.i_route.backend.domain.board.dto;

import com.i_route.backend.domain.board.entity.Board;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public class BoardDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank
        private String name;
        private String description;
    }

    @Getter @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String createdBy;
        private LocalDateTime createdAt;
        private int postCount;

        public static Response from(Board board) {
            return Response.builder()
                    .id(board.getId())
                    .name(board.getName())
                    .description(board.getDescription())
                    .createdBy(board.getCreatedBy() != null ? board.getCreatedBy().getNickname() : null)
                    .createdAt(board.getCreatedAt())
                    .postCount(board.getPosts() != null ? board.getPosts().size() : 0)
                    .build();
        }
    }
    @Getter @Builder
    public static class DetailResponse {
        private Long id;
        private String name;
        private String description;
        private String createdBy;
        private LocalDateTime createdAt;
        private boolean bookmarkedByMe;
        private Page<PostDto.ListResponse> posts;
    }


}
