package com.i_route.backend.domain.board.entity;

import com.i_route.backend.domain.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comment_likes")
@Getter @Setter
@NoArgsConstructor
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}