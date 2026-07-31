CREATE INDEX idx_attendance_class_date ON attendance (class_id, date);
CREATE INDEX idx_grades_student_term ON grades (student_id, term_id);
CREATE INDEX idx_grades_class_term ON grades (class_id, term_id);
CREATE INDEX idx_student_enrollments_session_status ON student_enrollments (session_id, status);
CREATE INDEX idx_students_class_id ON students (class_id);
