package com.i_route.backend.controller;

import com.i_route.backend.dto.AiDto;
import com.i_route.backend.service.MathAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class MathAiController {

    private final MathAiService mathAiService;

    /**
     * 리액트(Frontend)에서 수학 질문을 보낼 통로
     * 주소: POST http://localhost:8080/api/ai/math
     */
    @PostMapping("/math")
    public Mono<String> askMathQuestion(@RequestBody AiDto.Request request) {
        return mathAiService.getMathExplanation(request.getQuestion());
    }
}