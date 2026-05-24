package com.i_route.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    private Long subjectId;

    private String conceptTag;

    @Column(length = 2000)
    private String content;

    private Integer difficulty;
}
