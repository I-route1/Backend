package com.i_route.backend.gps.domain.gps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long busId;

    private Double latitude;

    private Double longitude;

    private Double speed;

    private Double heading;

    private LocalDateTime recordedAt;
}
