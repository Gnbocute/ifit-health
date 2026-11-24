package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.SeedData;
import io.restassured.response.ValidatableResponse;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("API de relatórios: GET /activity-reports")
class ActivityReportApiTest extends ApiTestBase {
    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate MARCH_31 = LocalDate.of(2026, 3, 31);
    private static final String SWIMMING_ITEM = "items.find { it.activityClassId == '%s' }".formatted(SeedData.SWIMMING_CLASS);

    private ValidatableResponse report(LocalDate start, LocalDate end, String... extraParams) {
        var request = asAdmin().queryParam("startDate", start.toString()).queryParam("endDate", end.toString());
        for (int i = 0; i < extraParams.length; i += 2) {
            request = request.queryParam(extraParams[i], extraParams[i + 1]);
        }
        return request.get("/activity-reports").then();
    }

    @Test
    @DisplayName("retorna o período e todas as atividades zeradas quando não há matrículas")
    void shouldReturnEmptyValuesWithoutEnrollments() {
        report(MARCH_1, MARCH_31)
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("startDate", equalTo(MARCH_1.toString()))
                .body("endDate", equalTo(MARCH_31.toString()))
                .body("items.size()", equalTo(1))
                .body(SWIMMING_ITEM + ".capacity", equalTo(SeedData.SWIMMING_CAPACITY))
                .body(SWIMMING_ITEM + ".enrollmentCount", equalTo(0))
                .body(SWIMMING_ITEM + ".revenue", isNumber(0))
                .body(SWIMMING_ITEM + ".averageOccupancyPercent", isNumber(0));
    }

    @Test
    @DisplayName("conta matrículas, soma receita e calcula ocupação no período de um mês")
    void shouldCalculateValuesForEnrollmentsInThePeriod() {
        DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE, MARCH_1);
        DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE, MARCH_1);

        report(MARCH_1, MARCH_31)
                .statusCode(200)
                .body(SWIMMING_ITEM + ".enrollmentCount", equalTo(2))
                .body(SWIMMING_ITEM + ".revenue", isNumber(360))
                .body(SWIMMING_ITEM + ".averageOccupancyPercent", isNumber(10.0));
    }

    @Test
    @DisplayName("não conta matrícula encerrada antes do período do relatório")
    void shouldIgnoreEnrollmentEndedBeforePeriod() {
        UUID enrollment = DbFixtures.insertEnrollment(DbFixtures.insertCustomer(), true);
        DbFixtures.insertEnrollmentActivity(enrollment, SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 10), false);

        report(MARCH_1, MARCH_31)
                .statusCode(200)
                .body(SWIMMING_ITEM + ".enrollmentCount", equalTo(0))
                .body(SWIMMING_ITEM + ".revenue", isNumber(0));
    }
}
