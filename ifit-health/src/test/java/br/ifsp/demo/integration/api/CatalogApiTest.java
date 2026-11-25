package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
import br.ifsp.demo.support.SeedData;
import br.ifsp.demo.ui.pages.AdminActivitiesPage;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import java.text.Collator;
import java.util.Locale;
import java.util.List;

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

    @Test
    @DisplayName("retorna todos os professores e o status de cada um")
    void shouldListTrainers() {
        asAdmin().when().get("/trainers").then().statusCode(200)
            .body("size()", equalTo(SeedData.TRAINERS_COUNT))
            .body("find { it.id == '%s' }.name".formatted(SeedData.TRAINER_JULIANA), equalTo(SeedData.TRAINER_JULIANA_NAME))
            .body("active", everyItem(is(true)));
    }

    @Test
    @DisplayName("retorna professores em ordem alfabética")
    void shouldSortTrainersByName() {
        List<String> names = asAdmin().when().get("/trainers").then().statusCode(200).extract().path("name");

        assertThat(names).isSortedAccordingTo(Collator.getInstance(Locale.forLanguageTag("pt-BR")));
    }

    @Test
    @DisplayName("retorna modalidades em ordem alfabética do pt-BR, com acentos")
    void shouldSortSportsAlphabetically() {
        List<String> names = asAdmin().when().get("/sports").then().statusCode(200).extract().path("name");

        assertThat(names).isSortedAccordingTo(Collator.getInstance(Locale.forLanguageTag("pt-BR")));
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"/sports", "/rooms", "/trainers"})
    @DisplayName("retorna 403 para aluno no catálogo")
    void shouldForbidStudents(String path) {
        as(newStudent()).when().get(path).then().statusCode(403);
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"/sports", "/rooms", "/trainers"})
    @DisplayName("retorna 401 no catálogo sem token")
    void shouldRequireAuthentication(String path) {
        anonymous().when().get(path).then().statusCode(401);
    }

    @Test
    @DisplayName("cria atividade válida e mostra na lista")
    void shouldCreateActivity() {
        int capacity = FakeData.faker().number().numberBetween(5, 31);
        int fee = FakeData.faker().number().numberBetween(80, 250);

        form.fill("Musculação", "Sala de Musculação", "John Doe", List.of("Terça", "Quinta"),
                "08:00", "09:00", String.valueOf(capacity), String.valueOf(fee));
        AdminActivitiesPage list = form.submitAndReturnToList();

        assertThat(list.toast()).isEqualTo("Atividade cadastrada!");
        assertThat(list.cell("Musculação", 3)).contains("Ter · Qui").contains("08:00–09:00");
        assertThat(list.cell("Musculação", 4)).isEqualTo("0/" + capacity);
        assertThat(list.cell("Musculação", 5)).isEqualTo("R$ " + fee + ",00");
        assertThat(DbFixtures.count("SELECT COUNT(*) FROM activity_class")).isEqualTo(2);
    }
}
