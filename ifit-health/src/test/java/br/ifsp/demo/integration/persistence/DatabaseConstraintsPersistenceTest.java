package br.ifsp.demo.integration.persistence;

import br.ifsp.demo.support.PersistenceTestBase;
import br.ifsp.demo.support.SeedData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
}
