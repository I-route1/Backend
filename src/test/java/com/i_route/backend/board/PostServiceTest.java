package com.i_route.backend.board;

import com.i_route.backend.board.dto.PostDto;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.Bookmark;
import com.i_route.backend.board.entity.Like;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.repository.*;
import com.i_route.backend.board.service.PostService;
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
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock private PostRepository postRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private UserRepository userRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private CommentLikeRepository commentLikeRepository;

    private User user(Long id, String nickname, User.UserRole role) {
        return User.builder()
                .id(id).nickname(nickname)
                .role(role).loginType(User.LoginType.EMAIL)
                .build();
    }

    private Board board(Long id) {
        Board b = new Board();
        b.setId(id);
        b.setName("테스트게시판");
        User creator = user(1L, "테스터", User.UserRole.PARENT);
        b.setCreatedBy(creator);
        return b;
    }

    private Post post(Long id, String title, User author, boolean anonymous) {
        Post p = new Post();
        p.setId(id);
        p.setTitle(title);
        p.setContent("내용");
        p.setAuthor(author);
        p.setAnonymous(anonymous);
        return p;
    }

    private PostDto.Request postRequest(String title, String content, boolean anonymous) {
        PostDto.Request req = new PostDto.Request();
        req.setTitle(title);
        req.setContent(content);
        req.setAnonymous(anonymous);
        return req;
    }

    @Test
    @DisplayName("게시글 생성 성공")
    void createPost_success() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Board b = board(1L);
        Post saved = post(10L, "새글", author, false);
        given(boardRepository.findById(1L)).willReturn(Optional.of(b));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        PostDto.ListResponse resp = postService.createPost(1L, postRequest("새글", "내용", false), 1L);

        assertThat(resp.getTitle()).isEqualTo("새글");
        assertThat(resp.getAuthor()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("게시글 생성 실패 - 게시판 없음")
    void createPost_boardNotFound() {
        given(boardRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(99L, postRequest("새글", "내용", false), 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("게시글 생성 실패 - 유저 없음")
    void createPost_userNotFound() {
        given(boardRepository.findById(1L)).willReturn(Optional.of(board(1L)));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(1L, postRequest("새글", "내용", false), 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("게시글 익명 작성 - 작성자명 '익명' 처리")
    void createPost_anonymous_showsAnonymous() {
        User author = user(1L, "실명작성자", User.UserRole.PARENT);
        Board b = board(1L);
        Post saved = post(10L, "익명글", author, true);
        given(boardRepository.findById(1L)).willReturn(Optional.of(b));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        PostDto.ListResponse resp = postService.createPost(1L, postRequest("익명글", "내용", true), 1L);

        assertThat(resp.getAuthor()).isEqualTo("익명");
    }

    @Test
    @DisplayName("게시글 조회 - 조회수 증가")
    void getPost_incrementsViewCount() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Post p = post(1L, "조회글", author, false);
        p.setViewCount(5);
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(likeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(false);
        given(bookmarkRepository.existsByUserIdAndPostId(1L, 1L)).willReturn(false);

        PostDto.DetailResponse resp = postService.getPost(1L, 1L);

        assertThat(resp.getViewCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("게시글 조회 실패 - 삭제된 게시글")
    void getPost_deletedPost_throws() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Post p = post(1L, "삭제글", author, false);
        p.setDeletedAt(java.time.LocalDateTime.now());
        given(postRepository.findById(1L)).willReturn(Optional.of(p));

        assertThatThrownBy(() -> postService.getPost(1L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("게시글 소프트 삭제 - deletedAt 설정")
    void deletePost_softDelete() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Post p = post(1L, "삭제글", author, false);
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));

        postService.deletePost(1L, 1L);

        assertThat(p.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자 불일치")
    void deletePost_notAuthor_throws() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        User other = user(2L, "타인", User.UserRole.PARENT);
        Post p = post(1L, "삭제글", author, false);
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(2L)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> postService.deletePost(1L, 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("게시글 삭제 성공 - ADMIN은 타인 글도 삭제 가능")
    void deletePost_admin_bypass() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        User admin = user(99L, "관리자", User.UserRole.ADMIN);
        Post p = post(1L, "삭제글", author, false);
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(99L)).willReturn(Optional.of(admin));

        postService.deletePost(1L, 99L);

        assertThat(p.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글 공감 추가")
    void toggleLike_add() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Post p = post(1L, "글", author, false);
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(likeRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.empty());

        String result = postService.toggleLike(1L, 1L);

        assertThat(result).contains("공감");
        then(likeRepository).should().save(any(Like.class));
    }

    @Test
    @DisplayName("게시글 공감 취소")
    void toggleLike_remove() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Post p = post(1L, "글", author, false);
        Like existing = new Like();
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(likeRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.of(existing));
        willDoNothing().given(likeRepository).delete(existing);

        String result = postService.toggleLike(1L, 1L);

        assertThat(result).contains("취소");
        then(likeRepository).should().delete(existing);
    }

    @Test
    @DisplayName("게시글 즐겨찾기 추가")
    void toggleBookmark_add() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Post p = post(1L, "글", author, false);
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(bookmarkRepository.findByUserIdAndPostId(1L, 1L)).willReturn(Optional.empty());

        String result = postService.toggleBookmark(1L, 1L);

        assertThat(result).contains("추가");
        then(bookmarkRepository).should().save(any(Bookmark.class));
    }

    @Test
    @DisplayName("게시글 즐겨찾기 해제")
    void toggleBookmark_remove() {
        User author = user(1L, "작성자", User.UserRole.PARENT);
        Post p = post(1L, "글", author, false);
        Bookmark existing = new Bookmark();
        given(postRepository.findById(1L)).willReturn(Optional.of(p));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(bookmarkRepository.findByUserIdAndPostId(1L, 1L)).willReturn(Optional.of(existing));
        willDoNothing().given(bookmarkRepository).delete(existing);

        String result = postService.toggleBookmark(1L, 1L);

        assertThat(result).contains("해제");
        then(bookmarkRepository).should().delete(existing);
    }
}
