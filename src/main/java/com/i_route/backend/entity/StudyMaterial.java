package com.i_route.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_material")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialId;

    private String title;

    private String materialType;  // 교재 / 인강

    private Integer level;

    private Long subjectId;
}
