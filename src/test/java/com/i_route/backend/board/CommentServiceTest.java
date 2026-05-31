package com.i_route.backend.board;

import com.i_route.backend.board.dto.CommentDto;
import com.i_route.backend.board.entity.Comment;
import com.i_route.backend.board.entity.CommentLike;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.repository.CommentLikeRepository;
import com.i_route.backend.board.repository.CommentRepository;
import com.i_route.backend.board.repository.PostRepository;
import com.i_route.backend.board.service.CommentService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentLikeRepository commentLikeRepository;

    private User user(Long id, String nickname) {
        return User.builder()
                .id(id).nickname(nickname)
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL)
                .build();
    }

    private Post post(Long id) {
        Post p = new Post();
        p.setId(id);
        p.setTitle("테스트글");
        p.setContent("내용");
        p.setAuthor(user(1L, "작성자"));
        return p;
    }

    private Comment comment(Long id, User author) {
        Comment c = new Comment();
        c.setId(id);
        c.setContent("댓글 내용");
        c.setAuthor(author);
        c.setPost(post(1L));
        return c;
    }

    private CommentDto.Request commentRequest(String content) {
        CommentDto.Request req = new CommentDto.Request();
        req.setContent(content);
        return req;
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void addComment_success() {
        User author = user(1L, "댓글작성자");
        Post p = post(1L);
        Comment saved = comment(10L, author);
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);

        CommentDto.Response resp = commentService.addComment(1L, commentRequest("댓글 내용"), 1L);

        assertThat(resp.getContent()).isEqualTo("댓글 내용");
        assertThat(resp.getAuthor()).isEqualTo("댓글작성자");
    }

    @Test
    @DisplayName("댓글 작성 실패 - 게시글 없음")
    void addComment_postNotFound() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(99L, commentRequest("내용"), 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 작성 실패 - 유저 없음")
    void addComment_userNotFound() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post(1L)));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(1L, commentRequest("내용"), 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 삭제 성공 - 작성자 일치")
    void deleteComment_success() {
        User author = user(1L, "작성자");
        Comment c = comment(1L, author);
        given(commentRepository.findById(1L)).willReturn(Optional.of(c));
        willDoNothing().given(commentRepository).delete(c);

        commentService.deleteComment(1L, 1L);

        then(commentRepository).should().delete(c);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자 불일치")
    void deleteComment_notAuthor_throws() {
        User author = user(1L, "작성자");
        Comment c = comment(1L, author);
        given(commentRepository.findById(1L)).willReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.deleteComment(1L, 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글 없음")
    void deleteComment_notFound_throws() {
        given(commentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(99L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 추가")
    void toggleCommentLike_add() {
        User author = user(1L, "사용자");
        Comment c = comment(1L, author);
        given(commentRepository.findById(1L)).willReturn(Optional.of(c));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(commentLikeRepository.findByCommentIdAndUserId(1L, 1L)).willReturn(Optional.empty());

        String result = commentService.toggleCommentLike(1L, 1L);

        assertThat(result).contains("좋아요");
        then(commentLikeRepository).should().save(any(CommentLike.class));
    }

    @Test
    @DisplayName("댓글 좋아요 취소")
    void toggleCommentLike_remove() {
        User author = user(1L, "사용자");
        Comment c = comment(1L, author);
        CommentLike existing = new CommentLike();
        given(commentRepository.findById(1L)).willReturn(Optional.of(c));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(commentLikeRepository.findByCommentIdAndUserId(1L, 1L)).willReturn(Optional.of(existing));
        willDoNothing().given(commentLikeRepository).delete(existing);

        String result = commentService.toggleCommentLike(1L, 1L);

        assertThat(result).contains("취소");
        then(commentLikeRepository).should().delete(existing);
    }
}
