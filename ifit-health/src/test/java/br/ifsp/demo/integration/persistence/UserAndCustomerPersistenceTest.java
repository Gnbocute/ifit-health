package br.ifsp.demo.integration.persistence;

import br.ifsp.demo.security.user.JpaUserRepository;
import br.ifsp.demo.security.user.Role;
import br.ifsp.demo.security.user.User;
import br.ifsp.demo.support.PersistenceTestBase;
import br.ifsp.demo.support.SeedData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Persistência de contas (JpaUserRepository) e clientes (SqliteCustomerRepository)")
class UserAndCustomerPersistenceTest extends PersistenceTestBase {
    @Autowired
    private JpaUserRepository userRepository;

    @Test
    @DisplayName("encontra a conta pelo e-mail sem diferenciar maiúsculas")
    void shouldFindUserIgnoringCase() {
        assertThat(userRepository.findByEmailIgnoreCase("ADMIN@iFit.com"))
                .get()
                .extracting(User::getId, User::getRole)
                .containsExactly(SeedData.ADMIN_USER_ID, Role.ADMIN);
    }
}
