package com.i_route.backend.gps.domain.route.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long routeId;

    private String stopName;

    private Double latitude;

    private Double longitude;

    private Integer stopOrder;

    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime scheduledArrivalTime;
}
