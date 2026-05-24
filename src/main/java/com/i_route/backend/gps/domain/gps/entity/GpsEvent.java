package com.i_route.backend.gps.domain.gps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long busId;

    @Enumerated(EnumType.STRING)
    private GpsEventType eventType;

    private String message;

    private LocalDateTime createdAt;
}
