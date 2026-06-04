package com.i_route.backend.student.service;

import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.student.dto.ChildCreateRequest;
import com.i_route.backend.student.dto.StudentCreateRequest;
import com.i_route.backend.student.dto.StudentResponse;
import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.AcademyRepository;
import com.i_route.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final AcademyRepository academyRepository;
    private final UserRepository userRepository;

    @Transactional
    public StudentResponse addStudent(Long parentId, StudentCreateRequest request) {
        Student student = Student.builder()
                .name(request.getName())
                .parentId(parentId)
                .gradeStudentId(request.getGradeStudentId())
                .build();

        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getMyChildren(Long parentId) {
        return studentRepository.findAll().stream()
                .filter(s -> parentId.equals(s.getParentId()))
                .map(StudentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentResponse addChild(Long parentId, ChildCreateRequest request) {
        Long academyId = null;

        if (request.getAcademies() != null && !request.getAcademies().isEmpty()) {
            String academyCode = request.getAcademies().get(0).getCode();
            Academy academy = academyRepository.findByInviteCode(academyCode)
                    .orElse(null);
            if (academy != null) {
                academyId = academy.getAcademyId();
            }
        }

        Student student = Student.builder()
                .name(request.getName())
                .parentId(parentId)
                .academyId(academyId)
                .build();

        return StudentResponse.from(studentRepository.save(student));
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

        return studentRepository.findAll().stream()
                .filter(s -> academyId.equals(s.getAcademyId()))
                .map(StudentResponse::from)
                .collect(Collectors.toList());
    }
}
