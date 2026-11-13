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

    // ==========================================
    // 1. 게시판 (Board) 관리
    // ==========================================

    // 1. 게시판 목록 조회 (line 41 부근)
    @Transactional(readOnly = true)
    public List<Board> getBoards() {
        return boardRepository.findAllWithAcademyAndUser();
    }

    // 2. 게시판 단건 조회 헬퍼 (line 327 부근)
    private Board getBoardOrThrow(Long boardId) {
        return boardRepository.findByIdWithAcademyAndUser(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Board getBoardDetail(Long boardId) {
        return getBoardOrThrow(boardId);
    }

    // ==========================================
    // 2. 게시글 (Post) 관리
    // ==========================================

    /**
     * 전체 게시글 조회
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getPosts(Long userId) {
        return postRepository.findAll()
                .stream()
                .map(post -> toPostResponse(post, userId))
                .toList();
    }

    /**
     * 특정 게시판(boardId)에 속한 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getPostsByBoard(Long boardId, Long userId) {
        getBoardOrThrow(boardId); // 게시판 존재 여부 검증
        return postRepository.findByBoardId(boardId)
                .stream()
                .map(post -> toPostResponse(post, userId))
                .toList();
    }

    /**
     * 특정 학원(academyId)에 속한 게시판들의 글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getPostsByAcademy(Long academyId, Long userId) {
        return postRepository.findByBoard_Academy_AcademyId(academyId)
                .stream()
                .map(post -> toPostResponse(post, userId))
                .toList();
    }

    /**
     * 게시글 작성 (요청 DTO에 담긴 boardId 활용)
     */
    public PostResponseDto createPost(PostRequestDto request, Long userId) {
        // DTO에서 받은 boardId로 게시판 조회 (없으면 예외 발생)
        Board board = getBoardOrThrow(request.getBoardId());
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

    /**
     * 게시글 상세 조회 (조회수 중복 방지 로직 유지)
     */
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

    @Transactional(readOnly = true)
    public List<PostResponseDto> searchPosts(String keyword, Long userId) {
        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword)
                .stream()
                .map(post -> toPostResponse(post, userId))
                .toList();
    }

    public PostResponseDto updatePost(Long postId, PostRequestDto request, Long userId) {
        Post post = getPostOrThrow(postId);

        // 작성자 본인 확인 검증 (선택적 구현)
        if (post.getUser() != null && !post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 글을 수정할 수 있습니다.");
        }

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

    // ==========================================
    // 3. 좋아요 / 북마크 기능
    // ==========================================

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

        boolean alreadyBookmarked = postBookmarkRepository.existsByPostIdAndUserId(postId, userId);

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

    // ==========================================
    // 4. 댓글 (Comment) 관리
    // ==========================================

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

        boolean alreadyLiked = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

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

    // ==========================================
    // 5. Helper & DTO 매핑 메서드
    // ==========================================

    private PostResponseDto toPostResponse(Post post, Long userId) {
        Long postId = post.getId();

        long likeCount = postLikeRepository.countByPostId(postId);
        long commentCount = commentRepository.countByPostId(postId);

        boolean likedByMe = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);
        boolean bookmarked = userId != null && postBookmarkRepository.existsByPostIdAndUserId(postId, userId);

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

    // 해당 유저가 이 학원 게시판의 관리자(선생님)인지 확인하는 헬퍼 메서드
    public boolean isBoardManager(Board board, Long userId) {
        if (userId == null || board.getAcademy() == null || board.getAcademy().getUser() == null) {
            return false;
        }
        return board.getAcademy().getUser().getId().equals(userId);
    }

    @Transactional
    public BoardResponseDto updateBoard(Long boardId, BoardUpdateRequestDto requestDto, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));

        if (!isBoardManager(board, userId)) {
            throw new IllegalStateException("해당 학원의 선생님(관리자)만 게시판을 수정할 수 있습니다.");
        }

        // 1. 게시판 이름 및 소개 수정
        board.updateInfo(requestDto.getName(), requestDto.getDescription());

        // 2. 학원 게시판인 경우 학원 주소도 함께 수정
        if (board.getAcademy() != null && requestDto.getAcademyAddress() != null) {
            String trimmedAddress = requestDto.getAcademyAddress().trim();
            board.getAcademy().setAcademyAddress(trimmedAddress.isEmpty() ? null : trimmedAddress);
        }

        return BoardResponseDto.from(board);
    }
}