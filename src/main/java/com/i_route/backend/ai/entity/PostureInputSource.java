package com.i_route.backend.ai.entity;

/** Input provenance prevents replayed test media from being presented as a live camera session. */
public enum PostureInputSource {
    CAMERA, IMAGE, VIDEO;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static PostureInputSource fromJson(com.fasterxml.jackson.databind.JsonNode value) {
        if (!value.isTextual()) throw new IllegalArgumentException("inputSource는 문자열이어야 합니다.");
        return valueOf(value.textValue());
    }
}
