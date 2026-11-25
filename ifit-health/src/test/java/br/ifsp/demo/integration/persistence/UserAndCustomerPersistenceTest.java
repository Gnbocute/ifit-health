package br.ifsp.demo.integration.persistence;

import br.ifsp.demo.security.user.JpaUserRepository;
import br.ifsp.demo.security.user.Role;
import br.ifsp.demo.security.user.User;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
import br.ifsp.demo.support.PersistenceTestBase;
import br.ifsp.demo.support.SeedData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Persistência de contas (JpaUserRepository) e clientes (SqliteCustomerRepository)")
class UserAndCustomerPersistenceTest extends PersistenceTestBase {
    @Autowired
    private JpaUserRepository userRepository;

    private User newUser(String email) {
        return User.builder().id(UUID.randomUUID()).name(FakeData.firstName()).lastname(FakeData.lastName())
                .email(email).password("hash").role(Role.USER).build();
    }

    @Test
    @DisplayName("encontra a conta pelo e-mail sem diferenciar maiúsculas")
    void shouldFindUserIgnoringCase() {
        assertThat(userRepository.findByEmailIgnoreCase("ADMIN@iFit.com"))
                .get()
                .extracting(User::getId, User::getRole)
                .containsExactly(SeedData.ADMIN_USER_ID, Role.ADMIN);
    }

    @Test
    @DisplayName("existsByEmailIgnoreCase responde true só para e-mails existentes")
    void shouldCheckIfEmailExists() {
        assertThat(userRepository.existsByEmailIgnoreCase(SeedData.ANA_EMAIL.toUpperCase())).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase(FakeData.email())).isFalse();
    }

    @Test
    @DisplayName("grava uma nova conta com saveAndFlush")
    void shouldSaveUser() {
        User user = newUser(FakeData.email());

        userRepository.saveAndFlush(user);

        assertThat(DbFixtures.text("SELECT role FROM app_user WHERE id = ?", user.getId().toString())).isEqualTo("USER");
        assertThat(userRepository.count()).isEqualTo(3);
    }
}
