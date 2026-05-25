package com.i_route.backend.board.controller;

import com.i_route.backend.board.dto.PostDto;
import com.i_route.backend.board.service.PostService;
import com.i_route.backend.global.security.CustomUserDetails;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/boards/{boardId}/posts")
    public ResponseEntity<Page<PostDto.ListResponse>> getPosts(
            @PathVariable Long boardId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.getPostsByBoard(boardId, pageable));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostDto.DetailResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(postService.getPost(postId, extractUserId(userDetails)));
    }

    @GetMapping("/posts/search")
    public ResponseEntity<Page<PostDto.ListResponse>> searchPosts(
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(postService.searchPosts(keyword, pageable));
    }

    @PostMapping("/boards/{boardId}/posts")
    public ResponseEntity<PostDto.ListResponse> createPost(
            @PathVariable Long boardId,
            @Valid @RequestBody PostDto.Request request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(boardId, request, extractUserId(userDetails)));
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<PostDto.ListResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostDto.Request request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(postService.updatePost(postId, request, extractUserId(userDetails)));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        postService.deletePost(postId, extractUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<String> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(postService.toggleLike(postId, extractUserId(userDetails)));
    }

    @PostMapping("/posts/{postId}/bookmark")
    public ResponseEntity<String> toggleBookmark(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(postService.toggleBookmark(postId, extractUserId(userDetails)));
    }

    @GetMapping("/me/bookmarks")
    public ResponseEntity<List<PostDto.ListResponse>> getMyBookmarks(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(postService.getMyBookmarks(extractUserId(userDetails)));
    }

    private Long extractUserId(UserDetails userDetails) {
        return ((CustomUserDetails) userDetails).getId();
    }
}
