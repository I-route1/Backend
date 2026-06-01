package com.i_route.backend.board;

import com.i_route.backend.board.dto.BoardRequestDto;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.repository.BoardRepository;
import com.i_route.backend.board.service.BoardService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @InjectMocks
    private BoardService boardService;

    @Mock
    private BoardRepository boardRepository;

    @Test
    @DisplayName("게시판 전체 조회 - 성공")
    void getBoards_success() {
        Board board = new Board();
        board.setName("공지사항");
        board.setDescription("공지 게시판");

        given(boardRepository.findAll()).willReturn(List.of(board));

        List<BoardRequestDto.Response> result = boardService.getBoards();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("게시판 검색 - 키워드 포함된 결과 반환")
    void searchBoards_success() {
        Board board = new Board();
        board.setName("자유게시판");

        given(boardRepository.findByNameContainingIgnoreCase("자유")).willReturn(List.of(board));

        List<BoardRequestDto.Response> result = boardService.searchBoards("자유");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("자유게시판");
    }

    @Test
    @DisplayName("게시판 등록 - 성공")
    void createBoard_success() {
        BoardRequestDto.Request request = new BoardRequestDto.Request();
        request.setName("새 게시판");
        request.setDescription("설명");

        Board saved = new Board();
        saved.setName("새 게시판");
        saved.setDescription("설명");

        given(boardRepository.save(any(Board.class))).willReturn(saved);

        BoardRequestDto.Response result = boardService.createBoard(request);

        assertThat(result.getName()).isEqualTo("새 게시판");
    }

    @Test
    @DisplayName("게시판 수정 - 성공")
    void updateBoard_success() {
        Board board = new Board();
        board.setName("기존 이름");

        BoardRequestDto.Request request = new BoardRequestDto.Request();
        request.setName("수정된 이름");
        request.setDescription("수정된 설명");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        BoardRequestDto.Response result = boardService.updateBoard(1L, request);

        assertThat(result.getName()).isEqualTo("수정된 이름");
    }

    @Test
    @DisplayName("게시판 수정 - 존재하지 않는 ID로 예외 발생")
    void updateBoard_notFound() {
        given(boardRepository.findById(999L)).willReturn(Optional.empty());

        BoardRequestDto.Request request = new BoardRequestDto.Request();
        request.setName("이름");

        assertThatThrownBy(() -> boardService.updateBoard(999L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("게시판 삭제 - 성공")
    void deleteBoard_success() {
        willDoNothing().given(boardRepository).deleteById(1L);

        assertThatCode(() -> boardService.deleteBoard(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("게시판 상세 조회 - 성공")
    void getBoardDetail_success() {
        Board board = new Board();
        board.setName("공지사항");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        BoardRequestDto.Response result = boardService.getBoardDetail(1L);

        assertThat(result.getName()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("게시판 상세 조회 - 존재하지 않는 ID로 예외 발생")
    void getBoardDetail_notFound() {
        given(boardRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardDetail(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
