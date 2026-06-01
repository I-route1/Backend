package com.i_route.backend.board;

import com.i_route.backend.board.dto.CommentRequestDto;
import com.i_route.backend.board.entity.Comment;
import com.i_route.backend.board.entity.CommentLike;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.repository.CommentRepository;
import com.i_route.backend.board.repository.PostRepository;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
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
class CommentServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private UserRepository userRepository;

    // PostService가 의존하는 나머지 Mock (사용 안 해도 선언 필요)
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

        given(commentRepository.findByPostId(1L)).willReturn(List.of(comment));

        List<CommentRequestDto.Response> result = postService.getComments(1L);

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

        // post.id가 null이므로 equals 비교용 id 세팅 필요
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // postId가 null == null 이므로 통과
        CommentRequestDto.Response result = postService.getCommentDetail(null, 1L);

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

        CommentRequestDto.Request request = new CommentRequestDto.Request();
        request.setContent("새 댓글");
        request.setAuthor("작성자");

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        CommentRequestDto.Response result = postService.createComment(1L, request);

        assertThat(result.getContent()).isEqualTo("새 댓글");
    }

    @Test
    @DisplayName("댓글 작성 - 게시글 없으면 예외")
    void createComment_postNotFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        CommentRequestDto.Request request = new CommentRequestDto.Request();
        request.setContent("내용");

        assertThatThrownBy(() -> postService.createComment(999L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 삭제 - 성공")
    void deleteComment_success() {
        Post post = mockPost();
        Comment comment = new Comment();
        comment.setPost(post);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        willDoNothing().given(commentRepository).delete(comment);

        assertThatCode(() -> postService.deleteComment(null, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("댓글 좋아요 - 새로 추가")
    void likeComment_add() {
        Comment comment = new Comment();
        comment.setPost(mockPost());
        User user = new User();

        given(commentLikeRepository.findByCommentIdAndUserId(1L, String.valueOf(1L))).willReturn(Optional.empty());
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(commentLikeRepository.save(any(CommentLike.class))).willReturn(new CommentLike());

        assertThatCode(() -> postService.likeComment(1L, 1L, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("댓글 좋아요 - 이미 있으면 취소(토글)")
    void likeComment_toggle() {
        CommentLike existing = new CommentLike();
        given(commentLikeRepository.findByCommentIdAndUserId(1L, String.valueOf(1L))).willReturn(Optional.of(existing));

        assertThatCode(() -> postService.likeComment(1L, 1L, null))
                .doesNotThrowAnyException();

        then(commentLikeRepository).should().delete(existing);
    }
}
