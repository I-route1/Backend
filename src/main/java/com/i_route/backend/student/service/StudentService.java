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

import java.util.List;
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
        Long academyId = null;

        // 학원 코드(InviteCode)로 학원 조회
        if (request.getAcademies() != null && !request.getAcademies().isEmpty()) {
            String academyCode = request.getAcademies().get(0).getCode();
            if (academyCode != null && !academyCode.isBlank()) {
                academyId = getAcademyByCode(academyCode).getAcademyId();
            }
        }

        // 기존 Student 엔티티 변경 없이 builder 그대로 활용 (grade 저장 추가)
        Student student = Student.builder()
                .name(request.getName())
                .grade(request.getGrade()) // 👈 학년 매핑 추가
                .parentId(parentId)
                .academyId(academyId)     // 👈 학원 ID 매핑
                .build();

        Student saved = studentRepository.save(student);

        if (saved.getAcademyId() != null) {
            boardService.addAcademyBoardBookmarks(saved.getAcademyId(), parentId);
            Academy academy = academyRepository.findById(saved.getAcademyId()).orElse(null);
            String academyName = academy != null ? academy.getAcademyName() : null;
            String academyCode = academy != null ? academy.getInviteCode() : null;
            return StudentResponse.from(saved, academyName, academyCode);
        }

        return StudentResponse.from(saved);
    }

    @Transactional
    public StudentResponse updateChildAcademy(Long parentId, Long studentId, String code) {
        Student student = studentRepository.findById(studentId)
                .filter(child -> parentId.equals(child.getParentId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "자녀 정보를 찾을 수 없습니다."));

        if (code == null || code.isBlank()) {
            student.changeAcademy(null);
            return StudentResponse.from(student);
        }

        Academy academy = getAcademyByCode(code);
        student.changeAcademy(academy.getAcademyId());
        boardService.addAcademyBoardBookmarks(academy.getAcademyId(), parentId);
        return StudentResponse.from(student, academy.getAcademyName(), academy.getInviteCode());
    }

    private Academy getAcademyByCode(String code) {
        return academyRepository.findByInviteCode(code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록된 학원 코드가 아닙니다."));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getMyChildren(Long parentId) {
        return studentRepository.findByParentId(parentId).stream()
                .map(student -> {
                    if (student.getAcademyId() != null) {
                        Academy academy = academyRepository.findById(student.getAcademyId()).orElse(null);
                        String academyName = academy != null ? academy.getAcademyName() : null;
                        String academyCode = academy != null ? academy.getInviteCode() : null;
                        return StudentResponse.from(student, academyName, academyCode);
                    }
                    return StudentResponse.from(student);
                })
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