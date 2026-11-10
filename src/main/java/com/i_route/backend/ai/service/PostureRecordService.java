package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.PostureRecordRequest;
import com.i_route.backend.ai.dto.PostureRecordResponse;
import com.i_route.backend.ai.entity.PostureRecord;
import com.i_route.backend.ai.repository.PostureRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service @RequiredArgsConstructor
public class PostureRecordService {
    private final PostureRecordRepository records;
    private final StudentRecordLock studentLock;

    @Transactional
    public PostureRecordResponse save(PostureRecordRequest request) {
        studentLock.lock(request.getStudentId());
        var started = request.getStartedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        var candidate = PostureRecord.builder().studentId(request.getStudentId()).submissionId(request.getSubmissionId())
                .subject(request.getSubject().strip()).startedAt(started)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS))
                .durationSeconds(request.getDurationSeconds()).inputSource(request.getInputSource()).modelName(request.getModelName())
                .totalSamples(request.getTotalSamples()).uprightSamples(request.getUprightSamples()).slouchedSamples(request.getSlouchedSamples())
                .headDownSamples(request.getHeadDownSamples()).tiltedSamples(request.getTiltedSamples()).unknownSamples(request.getUnknownSamples())
                .noPersonSamples(request.getNoPersonSamples()).multiplePeopleSamples(request.getMultiplePeopleSamples()).build();
        var existing = records.findByStudentIdAndSubmissionId(request.getStudentId(), request.getSubmissionId());
        if (existing.isPresent()) {
            // Compare canonical content, excluding generated IDs and creation time.
            var a = PostureRecordResponse.from(existing.get());
            var b = PostureRecordResponse.from(candidate);
            boolean same = a.subject().equals(b.subject()) && a.startedAt().equals(b.startedAt())
                    && a.durationSeconds() == b.durationSeconds() && a.inputSource() == b.inputSource()
                    && a.modelName().equals(b.modelName()) && a.totalSamples() == b.totalSamples()
                    && a.uprightSamples() == b.uprightSamples() && a.slouchedSamples() == b.slouchedSamples()
                    && a.headDownSamples() == b.headDownSamples() && a.tiltedSamples() == b.tiltedSamples()
                    && a.unknownSamples() == b.unknownSamples() && a.noPersonSamples() == b.noPersonSamples()
                    && a.multiplePeopleSamples() == b.multiplePeopleSamples();
            if (!same) throw new ResponseStatusException(HttpStatus.CONFLICT, "submissionId가 다른 자세 기록에 이미 사용되었습니다.");
            return a;
        }
        return PostureRecordResponse.from(records.save(candidate));
    }

    @Transactional(readOnly = true)
    public List<PostureRecordResponse> list(Long studentId, String subject, int limit) {
        var page = PageRequest.of(0, limit);
        var rows = subject == null ? records.findByStudentIdOrderByStartedAtDescIdDesc(studentId, page)
                : records.findByStudentIdAndSubjectOrderByStartedAtDescIdDesc(studentId, subject, page);
        return rows.stream().map(PostureRecordResponse::from).toList();
    }
}
