package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.SeedData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("API de matrícula: /me/enrollment")
class EnrollmentApiTest extends ApiTestBase {
    private static Map<String, Object> enrollIn(Object... activityIds) {
        return Map.of("activityClassIds", List.of(activityIds));
    }

    @Test
    @DisplayName("retorna 201 com a matrícula do aluno autenticado")
    void shouldEnrollStudent() {
        Student student = newStudent();

        as(student).body(enrollIn(SeedData.SWIMMING_CLASS)).when().post("/me/enrollment")
                .then().statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("customerId", equalTo(student.customerId().toString()))
                .body("active", is(true))
                .body("activities", hasSize(1))
                .body("activities[0].enrollmentActivityId", notNullValue())
                .body("activities[0].activityClassId", equalTo(SeedData.SWIMMING_CLASS.toString()))
                .body("activities[0].monthlyFee", isNumber(180))
                .body("activities[0].active", is(true))
                .body("activities[0].startDate", equalTo(LocalDate.now().toString()))
                .body("activities[0].endDate", nullValue());
    }
}
