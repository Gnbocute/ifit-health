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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Persistência de matrículas e consultas de conflito de horário")
class EnrollmentPersistenceTest extends PersistenceTestBase {
    private static final BigDecimal FEE = new BigDecimal("100");

    @Autowired
    private SqliteEnrollmentRepository enrollmentRepository;

    private UUID activityOn(UUID sport, UUID room, UUID trainer, int startHour, int capacity, DayOfWeek... days) {
        return DbFixtures.insertActivity(sport, room, trainer, LocalTime.of(startHour, 0), LocalTime.of(startHour + 1, 0),
                capacity, FEE, true, days);
    }

    private UUID judo(int capacity, DayOfWeek... days) {
        return activityOn(SeedData.SPORT_JUDO, SeedData.ROOM_JUDO, SeedData.TRAINER_CARLOS, 18, capacity, days);
    }

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

    @Test
    @DisplayName("reaproveita a matrícula existente do cliente")
    void shouldReuseExistingEnrollment() {
        Customer customer = customer(SeedData.ANA_CUSTOMER_ID);
        Enrollment first = enrollmentRepository.enroll(customer, List.of(activity(SeedData.SWIMMING_CLASS)));

        Enrollment second = enrollmentRepository.enroll(customer, List.of(activity(judo(10, DayOfWeek.TUESDAY))));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getEnrollmentActivities()).hasSize(2);
        assertThat(DbFixtures.count("SELECT COUNT(*) FROM enrollments")).isEqualTo(1);
    }
}
