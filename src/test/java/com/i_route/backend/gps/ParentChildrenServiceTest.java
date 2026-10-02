package com.i_route.backend.gps;

import com.i_route.backend.gps.domain.attendance.dto.ChildResponse;
import com.i_route.backend.gps.domain.attendance.service.AttendanceService;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ParentChildrenServiceTest {
    @Mock private StudentRepository studentRepository;
    @InjectMocks private AttendanceService attendanceService;

    @Test
    void filtersByParentInDatabaseAndPreservesStudentIdentifiers() {
        given(studentRepository.findByParentId(14L)).willReturn(List.of(Student.builder()
                .studentId(7L).parentId(14L).gradeStudentId("S-0155").name("child").busId(1L).build()));

        List<ChildResponse> children = attendanceService.getChildren(14L);

        assertThat(children).hasSize(1);
        assertThat(children.get(0).getGpsStudentId()).isEqualTo(7L);
        assertThat(children.get(0).getGradeStudentId()).isEqualTo("S-0155");
        assertThat(children.get(0).getName()).isEqualTo("child");
        assertThat(children.get(0).getBusId()).isEqualTo(1L);
        verifyNoMoreInteractions(studentRepository);
    }

    @Test
    void parentWithoutChildrenReceivesEmptyList() {
        given(studentRepository.findByParentId(14L)).willReturn(List.of());
        assertThat(attendanceService.getChildren(14L)).isEmpty();
    }
}
