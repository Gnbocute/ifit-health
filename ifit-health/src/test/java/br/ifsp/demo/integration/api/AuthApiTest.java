package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("API de autenticação: /register, /authenticate, /auth/me e /hello")
class AuthApiTest extends ApiTestBase {
    @Test
    @DisplayName("retorna 201 com ids do usuário e do cliente ao cadastrar com dados válidos")
    void shouldRegisterStudentWithValidData() {
        Map<String, Object> body = FakeData.registerRequest();

        var response = anonymous().body(body).when().post("/register")
                .then().statusCode(201)
                .contentType(ContentType.JSON)
                .body("userId", notNullValue())
                .body("customerId", notNullValue())
                .extract();

        UUID userId = UUID.fromString(response.path("userId"));
        assertThat(DbFixtures.text("SELECT role FROM app_user WHERE id = ?", userId.toString())).isEqualTo("USER");
        assertThat(DbFixtures.text("SELECT user_id FROM customers WHERE id = ?", response.path("customerId").toString()))
                .isEqualTo(userId.toString());
    }
}
