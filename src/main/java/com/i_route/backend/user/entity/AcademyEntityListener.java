package com.i_route.backend.user.entity;

import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardType;
import com.i_route.backend.board.repository.BoardRepository;
import jakarta.persistence.PostPersist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AcademyEntityListener {

    private static BoardRepository boardRepository;

    @Autowired
    public void init(@Lazy BoardRepository boardRepository) {
        AcademyEntityListener.boardRepository = boardRepository;
    }

    @PostPersist
    public void onPostPersist(Academy academy) {
        Board board = Board.builder()
                .name(academy.getAcademyName() + " 게시판")
                .description(academy.getAcademyName() + " 전용 소식 및 소통 공간입니다.")
                .boardType(BoardType.ACADEMY)
                .academy(academy)
                .createdBy("SYSTEM")
                .build();

        boardRepository.save(board);
    }
}