package com.i_route.backend.board;

import com.i_route.backend.board.dto.PostRequestDto;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.repository.*;
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
class PostServiceTest {

    @InjectMocks
    private PostService postService;

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
        given(postRepository.findByBoardId(1L)).willReturn(List.of(post));

        List<PostRequestDto.Response> result = postService.getPostsByBoard(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공")
    void getPostDetail_success() {
        Post post = mockPost(mockBoard());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostRequestDto.Response result = postService.getPostDetail(1L);

        assertThat(result.getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("게시글 상세 조회 - 없는 ID 예외")
    void getPostDetail_notFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostDetail(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("게시글 작성 - 성공")
    void createPost_success() {
        Board board = mockBoard();
        Post post = mockPost(board);

        PostRequestDto.Request request = new PostRequestDto.Request();
        request.setTitle("새 제목");
        request.setContent("새 내용");
        request.setAuthor("작성자");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(postRepository.save(any(Post.class))).willReturn(post);

        PostRequestDto.Response result = postService.createPost(1L, request);

        assertThat(result.getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void updatePost_success() {
        Post post = mockPost(mockBoard());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostRequestDto.Request request = new PostRequestDto.Request();
        request.setTitle("수정된 제목");
        request.setContent("수정된 내용");

        PostRequestDto.Response result = postService.updatePost(1L, request);

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

        given(postLikeRepository.findByPostIdAndUserId(1L, String.valueOf(1L))).willReturn(Optional.empty());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.save(any(PostLike.class))).willReturn(new PostLike());

        assertThatCode(() -> postService.likePost(1L, String.valueOf(1L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("게시글 좋아요 - 이미 있으면 취소(토글)")
    void likePost_toggle() {
        PostLike existing = new PostLike();
        given(postLikeRepository.findByPostIdAndUserId(1L, String.valueOf(1L))).willReturn(Optional.of(existing));

        assertThatCode(() -> postService.likePost(1L, String.valueOf(1L)))
                .doesNotThrowAnyException();

        then(postLikeRepository).should().delete(existing);
    }

    @Test
    @DisplayName("게시글 북마크 - 새로 추가")
    void bookmarkPost_add() {
        Post post = mockPost(mockBoard());
        User user = new User();

        given(postBookmarkRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.empty());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postBookmarkRepository.save(any(PostBookmarkRepository.class))).willReturn(new PostBookmarkRepository());

        assertThatCode(() -> postService.bookmarkPost(1L, String.valueOf(1L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("내 북마크 게시글 목록 조회")
    void getMyBookmarkedPosts_success() {
        Post post = mockPost(mockBoard());
        PostBookmarkRepository bookmark = new PostBookmarkRepository();
        bookmark.setPost(post);

        given(postBookmarkRepository.findByUserId(1L)).willReturn(List.of(bookmark));

        List<PostRequestDto.Response> result = postService.getMyBookmarkedPosts(String.valueOf(1L));

        assertThat(result).hasSize(1);
    }
}
