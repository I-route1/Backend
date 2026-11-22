package com.i_route.backend.student.service;

import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.student.dto.ChildCreateRequest;
import com.i_route.backend.student.dto.StudentResponse;
import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.board.service.BoardService;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.AcademyRepository;
import com.i_route.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final AcademyRepository academyRepository;
    private final BoardService boardService;
    private final UserRepository userRepository;

    // 자녀 추가 및 저장 (ChildCreateRequest 활용)
    @Transactional
    public StudentResponse addChild(Long parentId, ChildCreateRequest request) {
        List<String> codes = request.getAcademies() == null ? List.of() : request.getAcademies().stream()
                .filter(Objects::nonNull)
                .map(ChildCreateRequest.AcademyInfo::getCode)
                .toList();
        List<Long> academyIds = resolveAcademyIds(codes);
        Student student = Student.builder()
                .name(request.getName())
                .grade(request.getGrade())
                .parentId(parentId)
                .academyId(academyIds.isEmpty() ? null : academyIds.get(0))
                .build();
        student.replaceAcademyEnrollments(Set.copyOf(academyIds));
        Student saved = studentRepository.save(student);

        academyIds.stream().sorted().forEach(id -> boardService.addAcademyBoardBookmarks(id, parentId));
        return withEnrollments(saved);
    }

    @Transactional
    public StudentResponse updateChildAcademies(Long parentId, Long studentId,
                                                List<String> addCodes, List<Long> removeAcademyIds) {
        Student student = studentRepository.findLockedByStudentId(studentId)
                .filter(child -> parentId.equals(child.getParentId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "자녀 정보를 찾을 수 없습니다."));

        List<Long> addedIds = resolveAcademyIds(addCodes);
        Set<Long> previous = student.academyEnrollmentIds();
        Set<Long> next = new HashSet<>(previous);
        if (removeAcademyIds != null) {
            next.removeAll(removeAcademyIds);
        }
        next.addAll(addedIds);
        if (!next.equals(previous)) {
            student.replaceAcademyEnrollments(next);
            addedIds.stream().filter(id -> !previous.contains(id)).sorted()
                    .forEach(id -> boardService.addAcademyBoardBookmarks(id, parentId));
        }
        return withEnrollments(student);
    }

    private List<Long> resolveAcademyIds(List<String> codes) {
        return codes == null ? List.of() : codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .map(code -> getAcademyByCode(code).getAcademyId())
                .distinct()
                .toList();
    }

    private StudentResponse withEnrollments(Student student) {
        var academies = academyRepository.findAllById(student.academyEnrollmentIds()).stream()
                .sorted(Comparator.comparing((Academy a) -> !Objects.equals(a.getAcademyId(), student.getAcademyId()))
                        .thenComparing(Academy::getAcademyId))
                .map(academy -> StudentResponse.AcademyInfo.builder()
                        .id(academy.getAcademyId())
                        .name(academy.getAcademyName())
                        .code(academy.getInviteCode())
                        .build())
                .toList();
        return StudentResponse.from(student, academies);
    }

    private Academy getAcademyByCode(String code) {
        return academyRepository.findByInviteCode(code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록된 학원 코드가 아닙니다."));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getMyChildren(Long parentId) {
        return studentRepository.findByParentId(parentId).stream()
                .map(this::withEnrollments)
                .collect(Collectors.toList());
    }

    // 💡 자녀 삭제 메서드 (추가)
    @Transactional
    public void deleteStudent(Long parentId, Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("자녀 정보를 찾을 수 없습니다."));

        if (!student.getParentId().equals(parentId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        studentRepository.delete(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAcademyStudents(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }

        Academy academy = academyRepository.findByUser(user).orElse(null);
        if (academy == null) {
            return List.of();
        }

        Long academyId = academy.getAcademyId();

        return studentRepository.findByAcademyId(academyId).stream()
                .map(StudentResponse::from)
                .collect(Collectors.toList());
    }
}