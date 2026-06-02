package com.i_route.backend.board.dto;

import com.i_route.backend.board.entity.Board;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardResponseDto {
    private Long id;
    private String name;
    private String description;
    private int postCount;

    public static BoardResponseDto from(Board board) {
        return BoardResponseDto.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .postCount(board.getPost() == null ? 0 : board.getPost().size())
                .build();
    }
}