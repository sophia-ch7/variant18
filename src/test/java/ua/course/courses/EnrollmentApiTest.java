package ua.course.courses;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentApiTest extends AbstractApiTest {

    @Test
    void createStudent_returns201_andStudentBody() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"code\":\"ST-001\",\"age\":20,\"entryScore\":90,\"regular\":false}")
                .when()
                .post("/students")
                .then()
                .statusCode(201)
                .body("id", greaterThan(0))
                .body("code", equalTo("ST-001"))
                .body("age", equalTo(20))
                .body("entryScore", equalTo(90))
                .body("regular", equalTo(false));
    }

    @Test
    void getStudentById_returns200_andSameData() {
        long id = createStudent("ST-001");

        given()
                .when()
                .get("/students/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("code", equalTo("ST-001"))
                .body("age", equalTo(20))
                .body("entryScore", equalTo(90))
                .body("regular", equalTo(false));
    }

    @Test
    void getMissingStudent_returns404_withErrorContract() {
        given()
                .when()
                .get("/students/{id}", 999999)
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("Слухача не знайдено"));
    }

    @Test
    void cancelEnrollment_returns200_andCancelledState() {
        long studentId = createStudent("ST-001");
        long courseId = createCourse("CR-001", 100000);
        long enrollmentId = createEnrollment("EN-001", studentId, courseId);

        given()
                .contentType(ContentType.JSON)
                .body("{\"daysBeforeStart\":7}")
                .when()
                .post("/enrollments/{id}/cancel", enrollmentId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) enrollmentId))
                .body("status", equalTo("Cancelled"))
                .body("prepaid", equalTo(100000))
                .body("refund", equalTo(70000));
    }

    @Test
    void cancelTwice_returns409_withErrorContract() {
        long studentId = createStudent("ST-001");
        long courseId = createCourse("CR-001", 100000);
        long enrollmentId = createEnrollment("EN-001", studentId, courseId);

        given()
                .contentType(ContentType.JSON)
                .body("{\"daysBeforeStart\":7}")
                .when()
                .post("/enrollments/{id}/cancel", enrollmentId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body("{\"daysBeforeStart\":7}")
                .when()
                .post("/enrollments/{id}/cancel", enrollmentId)
                .then()
                .statusCode(409)
                .body("code", equalTo("ENROLLMENT_CONFLICT"))
                .body("message", equalTo("Зміна статусу заборонена"));
    }

    @Test
    void afterRejectedCancel_enrollmentStaysUnchanged() {
        long studentId = createStudent("ST-001");
        long courseId = createCourse("CR-001", 100000);
        long enrollmentId = createEnrollment("EN-001", studentId, courseId);

        given()
                .contentType(ContentType.JSON)
                .body("{\"daysBeforeStart\":7}")
                .when()
                .post("/enrollments/{id}/cancel", enrollmentId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body("{\"daysBeforeStart\":20}")
                .when()
                .post("/enrollments/{id}/cancel", enrollmentId)
                .then()
                .statusCode(409);

        given()
                .when()
                .get("/enrollments/{id}", enrollmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("Cancelled"))
                .body("prepaid", equalTo(100000))
                .body("refund", equalTo(70000));
    }

    @ParameterizedTest(name = "{0} днів, ціна {1} → повернення {2}")
    @CsvSource({
            "0,   100000, 70000",
            "9,   100000, 70000",
            "10,  100000, 100000",
            "100, 100000, 100000",
            "0,   20000,  0"
    })
    void cancel_appliesRefundRule(int daysBeforeStart, long price, int expectedRefund) {
        long studentId = createStudent("ST-001");
        long courseId = createCourse("CR-001", price);
        long enrollmentId = createEnrollment("EN-001", studentId, courseId);

        given()
                .contentType(ContentType.JSON)
                .body("{\"daysBeforeStart\":%d}".formatted(daysBeforeStart))
                .when()
                .post("/enrollments/{id}/cancel", enrollmentId)
                .then()
                .statusCode(200)
                .body("refund", equalTo(expectedRefund));

        given()
                .when()
                .get("/enrollments/{id}", enrollmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("Cancelled"))
                .body("prepaid", equalTo((int) price))
                .body("refund", equalTo(expectedRefund));
    }

    @Test
    void enrollmentWorkflow_createGetCancelGet() {
        long studentId = createStudent("ST-001");
        long courseId = createCourse("CR-001", 100000);

        var created = given()
                .contentType(ContentType.JSON)
                .body("{\"reference\":\"EN-001\",\"studentId\":%d,\"courseId\":%d,\"couponValid\":false}"
                        .formatted(studentId, courseId))
                .when()
                .post("/enrollments")
                .then()
                .statusCode(201)
                .body("status", equalTo("Enrolled"))
                .extract();

        long enrollmentId = created.jsonPath().getLong("id");
        assertTrue(enrollmentId > 0);
        assertThat(created.header("Location"), endsWith("/enrollments/" + enrollmentId));

        given()
                .when()
                .get("/enrollments/{id}", enrollmentId)
                .then()
                .statusCode(200)
                .body("reference", equalTo("EN-001"))
                .body("studentId", equalTo((int) studentId))
                .body("courseId", equalTo((int) courseId))
                .body("status", equalTo("Enrolled"))
                .body("total", equalTo(100000))
                .body("prepaid", equalTo(100000))
                .body("refund", equalTo(0));

        given()
                .contentType(ContentType.JSON)
                .body("{\"daysBeforeStart\":5}")
                .when()
                .post("/enrollments/{id}/cancel", enrollmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("Cancelled"));

        given()
                .when()
                .get("/enrollments/{id}", enrollmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("Cancelled"))
                .body("total", equalTo(100000))
                .body("prepaid", equalTo(100000))
                .body("refund", equalTo(70000));
    }
}