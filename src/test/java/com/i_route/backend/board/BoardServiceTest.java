package com.i_route.backend.board;

import com.i_route.backend.board.dto.BoardDto;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardBookmark;
import com.i_route.backend.board.repository.BoardBookmarkRepository;
import com.i_route.backend.board.repository.BoardRepository;
import com.i_route.backend.board.repository.PostRepository;
import com.i_route.backend.board.service.BoardService;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @InjectMocks
    private BoardService boardService;

    @Mock private BoardRepository boardRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private BoardBookmarkRepository boardBookmarkRepository;

    private User user(Long id, String nickname) {
        return User.builder()
                .id(id).nickname(nickname)
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL)
                .build();
    }

    private Board board(Long id, String name, User createdBy) {
        Board b = new Board();
        b.setId(id);
        b.setName(name);
        b.setCreatedBy(createdBy);
        return b;
    }

    private BoardDto.Request request(String name, String desc) {
        BoardDto.Request req = new BoardDto.Request();
        req.setName(name);
        req.setDescription(desc);
        return req;
    }

    @Test
    @DisplayName("게시판 전체 목록 조회")
    void getAllBoards_returnsList() {
        User creator = user(1L, "테스터");
        given(boardRepository.findAll())
                .willReturn(List.of(board(1L, "공지사항", creator), board(2L, "자유게시판", creator)));

        List<BoardDto.Response> result = boardService.getAllBoards();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("게시판 검색 - 키워드 일치 결과 반환")
    void searchBoards_returnsMatching() {
        User creator = user(1L, "테스터");
        given(boardRepository.findByNameContainingIgnoreCase("공지"))
                .willReturn(List.of(board(1L, "공지사항", creator)));

        List<BoardDto.Response> result = boardService.searchBoards("공지");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("게시판 생성 성공")
    void createBoard_success() {
        User creator = user(1L, "테스터");
        given(userRepository.findById(1L)).willReturn(Optional.of(creator));
        given(boardRepository.save(any(Board.class))).willReturn(board(10L, "새게시판", creator));

        BoardDto.Response resp = boardService.createBoard(request("새게시판", "설명"), 1L);

        assertThat(resp.getName()).isEqualTo("새게시판");
    }

    @Test
    @DisplayName("게시판 생성 실패 - 유저 없음")
    void createBoard_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.createBoard(request("새게시판", "설명"), 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("게시판 수정 성공 - 소유자 일치")
    void updateBoard_success() {
        User creator = user(1L, "테스터");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board(1L, "기존이름", creator)));

        BoardDto.Response resp = boardService.updateBoard(1L, request("수정이름", "수정설명"), 1L);

        assertThat(resp.getName()).isEqualTo("수정이름");
    }

    @Test
    @DisplayName("게시판 수정 실패 - 소유자 불일치")
    void updateBoard_notOwner_throws() {
        User creator = user(1L, "테스터");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board(1L, "기존이름", creator)));

        assertThatThrownBy(() -> boardService.updateBoard(1L, request("수정이름", "수정설명"), 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("게시판 삭제 성공 - 소유자 일치")
    void deleteBoard_success() {
        User creator = user(1L, "테스터");
        Board b = board(1L, "게시판", creator);
        given(boardRepository.findById(1L)).willReturn(Optional.of(b));
        willDoNothing().given(boardRepository).delete(b);

        boardService.deleteBoard(1L, 1L);

        then(boardRepository).should().delete(b);
    }

    @Test
    @DisplayName("게시판 삭제 실패 - 소유자 불일치")
    void deleteBoard_notOwner_throws() {
        User creator = user(1L, "테스터");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board(1L, "게시판", creator)));

        assertThatThrownBy(() -> boardService.deleteBoard(1L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("게시판 즐겨찾기 추가")
    void toggleBoardBookmark_add() {
        User creator = user(1L, "테스터");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board(1L, "게시판", creator)));
        given(userRepository.findById(1L)).willReturn(Optional.of(creator));
        given(boardBookmarkRepository.findByUserIdAndBoardId(1L, 1L)).willReturn(Optional.empty());

        String result = boardService.toggleBoardBookmark(1L, 1L);

        assertThat(result).contains("추가");
        then(boardBookmarkRepository).should().save(any(BoardBookmark.class));
    }

    @Test
    @DisplayName("게시판 즐겨찾기 해제")
    void toggleBoardBookmark_remove() {
        User creator = user(1L, "테스터");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board(1L, "게시판", creator)));
        given(userRepository.findById(1L)).willReturn(Optional.of(creator));
        BoardBookmark existing = new BoardBookmark();
        given(boardBookmarkRepository.findByUserIdAndBoardId(1L, 1L)).willReturn(Optional.of(existing));
        willDoNothing().given(boardBookmarkRepository).delete(existing);

        String result = boardService.toggleBoardBookmark(1L, 1L);

        assertThat(result).contains("해제");
        then(boardBookmarkRepository).should().delete(existing);
    }

    @Test
    @DisplayName("내 게시판 즐겨찾기 목록 조회")
    void getMyBoardBookmarks_returnsList() {
        User creator = user(1L, "테스터");
        Board b = board(1L, "즐겨찾기게시판", creator);
        BoardBookmark bm = mock(BoardBookmark.class);
        given(bm.getBoard()).willReturn(b);
        given(boardBookmarkRepository.findByUserId(1L)).willReturn(List.of(bm));

        List<BoardDto.Response> result = boardService.getMyBoardBookmarks(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("즐겨찾기게시판");
    }
}
