package com.i_route.backend.board.controller;

import com.i_route.backend.board.dto.*;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/boards")
    public List<BoardResponseDto> getBoards() {
        return boardService.getBoards()
                .stream()
                .map(BoardResponseDto::from)
                .toList();
    }

    @GetMapping("/boards/search")
    public List<Board> searchBoards(@RequestParam String keyword) {
        return boardService.searchBoards(keyword);
    }

    @PostMapping("/boards")
    public Board createBoard(@RequestBody BoardRequestDto request) {
        return boardService.createBoard(request);
    }

    @PutMapping("/boards/{boardId}")
    public Board updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardRequestDto request
    ) {
        return boardService.updateBoard(boardId, request);
    }

    @DeleteMapping("/boards/{boardId}")
    public void deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
    }

    @GetMapping("/boards/{boardId}")
    public BoardResponseDto getBoardDetail(@PathVariable Long boardId) {
        return BoardResponseDto.from(boardService.getBoardDetail(boardId));
    }

    @GetMapping("/posts")
    public List<PostResponseDto> getPosts(
            @RequestParam(required = false) Long userId
    ) {
        return boardService.getPosts(userId);
    }

    @PostMapping("/posts")
    public PostResponseDto createPost(
            @RequestParam(required = false) Long userId,
            @RequestBody PostRequestDto request
    ) {
        return boardService.createPost(request, userId);
    }
    @GetMapping("/posts/{postId}")
    public PostResponseDto getPostDetail(
            @PathVariable Long postId,
            @RequestParam(required = false) Long userId
    ) {
        return boardService.getPostDetail(postId, userId);
    }

    @GetMapping("/posts/search")
    public List<PostResponseDto> searchPosts(
            @RequestParam String keyword,
            @RequestParam(required = false) Long userId
    ) {
        return boardService.searchPosts(keyword, userId);
    }

    @PutMapping("/posts/{postId}")
    public PostResponseDto updatePost(
            @PathVariable Long postId,
            @RequestParam(required = false) Long userId,
            @RequestBody PostRequestDto request
    ) {
        return boardService.updatePost(postId, request, userId);
    }

    @DeleteMapping("/posts/{postId}")
    public void deletePost(@PathVariable Long postId) {
        boardService.deletePost(postId);
    }

    @PostMapping("/posts/{postId}/like")
    public PostResponseDto likePost(
            @PathVariable Long postId,
            @RequestParam Long userId
    ) {
        return boardService.likePost(postId, userId);
    }

    @PostMapping("/posts/{postId}/bookmark")
    public PostResponseDto bookmarkPost(
            @PathVariable Long postId,
            @RequestParam Long userId
    ) {
        return boardService.bookmarkPost(postId, userId);
    }

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
}