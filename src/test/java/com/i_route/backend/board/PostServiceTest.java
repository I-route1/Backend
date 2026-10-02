package com.i_route.backend.board;

import com.i_route.backend.board.dto.*;
import com.i_route.backend.board.service.BoardService;
import com.i_route.backend.board.entity.*;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.repository.*;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
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
class PostServiceTest {

    @InjectMocks
    private BoardService postService;

    @Mock private PostRepository postRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostBookmarkRepository postBookmarkRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private UserRepository userRepository;

    private Board mockBoard() {
        Board board = new Board();
        board.setName("테스트 게시판");
        return board;
    }

    private Post mockPost(Board board) {
        Post post = new Post();
        post.setBoard(board);
        post.setTitle("테스트 제목");
        post.setContent("테스트 내용");
        post.setAuthor("작성자");
        return post;
    }

    @Test
    @DisplayName("게시글 목록 조회 - 성공")
    void getPostsByBoard_success() {
        Post post = mockPost(mockBoard());
        given(postRepository.findAll()).willReturn(List.of(post));

        List<PostResponseDto> result = postService.getPosts(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공")
    void getPostDetail_success() {
        Post post = mockPost(mockBoard());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostResponseDto result = postService.getPostDetail(1L, null);

        assertThat(result.getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("게시글 상세 조회 - 없는 ID 예외")
    void getPostDetail_notFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostDetail(999L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("게시글 작성 - 성공")
    void createPost_success() {
        Board board = mockBoard();
        Post post = mockPost(board);

        PostRequestDto request = new PostRequestDto();
        request.setTitle("새 제목");
        request.setContent("새 내용");
        request.setAuthor("작성자");

        given(boardRepository.findAll()).willReturn(List.of(board));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().id(1L).nickname("author").build()));

        PostResponseDto result = postService.createPost(request, 1L);

        assertThat(result.getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void updatePost_success() {
        Post post = mockPost(mockBoard());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostRequestDto request = new PostRequestDto();
        request.setTitle("수정된 제목");
        request.setContent("수정된 내용");

        PostResponseDto result = postService.updatePost(1L, request, null);

        assertThat(result.getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void deletePost_success() {
        willDoNothing().given(postRepository).deleteById(1L);

        assertThatCode(() -> postService.deletePost(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("게시글 좋아요 - 새로 추가")
    void likePost_add() {
        Post post = mockPost(mockBoard());
        User user = new User();


        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));


        assertThatCode(() -> postService.likePost(1L, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("게시글 좋아요 - 이미 있으면 취소(토글)")
    void likePost_toggle() {
        Post post = mockPost(mockBoard());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(true);

        assertThatCode(() -> postService.likePost(1L, 1L))
                .doesNotThrowAnyException();

        then(postLikeRepository).should().deleteByPostIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("게시글 북마크 - 새로 추가")
    void bookmarkPost_add() {
        Post post = mockPost(mockBoard());
        User user = new User();


        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));


        assertThatCode(() -> postService.bookmarkPost(1L, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    void bookmarkPost_toggle() {
        given(postRepository.findById(1L)).willReturn(Optional.of(mockPost(mockBoard())));
        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(postBookmarkRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(true);
        postService.bookmarkPost(1L, 1L);
        then(postBookmarkRepository).should().deleteByPostIdAndUserId(1L, 1L);
        then(postBookmarkRepository).should(never()).save(any());
    }
}
