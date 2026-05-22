package com.i_route.backend.dto;

import lombok.Data;

@Data
public class AiReportResponse {
    private String studentId;
    private String reportHtml; // LLaMA가 작성한 마크다운 리포트 본문
}