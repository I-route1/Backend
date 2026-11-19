package com.i_route.backend.message;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {
    private static final int PAGE_SIZE = 50;

    private final DirectMessageRepository messages;
    private final UserRepository users;

    @GetMapping("/users")
    public List<Contact> findContacts(@AuthenticationPrincipal CustomUserDetails user,
                                      @RequestParam String nickname) {
        Long me = accountId(user);
        String query = nickname.strip();
        if (query.isEmpty()) return List.of();
        return users.findTop20ByNicknameContainingIgnoreCaseAndIdNotOrderByNicknameAsc(query, me)
                .stream().map(this::contact).toList();
    }

    @GetMapping("/unread")
    public Map<String, Long> unread(@AuthenticationPrincipal CustomUserDetails user) {
        return Map.of("count", messages.countByRecipientIdAndReadAtIsNull(accountId(user)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Conversation> conversations(@AuthenticationPrincipal CustomUserDetails user) {
        Long me = accountId(user);
        var latest = messages.latestConversations(me);
        var peerIds = latest.stream().map(message -> peerId(message, me)).distinct().toList();
        Map<Long, Contact> contacts = new HashMap<>();
        users.findAllById(peerIds).forEach(peer -> contacts.put(peer.getId(), contact(peer)));
        Map<Long, Long> unread = new HashMap<>();
        messages.unreadBySender(me).forEach(row -> unread.put(row.getSenderId(), row.getTotal()));

        return latest.stream().map(message -> {
            Long peer = peerId(message, me);
            Contact contact = contacts.get(peer);
            return contact == null ? null
                    : new Conversation(contact, item(message), unread.getOrDefault(peer, 0L));
        }).filter(Objects::nonNull).toList();
    }

    @GetMapping("/{peerId}")
    @Transactional(readOnly = true)
    public History history(@AuthenticationPrincipal CustomUserDetails user,
                           @PathVariable Long peerId,
                           @RequestParam(required = false) Long before) {
        Long me = accountId(user);
        Contact peer = contact(requirePeer(me, peerId));
        var page = messages.history(me, peerId, before == null ? Long.MAX_VALUE : before,
                PageRequest.of(0, PAGE_SIZE + 1));
        var items = page.stream().limit(PAGE_SIZE).map(this::item).toList();
        return new History(peer, items, page.size() > PAGE_SIZE ? items.get(PAGE_SIZE - 1).id() : null);
    }

    @PostMapping("/{peerId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public MessageItem send(@AuthenticationPrincipal CustomUserDetails user,
                            @PathVariable Long peerId, @Valid @RequestBody SendRequest request) {
        Long me = accountId(user);
        requirePeer(me, peerId);
        return item(messages.save(new DirectMessage(me, peerId, request.content().strip())));
    }

    @PatchMapping("/{peerId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void markRead(@AuthenticationPrincipal CustomUserDetails user,
                         @PathVariable Long peerId, @Valid @RequestBody ReadRequest request) {
        Long me = accountId(user);
        requirePeer(me, peerId);
        messages.markRead(me, peerId, request.through(), Instant.now());
    }

    private Long accountId(CustomUserDetails user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user.getId();
    }

    private User requirePeer(Long me, Long peerId) {
        if (me.equals(peerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자신에게 메시지를 보낼 수 없습니다.");
        }
        return users.findById(peerId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Long peerId(DirectMessage message, Long me) {
        return message.getSenderId().equals(me) ? message.getRecipientId() : message.getSenderId();
    }

    private Contact contact(User user) {
        return new Contact(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    private MessageItem item(DirectMessage message) {
        return new MessageItem(message.getId(), message.getSenderId(), message.getContent(),
                message.getCreatedAt(), message.getReadAt());
    }

    public record Contact(Long id, String nickname, String profileImageUrl) {}
    public record MessageItem(Long id, Long senderId, String content, Instant createdAt, Instant readAt) {}
    public record Conversation(Contact peer, MessageItem latest, long unreadCount) {}
    public record History(Contact peer, List<MessageItem> messages, Long nextBefore) {}
    public record SendRequest(@NotBlank @Size(max = 2000) String content) {}
    public record ReadRequest(@Positive long through) {}
}
