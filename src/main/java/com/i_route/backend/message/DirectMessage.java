package com.i_route.backend.message;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_message", indexes = {
        @Index(columnList = "sender_id, recipient_id, id"),
        @Index(columnList = "recipient_id, sender_id, read_at")
})
public class DirectMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    public DirectMessage(Long senderId, Long recipientId, String content) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
