package com.i_route.backend.board;

import com.i_route.backend.board.dto.PostRequestDto;
import com.i_route.backend.board.entity.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

class PostServiceTest extends BoardServiceTestSupport {
    @Test void getPosts_returnsPostsWithUserFlagsAndCounts() {
        given(posts.findAll()).willReturn(List.of(post()));
        given(postLikes.countByPostId(10L)).willReturn(3L);
        given(comments.countByPostId(10L)).willReturn(2L);
        given(postLikes.existsByPostIdAndUserId(10L, 7L)).willReturn(true);
        var result = service.getPosts(7L);
        assertThat(result).singleElement().satisfies(p -> {
            assertThat(p.getTitle()).isEqualTo("제목");
            assertThat(p.getUserId()).isEqualTo(7L);
            assertThat(p.getLikeCount()).isEqualTo(3L);
            assertThat(p.getCommentCount()).isEqualTo(2L);
            assertThat(p.isLikedByMe()).isTrue();
        });
    }
    @Test void getPostDetail_countsOneViewForRepeatedImmediateReadsBySameUser() {
        var post = post(); given(posts.findById(10L)).willReturn(Optional.of(post));
        assertThat(service.getPostDetail(10L, 7L).getTitle()).isEqualTo("제목");
        service.getPostDetail(10L, 7L);
        assertThat(post.getViewCount()).isEqualTo(1);
        service.getPostDetail(10L, 8L);
        assertThat(post.getViewCount()).isEqualTo(2);
    }
    @Test void getPostDetail_missingPostThrows() {
        assertThatThrownBy(() -> service.getPostDetail(999L, 7L)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }
    @Test void createPost_usesAuthenticatedAuthorAndRequestContent() {
        var board = board();
        given(boards.findByIdWithAcademyAndUser(1L)).willReturn(Optional.of(board));
        given(users.findById(7L)).willReturn(Optional.of(author()));
        given(posts.save(any(Post.class))).willAnswer(inv -> { Post p = inv.getArgument(0); p.setId(10L); return p; });
        var request = new PostRequestDto();
        request.setBoardId(1L);
        request.setTitle("제목");
        request.setContent("내용");
        request.setAuthor("임의 작성자");
        var result = service.createPost(request, 7L);
        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getContent()).isEqualTo("내용");
        assertThat(result.getAuthor()).isEqualTo("작성자");
        assertThat(result.getCategory()).isEqualTo("자유");
        then(posts).should().save(argThat(p -> p.getUser().getId().equals(7L) && p.getBoard().getId().equals(1L)));
    }
    @Test void updatePost_updatesTextAndPreservesCategoryIfOmitted() {
        var post = post(); given(posts.findById(10L)).willReturn(Optional.of(post));
        var request = new PostRequestDto(); request.setTitle("수정 제목"); request.setContent("수정 내용");
        var result = service.updatePost(10L, request, 7L);
        assertThat(result.getTitle()).isEqualTo("수정 제목");
        assertThat(result.getContent()).isEqualTo("수정 내용");
        assertThat(result.getCategory()).isEqualTo("자유");
        assertThat(result.getUpdatedAt()).isNotNull();
    }
    @Test void deletePost_deletesRequestedPost() {
        service.deletePost(10L); then(posts).should().deleteById(10L);
    }
    private void existingPostAndUser() {
        given(posts.findById(10L)).willReturn(Optional.of(post()));
        given(users.findById(7L)).willReturn(Optional.of(author()));
    }
    @Test void likePost_addsLikeForAuthenticatedUser() {
        existingPostAndUser();
        given(postLikes.existsByPostIdAndUserId(10L, 7L)).willReturn(false, true);
        assertThat(service.likePost(10L, 7L).isLikedByMe()).isTrue();
        then(postLikes).should().save(argThat(l -> l.getPost().getId().equals(10L) && l.getUser().getId().equals(7L)));
    }
    @Test void likePost_removesExistingLike() {
        existingPostAndUser();
        given(postLikes.existsByPostIdAndUserId(10L, 7L)).willReturn(true, false);
        assertThat(service.likePost(10L, 7L).isLikedByMe()).isFalse();
        then(postLikes).should().deleteByPostIdAndUserId(10L, 7L);
        then(postLikes).should(never()).save(any());
    }
    @Test void bookmarkPost_addsBookmarkForAuthenticatedUser() {
        existingPostAndUser();
        given(bookmarks.existsByPostIdAndUserId(10L, 7L)).willReturn(false, true);
        assertThat(service.bookmarkPost(10L, 7L).isBookmarked()).isTrue();
        then(bookmarks).should().save(argThat(b -> b.getPost().getId().equals(10L) && b.getUser().getId().equals(7L)));
    }
    @Test void bookmarkPost_removesExistingBookmark() {
        existingPostAndUser();
        given(bookmarks.existsByPostIdAndUserId(10L, 7L)).willReturn(true, false);
        assertThat(service.bookmarkPost(10L, 7L).isBookmarked()).isFalse();
        then(bookmarks).should().deleteByPostIdAndUserId(10L, 7L);
    }
    @Test void getPosts_marksOwnBookmarkedPostsWithoutMarkingOtherUsers() {
        given(posts.findAll()).willReturn(List.of(post()));
        given(bookmarks.existsByPostIdAndUserId(10L, 7L)).willReturn(true);
        assertThat(service.getPosts(7L).get(0).isBookmarked()).isTrue();
        assertThat(service.getPosts(8L).get(0).isBookmarked()).isFalse();
    }
}
