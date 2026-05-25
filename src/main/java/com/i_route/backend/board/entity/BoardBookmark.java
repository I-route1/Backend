package com.i_route.backend.board.entity;

import com.i_route.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "board_id"}))
@Getter @Setter
@NoArgsConstructor
public class BoardBookmark {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
}

