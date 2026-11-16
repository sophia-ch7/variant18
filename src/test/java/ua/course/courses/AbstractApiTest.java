package ua.course.courses;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import io.restassured.http.ContentType;
import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractApiTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUpApi() {
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = port;

        jdbc.update("DELETE FROM enrollments");
        jdbc.update("DELETE FROM students");
        jdbc.update("DELETE FROM courses");
    }
    long createStudent(String code) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"code\":\"%s\",\"age\":20,\"entryScore\":90,\"regular\":false}".formatted(code))
                .when()
                .post("/students")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    long createCourse(String code, long price) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"code\":\"%s\",\"price\":%d,\"capacity\":10,\"level\":1}".formatted(code, price))
                .when()
                .post("/courses")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    long createEnrollment(String reference, long studentId, long courseId) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"reference\":\"%s\",\"studentId\":%d,\"courseId\":%d,\"couponValid\":false}"
                        .formatted(reference, studentId, courseId))
                .when()
                .post("/enrollments")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }
}