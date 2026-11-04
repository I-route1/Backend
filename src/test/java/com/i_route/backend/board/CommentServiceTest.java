package com.i_route.backend.board;

import com.i_route.backend.board.dto.CommentRequestDto;
import com.i_route.backend.board.entity.Comment;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

class CommentServiceTest extends BoardServiceTestSupport {
    @Test void getComments_returnsContentAndUserLikeFlag() {
        given(comments.findByPostIdOrderByCreatedAtDesc(10L)).willReturn(List.of(comment()));
        given(commentLikes.existsByCommentIdAndUserId(20L, 7L)).willReturn(true);
        assertThat(service.getComments(10L, 7L)).singleElement().satisfies(c -> {
            assertThat(c.getContent()).isEqualTo("댓글");
            assertThat(c.isLikedByMe()).isTrue();
        });
    }
    @Test void getCommentDetail_returnsRealIdAndContent() {
        given(comments.findById(20L)).willReturn(Optional.of(comment()));
        var result = service.getCommentDetail(20L, 7L);
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getContent()).isEqualTo("댓글");
    }
    @Test void createComment_usesAuthenticatedAuthor() {
        given(posts.findById(10L)).willReturn(Optional.of(post()));
        given(users.findById(7L)).willReturn(Optional.of(author()));
        given(comments.save(any(Comment.class))).willAnswer(inv -> { Comment c = inv.getArgument(0); c.setId(20L); return c; });
        var request = new CommentRequestDto(); request.setContent("새 댓글"); request.setAuthor("위조 작성자");
        var result = service.createComment(10L, request, 7L);
        assertThat(result.getContent()).isEqualTo("새 댓글");
        assertThat(result.getAuthor()).isEqualTo("작성자");
        assertThat(result.getUserId()).isEqualTo(7L);
        then(comments).should().save(argThat(c -> c.getPost().getId().equals(10L)));
    }
    @Test void createComment_missingPostThrows() {
        assertThatThrownBy(() -> service.createComment(999L, new CommentRequestDto(), 7L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("게시글을 찾을 수 없습니다");
        then(comments).should(never()).save(any());
    }
    @Test void deleteComment_ownerCanDelete() {
        var comment = comment(); given(comments.findById(20L)).willReturn(Optional.of(comment));
        service.deleteComment(10L, 20L, 7L);
        then(comments).should().delete(comment);
    }
    @Test void deleteComment_nonOwnerCannotDelete() {
        given(comments.findById(20L)).willReturn(Optional.of(comment()));
        assertThatThrownBy(() -> service.deleteComment(10L, 20L, 8L)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("작성자만");
        then(comments).should(never()).delete(any());
    }
    private void existingCommentAndUser() {
        given(comments.findById(20L)).willReturn(Optional.of(comment()));
        given(users.findById(7L)).willReturn(Optional.of(author()));
    }
    @Test void likeComment_addsLikeForAuthenticatedUser() {
        existingCommentAndUser();
        given(commentLikes.existsByCommentIdAndUserId(20L, 7L)).willReturn(false, true);
        assertThat(service.likeComment(20L, 7L).isLikedByMe()).isTrue();
        then(commentLikes).should().save(argThat(l -> l.getComment().getId().equals(20L) && l.getUser().getId().equals(7L)));
    }
    @Test void likeComment_togglesExistingLikeOff() {
        existingCommentAndUser();
        given(commentLikes.existsByCommentIdAndUserId(20L, 7L)).willReturn(true, false);
        assertThat(service.likeComment(20L, 7L).isLikedByMe()).isFalse();
        then(commentLikes).should().deleteByCommentIdAndUserId(20L, 7L);
        then(commentLikes).should(never()).save(any());
    }
}
