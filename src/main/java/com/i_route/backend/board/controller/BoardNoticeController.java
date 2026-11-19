package com.i_route.backend.board.controller;

import com.i_route.backend.board.service.BoardNoticeService;
import com.i_route.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/board-notifications")
@RequiredArgsConstructor
public class BoardNoticeController {

    private final BoardNoticeService notices;

    @GetMapping
    public List<BoardNoticeService.NoticeItem> list(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return notices.list(user.getId());
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        notices.markRead(user.getId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        notices.delete(user.getId(), id);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal CustomUserDetails user) {
        return notices.subscribe(user.getId());
    }
}