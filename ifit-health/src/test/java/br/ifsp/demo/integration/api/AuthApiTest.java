package br.ifsp.demo.integration.api;

import br.ifsp.demo.support.ApiTestBase;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
import br.ifsp.demo.support.SeedData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

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

    @Test
    @DisplayName("grava o endereço quando ele é informado no cadastro")
    void shouldRegisterStudentWithAddress() {
        Map<String, Object> body = FakeData.registerRequest();
        Map<String, Object> address = FakeData.address();
        body.put("address", address);

        String customerId = anonymous().body(body).when().post("/register")
                .then().statusCode(201).extract().path("customerId");

        assertThat(DbFixtures.text("SELECT address_city FROM customers WHERE id = ?", customerId))
                .isEqualTo(address.get("city"));
    }

    @Test
    @DisplayName("aceita CPF com pontuação no cadastro e grava só os dígitos")
    void shouldNormalizeFormattedCpf() {
        Map<String, Object> body = FakeData.registerRequest();
        String cpf = (String) body.get("cpf");
        body.put("cpf", FakeData.formattedCpf(cpf));

        String customerId = anonymous().body(body).when().post("/register")
                .then().statusCode(201).extract().path("customerId");

        assertThat(DbFixtures.text("SELECT cpf FROM customers WHERE id = ?", customerId)).isEqualTo(cpf);
    }

    @Test
    @DisplayName("grava o e-mail do cadastro em minúsculas")
    void shouldNormalizeEmail() {
        Map<String, Object> body = FakeData.registerRequest();
        String email = (String) body.get("email");
        body.put("email", email.toUpperCase());

        String userId = anonymous().body(body).when().post("/register")
                .then().statusCode(201).extract().path("userId");

        assertThat(DbFixtures.text("SELECT email FROM app_user WHERE id = ?", userId)).isEqualTo(email.toLowerCase());
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("name", "   "),
                Arguments.of("name", "a".repeat(101)),
                Arguments.of("lastname", ""),
                Arguments.of("email", "not-an-email"),
                Arguments.of("email", null),
                Arguments.of("password", "1234567"),
                Arguments.of("password", "a".repeat(73)),
                Arguments.of("cpf", "123"),
                Arguments.of("cpf", "abc.def.ghi-jk"),
                Arguments.of("birthdate", FakeData.futureDate().toString()),
                Arguments.of("birthdate", null),
                Arguments.of("gender", null),
                Arguments.of("tel", "1".repeat(21)));
    }

    @ParameterizedTest(name = "[{index}] {0} = {1}")
    @MethodSource("invalidFields")
    @DisplayName("retorna 400 e identifica o campo inválido no cadastro")
    void shouldRejectInvalidField(String field, Object value) {
        Map<String, Object> body = FakeData.registerRequest();
        body.put(field, value);

        anonymous().body(body).when().post("/register")
                .then().statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("message", equalTo("Existem campos inválidos no cadastro."))
                .body("errors." + field, not(emptyOrNullString()));

        assertThat(DbFixtures.count("SELECT COUNT(*) FROM app_user")).isEqualTo(2);
    }

    @Test
    @DisplayName("retorna 400 no cadastro quando a UF do endereço não tem 2 letras")
    void shouldRejectInvalidAddressState() {
        Map<String, Object> body = FakeData.registerRequest();
        Map<String, Object> address = FakeData.address();
        address.put("state", "SAO");
        body.put("address", address);

        anonymous().body(body).when().post("/register")
                .then().statusCode(400)
                .body("errors.'address.state'", equalTo("Informe a sigla do estado com 2 letras."));
    }

    @Test
    @DisplayName("retorna 409 no cadastro quando o e-mail já existe, sem diferenciar maiúsculas")
    void shouldRejectDuplicatedEmail() {
        Map<String, Object> body = FakeData.registerRequest();
        body.put("email", SeedData.ADMIN_EMAIL.toUpperCase());

        anonymous().body(body).when().post("/register")
                .then().statusCode(409)
                .body("message", equalTo("Este e-mail já está cadastrado."));
    }
}
