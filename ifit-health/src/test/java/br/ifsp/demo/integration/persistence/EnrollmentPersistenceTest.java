package br.ifsp.demo.integration.persistence;

import br.ifsp.demo.domain.model.Customer;
import br.ifsp.demo.domain.model.Enrollment;
import br.ifsp.demo.persistence.sqlite.SqliteEnrollmentRepository;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.PersistenceTestBase;
import br.ifsp.demo.support.SeedData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Persistência de matrículas e consultas de conflito de horário")
class EnrollmentPersistenceTest extends PersistenceTestBase {
    @Autowired
    private SqliteEnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("cria a matrícula e a atividade com a mensalidade atual")
    void shouldCreateEnrollment() {
        Customer customer = customer(SeedData.ANA_CUSTOMER_ID);

        Enrollment enrollment = enrollmentRepository.enroll(customer, List.of(activity(SeedData.SWIMMING_CLASS)));

        assertThat(DbFixtures.text("SELECT status FROM enrollments WHERE customer_id = ?", customer.getId().toString())).isEqualTo("ACTIVE");
        assertThat(DbFixtures.text("SELECT monthly_fee FROM enrollment_activities WHERE enrollment_id = ?", enrollment.getId().toString()))
                .isEqualTo("180");
        assertThat(DbFixtures.text("SELECT start_date FROM enrollment_activities WHERE enrollment_id = ?", enrollment.getId().toString()))
                .isEqualTo(LocalDate.now().toString());
    }
}
