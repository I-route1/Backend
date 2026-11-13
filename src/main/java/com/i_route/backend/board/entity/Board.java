package com.i_route.backend.board.entity;

import com.i_route.backend.user.entity.Academy;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 예: "공지사항", "A학원 게시판", "자유게시판"

    private String description;

    private String createdBy;

    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardType boardType; // NOTICE, FREE, ACADEMY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id")
    private Academy academy; // 학원 게시판이 아니면 null 가능

    public void updateInfo(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.description = description != null ? description.trim() : null;
    }
}