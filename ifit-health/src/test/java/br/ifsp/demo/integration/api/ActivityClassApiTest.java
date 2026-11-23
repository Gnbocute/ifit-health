package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.SeedData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("API de turmas: /activity-classes")
class ActivityClassApiTest extends ApiTestBase {
    @Test
    @DisplayName("retorna as atividades com nomes do catálogo e disponibilidade")
    void shouldListActivitiesWithAvailability() {
        DbFixtures.fillActivity(SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE, 3);

        asAdmin().when().get("/activity-classes")
                .then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(SeedData.SWIMMING_CLASS.toString()))
                .body("[0].sportName", equalTo(SeedData.SWIMMING_NAME))
                .body("[0].roomName", equalTo("Piscina"))
                .body("[0].trainerName", equalTo(SeedData.TRAINER_JULIANA_NAME))
                .body("[0].weekdays", contains("MONDAY"))
                .body("[0].startTime", equalTo("11:30:00"))
                .body("[0].endTime", equalTo("12:30:00"))
                .body("[0].capacity", equalTo(SeedData.SWIMMING_CAPACITY))
                .body("[0].enrolledCount", equalTo(3))
                .body("[0].availableSpots", equalTo(SeedData.SWIMMING_CAPACITY - 3))
                .body("[0].active", is(true));
    }

    @Test
    @DisplayName("filtra as atividades pelo parâmetro active")
    void shouldFilterByActive() {
        UUID inactive = DbFixtures.insertActivity(SeedData.SPORT_JUDO, SeedData.ROOM_JUDO, SeedData.TRAINER_CARLOS,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 10, new BigDecimal("90"), false, DayOfWeek.FRIDAY);

        asAdmin().queryParam("active", false).when().get("/activity-classes")
                .then().statusCode(200)
                .body("id", contains(inactive.toString()))
                .body("[0].availableSpots", equalTo(0));

        asAdmin().queryParam("active", true).when().get("/activity-classes")
                .then().statusCode(200)
                .body("id", contains(SeedData.SWIMMING_CLASS.toString()));
    }

    @Test
    @DisplayName("libera a listagem de atividades para alunos")
    void shouldListActivitiesForStudents() {
        as(newStudent()).when().get("/activity-classes")
                .then().statusCode(200)
                .body("id", hasItem(SeedData.SWIMMING_CLASS.toString()));
    }

    @Test
    @DisplayName("retorna 401 ao listar atividades sem token")
    void shouldRequireTokenToListActivities() {
        anonymous().when().get("/activity-classes").then().statusCode(401);
    }

    @Test
    @DisplayName("retorna os dados da atividade buscada por id")
    void shouldReturnActivity() {
        asAdmin().when().get("/activity-classes/{id}", SeedData.SWIMMING_CLASS).then().statusCode(200).contentType(ContentType.JSON)
            .body("id", equalTo(SeedData.SWIMMING_CLASS.toString()))
            .body("sportId", equalTo(SeedData.SPORT_SWIMMING.toString()))
            .body("roomId", equalTo(SeedData.ROOM_POOL.toString()))
            .body("trainerId", equalTo(SeedData.TRAINER_JULIANA.toString()))
            .body("weekdays", contains(SeedData.SWIMMING_DAY.name()))
            .body("startTime", equalTo("11:30:00"))
            .body("endTime", equalTo("12:30:00"))
            .body("capacity", equalTo(SeedData.SWIMMING_CAPACITY))
            .body("monthlyFee", isNumber(SeedData.SWIMMING_FEE.doubleValue()))
            .body("active", is(true));
    }

    @Test
    @DisplayName("retorna 404 ao buscar atividade com id desconhecido")
    void shouldReturnNotFoundForUnknownActivity() {
        UUID unknown = UUID.randomUUID();

        asAdmin().when().get("/activity-classes/{id}", unknown).then().statusCode(404)
            .body("message", equalTo("Activity class not found: " + unknown));
    }
}
