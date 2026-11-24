package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.SeedData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("API de matrícula: /me/enrollment")
class EnrollmentApiTest extends ApiTestBase {
    private static Map<String, Object> enrollIn(Object... activityIds) {
        return Map.of("activityClassIds", List.of(activityIds));
    }

    private UUID gymOnMonday(LocalTime start, LocalTime end, int capacity) {
        return DbFixtures.insertActivity(SeedData.SPORT_WEIGHT_TRAINING, SeedData.ROOM_GYM, SeedData.TRAINER_JOHN,
                start, end, capacity, new BigDecimal("120"), true, DayOfWeek.MONDAY);
    }

    private String enrollInSwimming(Student student) {
        return as(student).body(enrollIn(SeedData.SWIMMING_CLASS)).when().post("/me/enrollment")
                .then().statusCode(201)
                .extract().path("activities[0].enrollmentActivityId");
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

    @Test
    @DisplayName("matricula em várias atividades de uma vez e reduz as vagas")
    void shouldEnrollInSeveralActivities() {
        UUID gym = gymOnMonday(LocalTime.of(8, 0), LocalTime.of(9, 0), 10);

        as(newStudent()).body(enrollIn(SeedData.SWIMMING_CLASS, gym)).when().post("/me/enrollment")
                .then().statusCode(201)
                .body("activities.activityClassId", containsInAnyOrder(SeedData.SWIMMING_CLASS.toString(), gym.toString()));

        asAdmin().when().get("/activity-classes")
                .then().body("find { it.id == '%s' }.availableSpots".formatted(gym), equalTo(9));
    }

    @Test
    @DisplayName("adiciona nova atividade a uma matrícula existente")
    void shouldAddActivityToExistingEnrollment() {
        Student student = newStudent();
        enrollInSwimming(student);
        UUID gym = gymOnMonday(LocalTime.of(8, 0), LocalTime.of(9, 0), 10);

        as(student).body(enrollIn(gym)).when().post("/me/enrollment")
                .then().statusCode(201)
                .body("activities", hasSize(2));

        assertThat(DbFixtures.count("SELECT COUNT(*) FROM enrollments WHERE customer_id = ?",
                student.customerId().toString())).isEqualTo(1);
    }

    @Test
    @DisplayName("retorna 400 ao matricular com lista vazia")
    void shouldRejectEmptyList() {
        as(newStudent()).body(Map.of("activityClassIds", List.of())).when().post("/me/enrollment")
                .then().statusCode(400)
                .body("errors.activityClassIds", not(emptyOrNullString()));
    }
}
