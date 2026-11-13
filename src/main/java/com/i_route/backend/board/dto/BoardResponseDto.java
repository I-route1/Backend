package com.i_route.backend.board.dto;

import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardResponseDto {

    private Long id;
    private String name;
    private String description;
    private BoardType boardType;
    private Long academyId;
    private String academyAddress;
    private Long managerUserId;
    private int postCount;

    public static BoardResponseDto from(Board board) {
        return BoardResponseDto.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .boardType(board.getBoardType())
                .academyId(board.getAcademy() != null ? board.getAcademy().getAcademyId() : null)
                .academyAddress(board.getAcademy() != null ? board.getAcademy().getAcademyAddress() : null)
                .managerUserId(
                        (board.getAcademy() != null && board.getAcademy().getUser() != null)
                                ? board.getAcademy().getUser().getId()
                                : null
                )
                .postCount(board.getPosts() == null ? 0 : board.getPosts().size())
                .build();
    }
}