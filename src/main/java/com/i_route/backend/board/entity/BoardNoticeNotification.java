package com.i_route.backend.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_notice_notification", uniqueConstraints =
@UniqueConstraint(columnNames = {"recipient_id", "post_id"}))
public class BoardNoticeNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    public enum Kind { ACADEMY_NOTICE, ACADEMY_POST, POST_LIKE, POST_COMMENT }

    public BoardNoticeNotification(Long recipientId, Long postId, String title, Kind kind) {
        this.recipientId = recipientId;
        this.postId = postId;
        refresh(title, kind);
    }

    public Kind notificationKind() {
        return kind == null ? Kind.ACADEMY_NOTICE : kind;
    }

    public void refresh(String title, Kind kind) {
        this.title = title;
        this.kind = kind;
        this.createdAt = Instant.now();
        this.readAt = null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}