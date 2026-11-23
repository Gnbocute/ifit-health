package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.SeedData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

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
}
