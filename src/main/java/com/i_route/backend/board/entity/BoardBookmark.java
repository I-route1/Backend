package com.i_route.backend.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_bookmark", uniqueConstraints =
@UniqueConstraint(columnNames = {"board_id", "user_id"}))
public class BoardBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public BoardBookmark(Long boardId, Long userId) {
        this.boardId = boardId;
        this.userId = userId;
    }
}