package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.dto.attendance.AttendanceMarkItem;
import com.example.backend.dto.attendance.MarkClassAttendanceRequest;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StaffAttendanceRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class AttendanceServiceMarkClassAuthTest {

    private AttendanceRepository attendanceRepository;
    private SchoolClassRepository schoolClassRepository;
    private TeacherClassRepository teacherClassRepository;
    private AttendanceService service;

    @BeforeEach
    void setUp() {
        attendanceRepository = Mockito.mock(AttendanceRepository.class);
        schoolClassRepository = Mockito.mock(SchoolClassRepository.class);
        teacherClassRepository = Mockito.mock(TeacherClassRepository.class);
        AuthorizationService authorizationService = new AuthorizationService(
            teacherClassRepository,
            Mockito.mock(ParentStudentRepository.class),
            Mockito.mock(StudentRepository.class),
            schoolClassRepository);
        service = new AttendanceService(
            attendanceRepository,
            Mockito.mock(StaffAttendanceRepository.class),
            Mockito.mock(StudentRepository.class),
            Mockito.mock(TeacherRepository.class),
            schoolClassRepository,
            new ActiveTermService(null, null),
            authorizationService,
            new AuditService(null));
    }

    @Test
    void teacherCannotMarkUnassignedClass() {
        when(schoolClassRepository.existsById(7L)).thenReturn(true);
        when(teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(4L, 7L)).thenReturn(false);

        SecurityUser teacher = new SecurityUser(11L, "teacher@email.com", "x", User.Role.TEACHER, 4L);
        MarkClassAttendanceRequest request = new MarkClassAttendanceRequest(
            7L,
            LocalDate.of(2026, 9, 24),
            List.of(new AttendanceMarkItem(22L, AttendanceStatus.PRESENT)));

        ForbiddenException ex = assertThrows(
            ForbiddenException.class,
            () -> service.markClass(request, teacher));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Not assigned to this class", ex.getMessage());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void teacherCannotReadUnassignedClass() {
        when(schoolClassRepository.existsById(7L)).thenReturn(true);
        when(teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(4L, 7L)).thenReturn(false);

        SecurityUser teacher = new SecurityUser(11L, "teacher@email.com", "x", User.Role.TEACHER, 4L);

        ForbiddenException ex = assertThrows(
            ForbiddenException.class,
            () -> service.forClass(7L, LocalDate.of(2026, 9, 24), teacher));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Not assigned to this class", ex.getMessage());
    }
}
