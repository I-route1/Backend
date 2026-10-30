package com.i_route.backend.gps;

import com.i_route.backend.global.config.AiServerKeyFilter;
import com.i_route.backend.global.config.SecurityConfig;
import com.i_route.backend.global.jwt.JwtAuthenticationFilter;
import com.i_route.backend.global.jwt.JwtUtil;
import com.i_route.backend.gps.domain.attendance.controller.AttendanceController;
import com.i_route.backend.gps.domain.attendance.dto.ChildResponse;
import com.i_route.backend.gps.domain.attendance.service.AttendanceService;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AiServerKeyFilter.class})
class ParentChildrenApiTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AttendanceService attendanceService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserRepository userRepository;

    private void authenticate(Long id, User.UserRole role) {
        given(jwtUtil.validateToken("test-token")).willReturn(true);
        given(jwtUtil.getUserId("test-token")).willReturn(id);
        given(userRepository.findById(id)).willReturn(Optional.of(User.builder()
                .id(id).nickname("parent").role(role).build()));
    }

    @Test
    void anonymousCannotListChildren() throws Exception {
        mvc.perform(get("/api/gps/parents/14/children")).andExpect(status().isForbidden());
        verifyNoInteractions(attendanceService);
    }

    @Test
    void parentCannotListAnotherParentsChildren() throws Exception {
        authenticate(14L, User.UserRole.PARENT);
        mvc.perform(get("/api/gps/parents/15/children")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(attendanceService);
    }

    @Test
    void anotherRoleCannotUseParentEndpoint() throws Exception {
        authenticate(14L, User.UserRole.DRIVER);
        mvc.perform(get("/api/gps/parents/14/children")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(attendanceService);
    }

    @Test
    void parentReceivesOwnChildrenWithOriginalResponseFields() throws Exception {
        authenticate(14L, User.UserRole.PARENT);
        given(attendanceService.getChildren(14L)).willReturn(List.of(ChildResponse.builder()
                .gpsStudentId(7L).gradeStudentId("S-0155").name("child").busId(1L).build()));
        mvc.perform(get("/api/gps/parents/14/children")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gpsStudentId").value(7))
                .andExpect(jsonPath("$[0].gradeStudentId").value("S-0155"))
                .andExpect(jsonPath("$[0].name").value("child"))
                .andExpect(jsonPath("$[0].busId").value(1));
    }

    @Test
    void parentWithoutChildrenReceivesEmptyArray() throws Exception {
        authenticate(14L, User.UserRole.PARENT);
        given(attendanceService.getChildren(14L)).willReturn(List.of());
        mvc.perform(get("/api/gps/parents/14/children")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
