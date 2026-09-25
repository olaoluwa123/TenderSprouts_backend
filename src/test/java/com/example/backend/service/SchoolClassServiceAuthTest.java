package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.ClassGroup;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TeacherSubjectRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class SchoolClassServiceAuthTest {

    private SchoolClassRepository schoolClassRepository;
    private StudentRepository studentRepository;
    private TeacherClassRepository teacherClassRepository;
    private SchoolClassService service;

    @BeforeEach
    void setUp() {
        schoolClassRepository = Mockito.mock(SchoolClassRepository.class);
        studentRepository = Mockito.mock(StudentRepository.class);
        teacherClassRepository = Mockito.mock(TeacherClassRepository.class);
        AuthorizationService authorizationService = new AuthorizationService(
            teacherClassRepository,
            Mockito.mock(ParentStudentRepository.class),
            studentRepository,
            schoolClassRepository);
        service = new SchoolClassService(
            schoolClassRepository,
            Mockito.mock(ClassSubjectRepository.class),
            Mockito.mock(SubjectRepository.class),
            teacherClassRepository,
            Mockito.mock(TeacherRepository.class),
            Mockito.mock(TeacherSubjectRepository.class),
            studentRepository,
            Mockito.mock(AttendanceRepository.class),
            Mockito.mock(TermResultRepository.class),
            new ActiveTermService(null, null),
            new AuditService(null),
            authorizationService);
    }

    @Test
    void teacherCannotReadUnassignedClassProfile() {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(5L);
        schoolClass.setName("Year 4");
        schoolClass.setClassGroup(ClassGroup.PRIMARY);
        when(schoolClassRepository.findById(5L)).thenReturn(Optional.of(schoolClass));
        when(teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(1L, 5L)).thenReturn(false);

        SecurityUser teacher = new SecurityUser(2L, "teacher@email.com", "x", User.Role.TEACHER, 1L);

        ForbiddenException ex = assertThrows(
            ForbiddenException.class,
            () -> service.getProfile(5L, LocalDate.of(2026, 9, 24), teacher));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Not assigned to this class", ex.getMessage());
        verify(studentRepository, never()).findByClassId(5L);
    }
}
