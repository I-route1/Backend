package com.i_route.backend.domain.route.entity;

import jakarta.persistence.*;
import lombok.*;

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
}