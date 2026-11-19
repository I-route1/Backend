package com.i_route.backend.board.dto;

import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardType;
import com.i_route.backend.user.entity.User;
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
    private String managerNickname;
    private int postCount;
    private long bookmarkCount;

    public static BoardResponseDto from(Board board) {
        return from(board, 0);
    }

    public static BoardResponseDto from(Board board, long bookmarkCount) {
        User manager = board.getAcademy() == null ? null : board.getAcademy().getUser();

        return BoardResponseDto.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .boardType(board.getBoardType())
                .academyId(board.getAcademy() != null ? board.getAcademy().getAcademyId() : null)
                .academyAddress(board.getAcademy() != null ? board.getAcademy().getAcademyAddress() : null)
                .managerUserId(manager == null ? null : manager.getId())
                .managerNickname(manager == null ? null : manager.getNickname())
                .bookmarkCount(bookmarkCount)
                .postCount(board.getPosts() == null ? 0 : board.getPosts().size())
                .build();
    }
}