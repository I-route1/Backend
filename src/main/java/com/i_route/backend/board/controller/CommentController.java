package com.i_route.backend.board.controller;

import com.i_route.backend.board.dto.CommentDto;
import com.i_route.backend.board.service.CommentService;
import com.i_route.backend.global.security.CustomUserDetails;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<Page<CommentDto.Response>> getComments(
            @PathVariable Long postId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = ((CustomUserDetails) userDetails).getId();

        return ResponseEntity.ok(
                commentService.getComments(postId, userId, pageable)
        );
    }

    // =========================
    // 댓글 작성
    // =========================
    @PostMapping
    public ResponseEntity<CommentDto.Response> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentDto.Request request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = ((CustomUserDetails) userDetails).getId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(postId, request, userId));
    }

    // =========================
    // 댓글 삭제
    // =========================
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = ((CustomUserDetails) userDetails).getId();

        commentService.deleteComment(commentId, userId);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // 댓글 좋아요 (추가)
    // =========================
    @PostMapping("/{commentId}/like")
    public ResponseEntity<String> toggleCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = ((CustomUserDetails) userDetails).getId();

        return ResponseEntity.ok(
                commentService.toggleCommentLike(commentId, userId)
        );
    }
}