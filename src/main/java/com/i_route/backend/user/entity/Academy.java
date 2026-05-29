package com.i_route.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "academy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Academy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long academyId;

    // 학원명
    @Column(nullable = false)
    private String academyName;

    // 학원 주소
    @Column(nullable = false)
    private String academyAddress;

    // 사업자 번호
    @Column(nullable = false, unique = true)
    private String businessNumber;

    // 회원과 연결
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
