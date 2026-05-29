package com.i_route.backend.payment.dto.request;

import com.i_route.backend.payment.entity.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentOrderRequest {

    @NotNull
    private PlanType planType;
}
