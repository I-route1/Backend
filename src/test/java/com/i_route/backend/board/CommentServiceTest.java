package com.i_route.backend.board;

import com.i_route.backend.board.dto.*;
import com.i_route.backend.board.service.BoardService;
import com.i_route.backend.board.repository.CommentLikeRepository;
import com.i_route.backend.board.entity.Comment;
import com.i_route.backend.board.entity.CommentLike;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.repository.CommentRepository;
import com.i_route.backend.board.repository.PostRepository;
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
class CommentServiceTest {

    @InjectMocks
    private BoardService postService;

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private UserRepository userRepository;

    // BoardService가 의존하는 나머지 Mock (사용 안 해도 선언 필요)
    @Mock private com.i_route.backend.board.repository.BoardRepository boardRepository;
    @Mock private com.i_route.backend.board.repository.PostBookmarkRepository postBookmarkRepository;
    @Mock private com.i_route.backend.board.repository.PostLikeRepository postLikeRepository;

    private Post mockPost() {
        Board board = new Board();
        board.setName("게시판");
        Post post = new Post();
        post.setBoard(board);
        post.setTitle("제목");
        return post;
    }

    @Test
    @DisplayName("댓글 목록 조회 - 성공")
    void getComments_success() {
        Comment comment = new Comment();
        comment.setPost(mockPost());
        comment.setContent("댓글 내용");
        comment.setAuthor("작성자");

        given(commentRepository.findByPostIdOrderByCreatedAtDesc(1L)).willReturn(List.of(comment));

        List<CommentResponseDto> result = postService.getComments(1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("댓글 내용");
    }

    @Test
    @DisplayName("댓글 상세 조회 - 성공")
    void getCommentDetail_success() {
        Post post = mockPost();
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setContent("상세 댓글");

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        CommentResponseDto result = postService.getCommentDetail(1L, null);

        assertThat(result.getContent()).isEqualTo("상세 댓글");
    }

    @Test
    @DisplayName("댓글 작성 - 성공")
    void createComment_success() {
        Post post = mockPost();
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setContent("새 댓글");
        comment.setAuthor("작성자");

        CommentRequestDto request = new CommentRequestDto();
        request.setContent("새 댓글");
        request.setAuthor("작성자");

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().id(1L).nickname("author").build()));

        CommentResponseDto result = postService.createComment(1L, request, 1L);

        assertThat(result.getContent()).isEqualTo("새 댓글");
    }

    @Test
    @DisplayName("댓글 작성 - 게시글 없으면 예외")
    void createComment_postNotFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        CommentRequestDto request = new CommentRequestDto();
        request.setContent("내용");

        assertThatThrownBy(() -> postService.createComment(999L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("댓글 삭제 - 성공")
    void deleteComment_success() {
        Post post = mockPost();
        Comment comment = new Comment();
        comment.setPost(post);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        comment.setUser(User.builder().id(1L).build());
        willDoNothing().given(commentRepository).delete(comment);

        assertThatCode(() -> postService.deleteComment(null, 1L, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("댓글 좋아요 - 새로 추가")
    void likeComment_add() {
        Comment comment = new Comment();
        comment.setPost(mockPost());
        User user = new User();


        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));


        assertThatCode(() -> postService.likeComment(1L, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("댓글 좋아요 - 이미 있으면 취소(토글)")
    void likeComment_toggle() {
        given(commentRepository.findById(1L)).willReturn(Optional.of(new Comment()));
        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(commentLikeRepository.existsByCommentIdAndUserId(1L, 1L)).willReturn(true);

        assertThatCode(() -> postService.likeComment(1L, 1L))
                .doesNotThrowAnyException();

        then(commentLikeRepository).should().deleteByCommentIdAndUserId(1L, 1L);
    }
}
