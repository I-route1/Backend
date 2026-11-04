package com.i_route.backend.ai.service;

import com.i_route.backend.ai.repository.LearningActivityRepository;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.user.repository.AcademyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StudentRecordAccess {
    private final StudentRepository students;
    private final AcademyRepository academies;
    private final LearningActivityRepository activities;

    public void requireStudent(Long studentId) {
        if (studentId == null || studentId <= 0) throw new IllegalArgumentException("studentId는 양수여야 합니다.");
        var user = currentUser();
        var student = students.findById(studentId).orElseThrow(this::denied);
        if (hasRole(user, "ADMIN")) return;
        if (hasRole(user, "PARENT") && user.getId().equals(student.getParentId())) return;
        if (hasRole(user, "ACADEMY") && academies.findByUser_Id(user.getId())
                .map(a -> a.getAcademyId().equals(student.getAcademyId())).orElse(false)) return;
        throw denied();
    }

    public void requireFeedback(Long activityId) {
        var user = currentUser();
        if (!hasRole(user, "ACADEMY") && !hasRole(user, "ADMIN")) throw denied();
        var activity = activities.findById(activityId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "학습 기록을 찾을 수 없습니다."));
        requireStudent(activity.getStudentId());
    }

    private CustomUserDetails currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return user;
    }
    private boolean hasRole(CustomUserDetails user, String role) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    private AccessDeniedException denied() { return new AccessDeniedException("접근 권한이 없습니다."); }
}
