package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.SeedData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("API de catálogo: /sports, /rooms e /trainers")
class CatalogApiTest extends ApiTestBase {
    @Test
    @DisplayName("retorna todas as modalidades com o tipo de sala")
    void shouldListSports() {
        asAdmin().when().get("/sports").then().statusCode(200).contentType(ContentType.JSON)
            .body("size()", equalTo(SeedData.SPORTS_COUNT))
            .body("find { it.id == '%s' }.name".formatted(SeedData.SPORT_SWIMMING), equalTo(SeedData.SWIMMING_NAME))
            .body("find { it.id == '%s' }.roomType".formatted(SeedData.SPORT_SWIMMING), equalTo("POOL"))
            .body("find { it.id == '%s' }.description".formatted(SeedData.SPORT_SWIMMING), not(emptyOrNullString()))
            .body("active", everyItem(is(true)));
    }

    @Test
    @DisplayName("retorna todas as salas com tipo e capacidade")
    void shouldListRooms() {
        asAdmin().when().get("/rooms").then().statusCode(200)
            .body("size()", equalTo(SeedData.ROOMS_COUNT))
            .body("find { it.id == '%s' }.name".formatted(SeedData.ROOM_POOL), equalTo("Piscina"))
            .body("find { it.id == '%s' }.roomType".formatted(SeedData.ROOM_POOL), equalTo("POOL"))
            .body("find { it.id == '%s' }.capacity".formatted(SeedData.ROOM_POOL), equalTo(SeedData.ROOM_POOL_CAPACITY))
            .body("capacity", everyItem(greaterThan(0)));
    }
}
