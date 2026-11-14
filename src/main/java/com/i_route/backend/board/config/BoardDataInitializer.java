package com.i_route.backend.board.config;

import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardType;
import com.i_route.backend.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardDataInitializer implements CommandLineRunner {

    private final BoardRepository boardRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ensureBoard(
                BoardType.NOTICE,
                "전체 공지사항",
                "모든 사용자가 확인할 수 있는 공지사항 게시판입니다."
        );

        ensureBoard(
                BoardType.FREE,
                "자유게시판",
                "자유롭게 이야기를 나누는 게시판입니다."
        );
    }

    private void ensureBoard(
            BoardType boardType,
            String name,
            String description
    ) {
        List<Board> existingBoards =
                boardRepository.findAllByBoardTypeOrderByIdAsc(boardType);

        if (existingBoards.isEmpty()) {
            boardRepository.save(Board.builder()
                    .name(name)
                    .description(description)
                    .boardType(boardType)
                    .createdBy("시스템")
                    .build());
            return;
        }

        // 이미 있는 게시판의 이름과 설명을 원하는 값으로 갱신
        existingBoards.get(0).updateInfo(name, description);
    }
}
