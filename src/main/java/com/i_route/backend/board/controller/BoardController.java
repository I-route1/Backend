package com.i_route.backend.domain.board.controller;


import com.i_route.backend.domain.board.dto.BoardDto;
import com.i_route.backend.domain.board.service.BoardService;
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
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<List<BoardDto.Response>> getBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BoardDto.Response>> searchBoards(@RequestParam String keyword) {
        return ResponseEntity.ok(boardService.searchBoards(keyword));
    }

    @PostMapping
    public ResponseEntity<BoardDto.Response> createBoard(
            @Valid @RequestBody BoardDto.Request request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(request, userId));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<BoardDto.Response> updateBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardDto.Request request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(boardService.updateBoard(boardId, request, extractUserId(userDetails)));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boardService.deleteBoard(boardId, extractUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    // 게시판 상세 조회 (게시글 목록 포함)
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDto.DetailResponse> getBoardDetail(
            @PathVariable Long boardId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(boardService.getBoardDetail(boardId, extractUserId(userDetails), pageable));
    }

    // 게시판 즐겨찾기 토글
    @PostMapping("/{boardId}/bookmark")
    public ResponseEntity<String> toggleBoardBookmark(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(boardService.toggleBoardBookmark(boardId, extractUserId(userDetails)));
    }

    // 내 게시판 즐겨찾기 목록
    @GetMapping("/bookmarks/me")
    public ResponseEntity<List<BoardDto.Response>> getMyBoardBookmarks(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(boardService.getMyBoardBookmarks(extractUserId(userDetails)));
    }

    private Long extractUserId(UserDetails userDetails) {
        // 로그인 구현 방식에 따라 userId 추출 로직 조정
        return ((CustomUserDetails) userDetails).getId();
    }
}

