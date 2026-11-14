package com.i_route.backend.board.config;

import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardType;
import com.i_route.backend.board.repository.BoardRepository;
import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.repository.AcademyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardDataInitializer implements CommandLineRunner {

    private final BoardRepository boardRepository;
    private final AcademyRepository academyRepository;

    @Override
    @Transactional
    public void run(String... args) {
        createDefaultBoardIfNotExists(
                BoardType.NOTICE,
                "전체 공지사항",
                "모든 사용자가 확인할 수 있는 공지사항 게시판입니다."
        );

        createDefaultBoardIfNotExists(
                BoardType.FREE,
                "자유게시판",
                "자유롭게 이야기를 나누는 게시판입니다."
        );

        createAcademyBoardsIfNotExists();
    }

    private void createDefaultBoardIfNotExists(
            BoardType boardType,
            String name,
            String description
    ) {
        if (boardRepository.existsByBoardType(boardType)) {
            return;
        }

        boardRepository.save(Board.builder()
                .name(name)
                .description(description)
                .boardType(boardType)
                .createdBy("시스템")
                .build());
    }

    private void createAcademyBoardsIfNotExists() {
        for (Academy academy : academyRepository.findAll()) {
            boolean exists = boardRepository
                    .existsByAcademy_AcademyIdAndBoardType(
                            academy.getAcademyId(),
                            BoardType.ACADEMY
                    );

            if (exists) {
                continue;
            }

            boardRepository.save(Board.builder()
                    .name(academy.getAcademyName() + " 게시판")
                    .description(academy.getAcademyName() + " 학원 게시판입니다.")
                    .boardType(BoardType.ACADEMY)
                    .createdBy("시스템")
                    .academy(academy)
                    .build());
        }
    }
}
