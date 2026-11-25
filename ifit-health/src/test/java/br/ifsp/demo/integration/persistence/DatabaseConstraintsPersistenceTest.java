package br.ifsp.demo.integration.persistence;

import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
import br.ifsp.demo.support.PersistenceTestBase;
import br.ifsp.demo.support.SeedData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Restrições do banco: UNIQUE, CHECK, FOREIGN KEY e triggers")
class DatabaseConstraintsPersistenceTest extends PersistenceTestBase {
    private static final String INSERT_USER =
            "INSERT INTO app_user (id, name, lastname, email, password, role) VALUES (?, 'Teste', 'Teste', ?, 'x', ?)";

    @Test
    @DisplayName("exige e-mail único em app_user sem diferenciar maiúsculas")
    void shouldRejectDuplicatedUserEmailIgnoringCase() {
        assertRejected(INSERT_USER, UUID.randomUUID().toString(), SeedData.ADMIN_EMAIL.toUpperCase(), "USER")
                .hasMessageContaining("UNIQUE constraint failed: app_user.email");
    }

    @Test
    @DisplayName("aceita só USER e ADMIN como papel em app_user")
    void shouldRejectUnknownRole() {
        assertRejected(INSERT_USER, UUID.randomUUID().toString(), FakeData.email(), "MANAGER")
                .hasMessageContaining("CHECK constraint failed");
    }

    @Test
    @DisplayName("exige CPF único em customers")
    void shouldRejectDuplicatedCpf() {
        assertRejected("INSERT INTO customers (id, cpf, name, birthdate, gender, email) VALUES (?, ?, 'X', '2000-01-01', 'MALE', ?)",
                UUID.randomUUID().toString(), SeedData.ANA_CPF, FakeData.email())
                .hasMessageContaining("UNIQUE constraint failed: customers.cpf");
    }

    @Test
    @DisplayName("permite vincular uma conta a um único cliente")
    void shouldRejectSecondCustomerForSameUser() {
        assertRejected("INSERT INTO customers (id, user_id, cpf, name, birthdate, gender, email) VALUES (?, ?, ?, 'X', '2000-01-01', 'MALE', ?)",
                UUID.randomUUID().toString(), SeedData.ANA_USER_ID.toString(), FakeData.cpf(), FakeData.email())
                .hasMessageContaining("UNIQUE constraint failed: customers.user_id");
    }

    @Test
    @DisplayName("permite vários clientes sem conta (índice único parcial)")
    void shouldAllowSeveralCustomersWithoutAccount() {
        DbFixtures.insertCustomer();
        DbFixtures.insertCustomer();

        assertThat(DbFixtures.count("SELECT COUNT(*) FROM customers WHERE user_id IS NULL")).isEqualTo(2);
    }

    @Test
    @DisplayName("exige que user_id aponte para uma conta existente")
    void shouldRejectCustomerWithUnknownUser() {
        assertRejected("INSERT INTO customers (id, user_id, cpf, name, birthdate, gender, email) VALUES (?, ?, ?, 'X', '2000-01-01', 'MALE', ?)",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), FakeData.cpf(), FakeData.email())
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }
}
