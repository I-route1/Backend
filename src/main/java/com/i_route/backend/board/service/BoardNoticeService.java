package com.i_route.backend.board.service;

import com.i_route.backend.board.entity.BoardNoticeNotification;
import com.i_route.backend.board.entity.BoardNoticeNotification.Kind;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.repository.BoardBookmarkRepository;
import com.i_route.backend.board.repository.BoardNoticeNotificationRepository;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BoardNoticeService {

    private final BoardBookmarkRepository bookmarks;
    private final BoardNoticeNotificationRepository notifications;
    private final StudentRepository students;
    private final ApplicationEventPublisher events;

    private final ConcurrentHashMap<Long, Set<SseEmitter>> streams =
            new ConcurrentHashMap<>();

    @Transactional
    public void saveFor(Post post) {
        Long boardId = post.getBoard().getId();
        Long academyId = post.getBoard().getAcademy().getAcademyId();
        Set<Long> recipients = new HashSet<>(bookmarks.findUserIdsByBoardId(boardId));

        recipients.addAll(students.findEnrolledParentIds(academyId));

        recipients.removeAll(notifications.findRecipientIdsByPostId(post.getId()));
        if (recipients.isEmpty()) {
            return;
        }

        notifications.saveAll(recipients.stream()
                .map(id -> new BoardNoticeNotification(id, post.getId(), post.getTitle(), Kind.ACADEMY_NOTICE))
                .toList());

        events.publishEvent(new NoticeSaved(Set.copyOf(recipients)));
    }

    @Transactional
    public void notifyAcademyManager(Post post) {
        var academy = post.getBoard().getAcademy();
        User manager = academy == null ? null : academy.getUser();
        if (manager == null || manager.getRole() != User.UserRole.ACADEMY
                || manager.getId().equals(post.getUser().getId())) {
            return;
        }
        saveActivity(post, manager.getId(), Kind.ACADEMY_POST);
    }

    @Transactional
    public void notifyPostAuthor(Post post, Long actorId, Kind kind) {
        User author = post.getUser();
        if (author == null || actorId.equals(author.getId())) {
            return;
        }
        saveActivity(post, author.getId(), kind);
    }

    private void saveActivity(Post post, Long recipientId, Kind kind) {
        var existing = notifications.findByRecipientIdAndPostId(recipientId, post.getId());
        if (existing.isPresent()) {
            existing.get().refresh(post.getTitle(), kind);
        } else {
            notifications.save(new BoardNoticeNotification(recipientId, post.getId(), post.getTitle(), kind));
        }
        events.publishEvent(new NoticeSaved(Set.of(recipientId)));
    }

    @Transactional(readOnly = true)
    public List<NoticeItem> list(Long userId) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NoticeItem(
                        n.getId(), n.getPostId(), n.getTitle(),
                        n.getCreatedAt(), n.getReadAt(), n.notificationKind()))
                .toList();
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notifications.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
                .markRead();
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        notifications.findByIdAndRecipientId(notificationId, userId)
                .ifPresent(notifications::delete);
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        Runnable remove = () -> removeStream(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());

        streams.compute(userId, (id, current) -> {
            Set<SseEmitter> connections =
                    current == null ? ConcurrentHashMap.newKeySet() : current;
            connections.add(emitter);
            return connections;
        });

        send(userId, emitter, "ready");
        return emitter;
    }

    @TransactionalEventListener
    public void notifyConnectedUsers(NoticeSaved event) {
        for (Long userId : event.recipientIds()) {
            for (SseEmitter emitter : streams.getOrDefault(userId, Set.of())) {
                send(userId, emitter, "board-notice");
            }
        }
    }

    private void send(Long userId, SseEmitter emitter, String eventName) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data("refresh"));
        } catch (IOException | IllegalStateException error) {
            removeStream(userId, emitter);
            emitter.completeWithError(error);
        }
    }

    private void removeStream(Long userId, SseEmitter emitter) {
        streams.computeIfPresent(userId, (id, current) -> {
            current.remove(emitter);
            return current.isEmpty() ? null : current;
        });
    }

    public record NoticeSaved(Set<Long> recipientIds) {}

    public record NoticeItem(
            Long id, Long postId, String title, Instant createdAt, Instant readAt, Kind kind) {}
}