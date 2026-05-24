package com.i_route.backend.gps.domain.bus.entity;


import com.i_route.backend.gps.domain.driver.entity.Driver;
import com.i_route.backend.gps.domain.route.entity.Route;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String busNumber;

    @Enumerated(EnumType.STRING)
    private OperationStatus operationStatus;

    private LocalDateTime operationStartedAt;

    private LocalDateTime operationEndedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    private Route route;

    public boolean isOperating() {
        return this.operationStatus == OperationStatus.OPERATING;
    }
}
