package com.i_route.backend.gps.domain.student.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column(name = "id")
    private Long studentId;

    private Long busId;

    private Long routeStopId;

    private String name;

    private LocalTime expectedDropOffTime;

    private Long parentId;

    private String nfcCardId;

    private String gradeStudentId; // 성적 시스템 연동용 ID (예: "S-0155")
}
