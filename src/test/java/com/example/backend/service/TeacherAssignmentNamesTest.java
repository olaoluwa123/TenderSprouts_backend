package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TeacherAssignmentNamesTest {

    private TeacherClassRepository teacherClassRepository;
    private TeacherRepository teacherRepository;
    private TeacherAssignmentService service;

    @BeforeEach
    void setUp() {
        teacherClassRepository = Mockito.mock(TeacherClassRepository.class);
        teacherRepository = Mockito.mock(TeacherRepository.class);
        service = new TeacherAssignmentService(
            teacherRepository,
            teacherClassRepository,
            Mockito.mock(SchoolClassRepository.class),
            new AuditService(null));
    }

    @Test
    void joinedTeacherNamesAreSortedAndEqual() {
        when(teacherClassRepository.findByClassIdAndIsActiveTrue(8L))
            .thenReturn(List.of(assignment(2L), assignment(1L)));
        when(teacherRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id == 1L) {
                return Optional.of(teacher(1L, "Tunde"));
            }
            if (id == 2L) {
                return Optional.of(teacher(2L, "Ada"));
            }
            return Optional.empty();
        });

        assertEquals("Ada / Tunde", service.joinedTeacherNames(8L));
    }

    private static TeacherClass assignment(Long teacherId) {
        TeacherClass row = new TeacherClass();
        row.setTeacherId(teacherId);
        row.setClassId(8L);
        row.setIsActive(true);
        return row;
    }

    private static Teacher teacher(Long id, String name) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setFullName(name);
        return teacher;
    }
}
