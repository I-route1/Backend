package com.i_route.backend.gps.domain.attendance.service;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.user.repository.AcademyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class AttendanceRecordAccess {
    private final StudentRepository students;
    private final AcademyRepository academies;
    private final BusRepository buses;

    public void requireStudent(Long studentId) {
        positive(studentId);
        var user = currentUser();
        // 없는 학생과 소속 밖 학생을 구분하지 않아 존재 여부를 노출하지 않는다.
        var student = students.findById(studentId).orElseThrow(this::denied);
        if (role(user, "ADMIN")) return;
        if (role(user, "PARENT") && user.getId().equals(student.getParentId())) return;
        if (role(user, "ACADEMY") && academies.findByUser_Id(user.getId())
                .map(a -> a.getAcademyId().equals(student.getAcademyId())).orElse(false)) return;
        if (role(user, "DRIVER") && assignedBus(user, student.getBusId())) return;
        throw denied();
    }

    public void requireParent(Long parentId) {
        positive(parentId);
        var user = currentUser();
        if (role(user, "ADMIN") || (role(user, "PARENT") && user.getId().equals(parentId))) return;
        throw denied();
    }

    /** 학원에는 소속 학생 필터를 반환한다. 관리자/배정 기사는 해당 버스 전체 조회가 가능하다. */
    public Long requireBusScope(Long busId) {
        positive(busId);
        var user = currentUser();
        if (role(user, "ADMIN")) return null;
        if (role(user, "DRIVER") && assignedBus(user, busId)) return null;
        if (role(user, "ACADEMY")) return academies.findByUser_Id(user.getId())
                .map(a -> a.getAcademyId()).orElseThrow(this::denied);
        throw denied();
    }

    private boolean assignedBus(CustomUserDetails user, Long busId) {
        return busId != null && buses.findById(busId)
                .map(bus -> bus.getDriver() != null && user.getId().equals(bus.getDriver().getUserId()))
                .orElse(false);
    }

    private void positive(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("ID는 양수여야 합니다.");
    }

    private CustomUserDetails currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return user;
    }

    private boolean role(CustomUserDetails user, String role) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private AccessDeniedException denied() { return new AccessDeniedException("접근 권한이 없습니다."); }
}
