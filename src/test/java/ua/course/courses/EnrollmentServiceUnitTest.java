package ua.course.courses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnrollmentServiceUnitTest {

    private final StudentRepository students = mock(StudentRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final EnrollmentPolicy policy = mock(EnrollmentPolicy.class);
    private final EnrollmentService service =
            new EnrollmentService(students, courses, enrollments, policy);

    @ParameterizedTest(name = "{0} днів, передоплата {1} → повернення {2}")
    @CsvSource({
            "0,   100000, 70000",
            "9,   100000, 70000",
            "10,  100000, 100000",
            "100, 100000, 100000",
            "0,   20000,  0"
    })
    void calculateRefund_followsFeeRule(int days, long prepaid, long expected) {
        assertEquals(expected, service.calculateRefund(prepaid, days));
    }

    @Test
    void calculateRefund_negativeDays_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.calculateRefund(100000, -1));
    }

    @Test
    void calculateTotal_noDiscount_returnsPrice() {
        assertEquals(100000, service.calculateTotal(100000, true, true));
    }

    @Test
    void enroll_noFreePlaces_rejectedWithoutPolicyCall() throws Exception {
        when(students.findById(1)).thenReturn(new Student(1, "S-1", 20, 90, false));
        when(courses.findById(2)).thenReturn(new Course(2, "C-1", 100000, 1, 1));
        when(enrollments.hasEnrollment(1, 2)).thenReturn(false);
        when(enrollments.countOccupied(2)).thenReturn(1L);

        assertThrows(EnrollmentRejectedException.class,
                () -> service.enroll(new EnrollmentRequest("E-1", 1, 2, false)));

        verifyNoInteractions(policy);
        verify(enrollments, never()).create(any());
    }
}