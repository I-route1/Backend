package com.i_route.backend.domain.board.service;

import com.i_route.backend.domain.board.dto.CommentDto;
import com.i_route.backend.domain.board.dto.PostDto;
import com.i_route.backend.domain.board.entity.Board;
import com.i_route.backend.domain.board.entity.Bookmark;
import com.i_route.backend.domain.board.entity.Like;
import com.i_route.backend.domain.board.entity.Post;
import com.i_route.backend.domain.board.repository.*;
import com.i_route.backend.domain.user.entity.User;
import com.i_route.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;

    // 게시글 목록 (페이지네이션)
    public Page<PostDto.ListResponse> getPostsByBoard(Long boardId, Pageable pageable) {
        return postRepository.findByBoardIdAndDeletedAtIsNull(boardId, pageable)
                .map(PostDto.ListResponse::from);
    }

    // 게시글 상세 + 조회수 증가
    @Transactional
    public PostDto.DetailResponse getPost(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        post.setViewCount(post.getViewCount() + 1);

        boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);
        boolean bookmarked = bookmarkRepository.existsByUserIdAndPostId(userId, postId);

        List<CommentDto.Response> comments = post.getComments().stream()
                .map(CommentDto.Response::from)
                .collect(Collectors.toList());

        return PostDto.DetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getAuthor().getNickname())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikes().size())
                .likedByMe(liked)
                .bookmarkedByMe(bookmarked)
                .comments(comments)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // 게시글 검색
    public Page<PostDto.ListResponse> searchPosts(String keyword, Pageable pageable) {
        return postRepository.searchByKeyword(keyword, pageable)
                .map(PostDto.ListResponse::from);
    }

    @Transactional
    public PostDto.ListResponse createPost(Long boardId, PostDto.Request request, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("게시판을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        Post post = new Post();
        post.setBoard(board);
        post.setAuthor(user);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        return PostDto.ListResponse.from(postRepository.save(post));
    }

    @Transactional
    public PostDto.ListResponse updatePost(Long postId, PostDto.Request request, Long userId) {

        Post post = getPostOrThrow(postId);

        checkPermission(post, userId);

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        return PostDto.ListResponse.from(post);
    }

    // 소프트 삭제
    @Transactional
    public void deletePost(Long postId, Long userId) {

        Post post = getPostOrThrow(postId);

        checkPermission(post, userId);

        post.setDeletedAt(LocalDateTime.now());
    }

    // 공감 토글
    @Transactional
    public String toggleLike(Long postId, Long userId) {

        Post post = getPostOrThrow(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        return likeRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    likeRepository.delete(existing);
                    return "공감을 취소했습니다.";
                })
                .orElseGet(() -> {
                    Like like = new Like();
                    like.setPost(post);
                    like.setUser(user);
                    likeRepository.save(like);
                    return "공감했습니다.";
                });
    }

    // 즐겨찾기 토글
    @Transactional
    public String toggleBookmark(Long postId, Long userId) {

        Post post = getPostOrThrow(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        return bookmarkRepository.findByUserIdAndPostId(userId, postId)
                .map(existing -> {
                    bookmarkRepository.delete(existing);
                    return "즐겨찾기를 해제했습니다.";
                })
                .orElseGet(() -> {
                    Bookmark bookmark = new Bookmark();
                    bookmark.setUser(user);
                    bookmark.setPost(post);
                    bookmarkRepository.save(bookmark);
                    return "즐겨찾기에 추가했습니다.";
                });
    }

    // 내 즐겨찾기 목록
    public List<PostDto.ListResponse> getMyBookmarks(Long userId) {
        return bookmarkRepository.findByUserId(userId).stream()
                .map(b -> PostDto.ListResponse.from(b.getPost()))
                .collect(Collectors.toList());
    }

    private Post getPostOrThrow(Long id) {
        return postRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));
    }

    private void checkPermission(Post post, Long requesterId) {

        User user = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        // 1. 관리자면 무조건 허용
        if (user.getRole() == User.UserRole.ADMIN) return;

        // 2. 작성자만 허용
        if (!post.getAuthor().getId().equals(requesterId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }
}

