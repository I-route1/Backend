package com.i_route.backend.gps.domain.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "fcm_tokens")
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    public FcmToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public void updateUserId(Long userId) {
        this.userId = userId;
    }
}
