package com.i_route.backend.board.config;

import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardType;
import com.i_route.backend.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardDataInitializer implements CommandLineRunner {

    private final BoardRepository boardRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (boardRepository.count() > 0) {
            return;
        }

        Board defaultBoard = Board.builder()
                .name("전체 공지사항")
                .description("아이루트 전체 공지사항 게시판입니다.")
                .boardType(BoardType.NOTICE)
                .createdBy("시스템")
                .build();

        boardRepository.save(defaultBoard);
    }
}