package com.i_route.backend.board.service;

import com.i_route.backend.board.dto.*;
import com.i_route.backend.board.entity.*;
import com.i_route.backend.board.repository.*;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final CommentLikeRepository commentLikeRepository;

    private final Map<String, LocalDateTime> viewHistory = new ConcurrentHashMap<>();

    public List<Board> getBoards() {
        return boardRepository.findAll();
    }

    public List<Board> searchBoards(String keyword) {
        return boardRepository.findByNameContaining(keyword);
    }

    public Board createBoard(BoardRequestDto request) {
        Board board = Board.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy("관리자")
                .build();

        return boardRepository.save(board);
    }

    public Board updateBoard(Long boardId, BoardRequestDto request) {
        Board board = getBoardOrThrow(boardId);
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        return board;
    }

    public void deleteBoard(Long boardId) {
        boardRepository.deleteById(boardId);
    }

    public Board getBoardDetail(Long boardId) {
        return getBoardOrThrow(boardId);
    }

    public List<PostResponseDto> getPosts(Long userId) {
        return postRepository.findAll()
                .stream()
                .map(post -> toPostResponse(post, userId))
                .toList();
    }

    public PostResponseDto createPost(
            PostRequestDto request,
            Long userId
    ) {

        Board board = boardRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("게시판이 존재하지 않습니다.")
                );

        User user = getUserOrThrow(userId);

        Post post = Post.builder()
                .board(board)
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory() != null ? request.getCategory() : "자유")
                .author(user.getNickname())
                .pinned(false)
                .build();
        Post savedPost = postRepository.save(post);

        return toPostResponse(savedPost, userId);
    }

    public PostResponseDto getPostDetail(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);

        String viewerKey = userId != null
                ? "USER:" + userId + ":POST:" + postId
                : "GUEST:" + postId;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastViewedAt = viewHistory.get(viewerKey);

        if (lastViewedAt == null || lastViewedAt.plusSeconds(3).isBefore(now)) {
            post.increaseViewCount();
            viewHistory.put(viewerKey, now);
        }

        return toPostResponse(post, userId);
    }

    public List<PostResponseDto> searchPosts(String keyword, Long userId) {
        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword)
                .stream()
                .map(post -> toPostResponse(post, userId))
                .toList();
    }

    public PostResponseDto updatePost(Long postId, PostRequestDto request, Long userId) {
        Post post = getPostOrThrow(postId);
        post.update(
                request.getTitle(),
                request.getContent(),
                request.getCategory() != null ? request.getCategory() : post.getCategory()
        );
        return toPostResponse(post, userId);
    }

    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }

    public PostResponseDto likePost(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        User user = getUserOrThrow(userId);

        boolean alreadyLiked = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        if (alreadyLiked) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            postLikeRepository.save(
                    PostLike.builder()
                            .post(post)
                            .user(user)
                            .build()
            );
        }

        return toPostResponse(post, userId);
    }

    public PostResponseDto bookmarkPost(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        User user = getUserOrThrow(userId);

        boolean alreadyBookmarked =
                postBookmarkRepository.existsByPostIdAndUserId(postId, userId);

        if (alreadyBookmarked) {
            postBookmarkRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            postBookmarkRepository.save(
                    PostBookmark.builder()
                            .post(post)
                            .user(user)
                            .build()
            );
        }

        return toPostResponse(post, userId);
    }

    public List<CommentResponseDto> getComments(Long postId, Long userId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(comment -> toCommentResponse(comment, userId))
                .toList();
    }

    public CommentResponseDto createComment(Long postId, CommentRequestDto request, Long userId) {
        Post post = getPostOrThrow(postId);
        User user = getUserOrThrow(userId);

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .author(user.getNickname())
                .build();

        Comment savedComment = commentRepository.save(comment);

        return toCommentResponse(savedComment, userId);
    }

    public CommentResponseDto getCommentDetail(Long commentId, Long userId) {
        Comment comment = getCommentOrThrow(commentId);
        return toCommentResponse(comment, userId);
    }

    public void deleteComment(Long postId, Long commentId, Long userId) {
        Comment comment = getCommentOrThrow(commentId);

        if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 댓글을 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    public CommentResponseDto likeComment(Long commentId, Long userId) {
        Comment comment = getCommentOrThrow(commentId);
        User user = getUserOrThrow(userId);

        boolean alreadyLiked =
                commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        if (alreadyLiked) {
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
        } else {
            commentLikeRepository.save(
                    CommentLike.builder()
                            .comment(comment)
                            .user(user)
                            .build()
            );
        }

        return toCommentResponse(comment, userId);
    }

    private PostResponseDto toPostResponse(Post post, Long userId) {
        Long postId = post.getId();

        long likeCount = postLikeRepository.countByPostId(postId);
        long commentCount = commentRepository.countByPostId(postId);

        boolean likedByMe =
                userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);

        boolean bookmarked =
                userId != null && postBookmarkRepository.existsByPostIdAndUserId(postId, userId);

        return PostResponseDto.builder()
                .id(post.getId())
                .boardId(post.getBoard().getId())
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .category(post.getCategory())
                .title(post.getTitle())
                .author(post.getAuthor())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .viewCount(post.getViewCount())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByMe(likedByMe)
                .bookmarked(bookmarked)
                .pinned(post.isPinned())
                .build();
    }

    private CommentResponseDto toCommentResponse(Comment comment, Long userId) {
        Long commentId = comment.getId();

        return CommentResponseDto.builder()
                .id(comment.getId())
                .userId(comment.getUser() != null ? comment.getUser().getId() : null)
                .author(comment.getAuthor())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .likeCount(commentLikeRepository.countByCommentId(commentId))
                .likedByMe(userId != null && commentLikeRepository.existsByCommentIdAndUserId(commentId, userId))
                .build();
    }

    private Board getBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}