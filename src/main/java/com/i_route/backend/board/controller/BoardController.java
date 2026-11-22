package com.i_route.backend.board.controller;

import com.i_route.backend.board.dto.*;
import com.i_route.backend.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.i_route.backend.global.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // ==========================================
    // 1. 게시판 (Board) API
    // ==========================================

    @GetMapping("/boards")
    public List<BoardResponseDto> getBoards() {
        return boardService.getBoardSummaries();
    }

    @GetMapping("/boards/search")
    public List<BoardResponseDto> searchBoards(@RequestParam String keyword) {
        return boardService.searchBoards(keyword).stream().map(BoardResponseDto::from).toList();
    }

    @PostMapping("/boards")
    public BoardResponseDto createBoard(@RequestBody BoardRequestDto request) {
        return BoardResponseDto.from(boardService.createBoard(request));
    }

    @PutMapping("/boards/{boardId}")
    public BoardResponseDto updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardRequestDto request
    ) {
        return BoardResponseDto.from(boardService.updateBoard(boardId, request));
    }

    @DeleteMapping("/boards/{boardId}")
    public void deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
    }

    @GetMapping("/boards/{boardId}")
    public BoardResponseDto getBoardDetail(@PathVariable Long boardId) {
        return BoardResponseDto.from(boardService.getBoardDetail(boardId));
    }

    // [신규 추가] 특정 게시판(boardId)에 속한 게시글 목록 조회
    @GetMapping("/boards/{boardId}/posts")
    public List<PostResponseDto> getPostsByBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return boardService.getPostsByBoard(boardId, user == null ? null : user.getId());
    }

    // [신규 추가] 특정 학원(academyId)에 속한 게시글 목록 조회
    @GetMapping("/academies/{academyId}/posts")
    public List<PostResponseDto> getPostsByAcademy(
            @PathVariable Long academyId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return boardService.getPostsByAcademy(academyId, user == null ? null : user.getId());
    }

    // ==========================================
    // 2. 게시글 (Post) API
    // ==========================================

    // 전체 게시글 조회
    @GetMapping("/posts")
    public List<PostResponseDto> getPosts(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return boardService.getPosts(user == null ? null : user.getId());
    }

    @PostMapping("/posts")
    public PostResponseDto createPost(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PostRequestDto request
    ) {
        return boardService.createPost(request, authenticatedUserId(user));
    }

    @GetMapping("/posts/{postId}")
    public PostResponseDto getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return boardService.getPostDetail(postId, user == null ? null : user.getId());
    }

    @GetMapping("/posts/search")
    public List<PostResponseDto> searchPosts(
            @RequestParam String keyword,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return boardService.searchPosts(keyword, user == null ? null : user.getId());
    }

    @PutMapping("/posts/{postId}")
    public PostResponseDto updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PostRequestDto request
    ) {
        return boardService.updatePost(postId, request, authenticatedUserId(user));
    }

    @DeleteMapping("/posts/{postId}")
    public void deletePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails user) {
        boardService.deletePost(postId, authenticatedUserId(user));
    }

    // ==========================================
    // 3. 좋아요 및 북마크 API
    // ==========================================

    @PostMapping("/posts/{postId}/like")
    public PostResponseDto likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return boardService.likePost(postId, authenticatedUserId(user));
    }

    @PostMapping("/posts/{postId}/bookmark")
    public PostResponseDto bookmarkPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return boardService.bookmarkPost(postId, authenticatedUserId(user));
    }

    // ==========================================
    // 4. 댓글 (Comment) API
    // ==========================================

    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponseDto> getComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long userId
    ) {
        return boardService.getComments(postId, userId);
    }

    @PostMapping("/posts/{postId}/comments")
    public CommentResponseDto createComment(
            @PathVariable Long postId,
            @RequestParam(required = false) Long userId,
            @RequestBody CommentRequestDto request
    ) {
        return boardService.createComment(postId, request, userId);
    }

    @GetMapping("/posts/{postId}/comments/{commentId}")
    public CommentResponseDto getCommentDetail(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long userId
    ) {
        return boardService.getCommentDetail(commentId, userId);
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public void deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam Long userId
    ) {
        boardService.deleteComment(postId, commentId, userId);
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/like")
    public CommentResponseDto likeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam Long userId
    ) {
        return boardService.likeComment(commentId, userId);
    }

    @GetMapping("/boards/bookmarks")
    public List<Long> getBookmarkedBoardIds(@AuthenticationPrincipal CustomUserDetails user) {
        return boardService.getBookmarkedBoardIds(authenticatedUserId(user));
    }

    @PutMapping("/boards/{boardId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addBoardBookmark(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        boardService.addBoardBookmark(boardId, authenticatedUserId(user));
    }

    @DeleteMapping("/boards/{boardId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBoardBookmark(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        boardService.removeBoardBookmark(boardId, authenticatedUserId(user));
    }

    private Long authenticatedUserId(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
    }
}   