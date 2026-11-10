package com.i_route.backend.ai.dto;

import com.i_route.backend.ai.entity.PostureInputSource;
import com.i_route.backend.ai.entity.PostureRecord;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
public record PostureRecordResponse(Long id, Long studentId, String submissionId, String subject,
        OffsetDateTime startedAt, OffsetDateTime createdAt, int durationSeconds,
        String measurementSource, PostureInputSource inputSource, String modelName,
        int totalSamples, int classifiedSamples, int uprightSamples, int slouchedSamples,
        int headDownSamples, int tiltedSamples, int unknownSamples, int noPersonSamples, int multiplePeopleSamples,
        Double uprightRatio, Double slouchedRatio, Double headDownRatio, Double tiltedRatio) {
    public static PostureRecordResponse from(PostureRecord record) {
        int classified = record.getUprightSamples() + record.getSlouchedSamples()
                + record.getHeadDownSamples() + record.getTiltedSamples();
        return new PostureRecordResponse(record.getId(), record.getStudentId(), record.getSubmissionId(), record.getSubject(),
                record.getStartedAt().atOffset(ZoneOffset.UTC), record.getCreatedAt().atOffset(ZoneOffset.UTC),
                record.getDurationSeconds(), "AI_HAT", record.getInputSource(), record.getModelName(),
                record.getTotalSamples(), classified, record.getUprightSamples(), record.getSlouchedSamples(),
                record.getHeadDownSamples(), record.getTiltedSamples(), record.getUnknownSamples(),
                record.getNoPersonSamples(), record.getMultiplePeopleSamples(),
                ratio(record.getUprightSamples(), classified), ratio(record.getSlouchedSamples(), classified),
                ratio(record.getHeadDownSamples(), classified), ratio(record.getTiltedSamples(), classified));
    }
    private static Double ratio(int count, int classified) { return classified == 0 ? null : (double) count / classified; }
}
