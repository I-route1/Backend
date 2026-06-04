package com.i_route.backend.user.entity;

import com.i_route.backend.gps.domain.driver.entity.Driver;
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

    // 학원명 / 소속 학원명
    @Column(nullable = false)
    private String academyName;

    // 학원 관리자일 때만 사용
    @Column(nullable = true)
    private String academyAddress;

    // 학원 관리자일 때만 사용
    @Column(nullable = true, unique = true)
    private String businessNumber;

    // 차량 기사일 때만 사용
    @Column(nullable = true, unique = true)
    private String vehicleNumber;

    // 차량 기사일 때만 사용
    @Column(nullable = true, unique = true)
    private String inviteCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 학원의 기사 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;
}