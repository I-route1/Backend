package com.i_route.backend.payment.dto.request;

import com.i_route.backend.payment.entity.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BillingIssueRequest {

    @NotBlank
    private String authKey;     // Toss 카드 등록 후 받은 인증키

    @NotNull
    private PlanType planType;  // MONTHLY_BASIC 또는 MONTHLY_PREMIUM
}
