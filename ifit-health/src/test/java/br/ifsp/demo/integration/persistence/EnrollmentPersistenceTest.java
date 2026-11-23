package br.ifsp.demo.integration.persistence;

import br.ifsp.demo.domain.model.Customer;
import br.ifsp.demo.domain.model.Enrollment;
import br.ifsp.demo.domain.model.EnrollmentActivity;
import br.ifsp.demo.exception.CapacityIsGreaterThanAcceptedException;
import br.ifsp.demo.persistence.sqlite.SqliteEnrollmentRepository;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Persistência de matrículas e consultas de conflito de horário")
class EnrollmentPersistenceTest extends PersistenceTestBase {
    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate MARCH_31 = LocalDate.of(2026, 3, 31);
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

    private UUID enrollmentWith(UUID customer, UUID activity, LocalDate start, LocalDate end, boolean active) {
        UUID enrollment = DbFixtures.insertEnrollment(customer, true);
        return DbFixtures.insertEnrollmentActivity(enrollment, activity, FEE, start, end, active);
    }

    private static List<UUID> ids(List<EnrollmentActivity> activities) {
        return activities.stream().map(EnrollmentActivity::getId).toList();
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

    @Test
    @DisplayName("rejeita atividade lotada usando o COUNT de matrículas ativas")
    void shouldRejectFullActivity() {
        UUID judo = judo(2, DayOfWeek.TUESDAY);
        DbFixtures.fillActivity(judo, FEE, 2);

        assertThatThrownBy(() -> enrollmentRepository.enroll(customer(SeedData.ANA_CUSTOMER_ID), List.of(activity(judo))))
                .isInstanceOf(CapacityIsGreaterThanAcceptedException.class);
    }

    @Test
    @DisplayName("ignora matrículas canceladas na contagem de vagas")
    void shouldIgnoreCanceledEnrollmentsWhenCounting() {
        UUID judo = judo(1, DayOfWeek.TUESDAY);
        enrollmentWith(DbFixtures.insertCustomer(), judo, MARCH_1, MARCH_31, false);

        enrollmentRepository.enroll(customer(SeedData.ANA_CUSTOMER_ID), List.of(activity(judo)));

        assertThat(DbFixtures.count("SELECT COUNT(*) FROM enrollment_activities WHERE activity_id = ? AND status = 'ACTIVE'",
                judo.toString())).isEqualTo(1);
    }

    @Test
    @DisplayName("desfaz todas as atividades quando uma delas está lotada")
    void shouldRollBackWhenOneActivityIsFull() {
        UUID full = judo(1, DayOfWeek.TUESDAY);
        DbFixtures.fillActivity(full, FEE, 1);
        int before = DbFixtures.count("SELECT COUNT(*) FROM enrollment_activities");

        assertThatThrownBy(() -> enrollmentRepository.enroll(customer(SeedData.ANA_CUSTOMER_ID),
                List.of(activity(SeedData.SWIMMING_CLASS), activity(full))))
                .isInstanceOf(CapacityIsGreaterThanAcceptedException.class);

        assertThat(DbFixtures.count("SELECT COUNT(*) FROM enrollment_activities")).isEqualTo(before);
        assertThat(DbFixtures.count("SELECT COUNT(*) FROM enrollments WHERE customer_id = ?",
                SeedData.ANA_CUSTOMER_ID.toString())).isZero();
    }

    @Test
    @DisplayName("grava a data de cancelamento e desativa a matrícula")
    void shouldPersistCancellation() {
        Customer customer = customer(SeedData.ANA_CUSTOMER_ID);
        Enrollment enrollment = enrollmentRepository.enroll(customer, List.of(activity(SeedData.SWIMMING_CLASS)));
        UUID activityId = enrollment.getEnrollmentActivities().getFirst().getId();
        LocalDate endDate = LocalDate.now().plusDays(5);

        enrollment.cancelActivity(activityId, endDate);
        enrollment.deactivate();
        enrollmentRepository.save(enrollment);

        assertThat(DbFixtures.text("SELECT status || '|' || end_date FROM enrollment_activities WHERE id = ?", activityId.toString()))
                .isEqualTo("INACTIVE|" + endDate);
        assertThat(DbFixtures.text("SELECT status || '|' || end_date FROM enrollments WHERE id = ?", enrollment.getId().toString()))
                .isEqualTo("INACTIVE|" + LocalDate.now());
    }

    @Test
    @DisplayName("reativa matrícula inativa e limpa a data de término")
    void shouldReactivateEnrollment() {
        Customer customer = customer(SeedData.ANA_CUSTOMER_ID);
        Enrollment enrollment = enrollmentRepository.enroll(customer, List.of(activity(SeedData.SWIMMING_CLASS)));
        enrollment.cancelActivity(enrollment.getEnrollmentActivities().getFirst().getId(), LocalDate.now());
        enrollment.deactivate();
        enrollmentRepository.save(enrollment);

        enrollmentRepository.enroll(customer, List.of(activity(SeedData.SWIMMING_CLASS)));

        assertThat(DbFixtures.text("SELECT status FROM enrollments WHERE id = ?", enrollment.getId().toString())).isEqualTo("ACTIVE");
        assertThat(DbFixtures.text("SELECT end_date FROM enrollments WHERE id = ?", enrollment.getId().toString())).isNull();
    }

    @Test
    @DisplayName("carrega a matrícula com as atividades ordenadas pela data de início")
    void shouldFindByCustomer() {
        UUID enrollment = DbFixtures.insertEnrollment(SeedData.ANA_CUSTOMER_ID, true);
        UUID later = DbFixtures.insertEnrollmentActivity(enrollment, SeedData.SWIMMING_CLASS, FEE, MARCH_31, null, true);
        UUID earlier = DbFixtures.insertEnrollmentActivity(enrollment, judo(10, DayOfWeek.TUESDAY), FEE, MARCH_1, MARCH_31, false);

        Enrollment found = enrollmentRepository.findByCustomer(customer(SeedData.ANA_CUSTOMER_ID)).orElseThrow();

        assertThat(found.getId()).isEqualTo(enrollment);
        assertThat(ids(found.getEnrollmentActivities())).containsExactly(earlier, later);
        assertThat(found.getEnrollmentActivities().getFirst().isActive()).isFalse();
        assertThat(found.getEnrollmentActivities().getFirst().getEndDate()).isEqualTo(MARCH_31);
    }

    @Test
    @DisplayName("retorna vazio quando o cliente não tem matrícula")
    void shouldReturnEmptyWithoutEnrollment() {
        assertThat(enrollmentRepository.findByCustomer(customer(SeedData.ANA_CUSTOMER_ID))).isEmpty();
    }

    @Test
    @DisplayName("busca por atividade retorna só linhas ativas de matrículas ativas")
    void shouldFindEnrolledActivities() {
        UUID active = DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, FEE, MARCH_1);
        enrollmentWith(DbFixtures.insertCustomer(), SeedData.SWIMMING_CLASS, MARCH_1, MARCH_31, false);
        UUID inactiveEnrollment = DbFixtures.insertEnrollment(DbFixtures.insertCustomer(), false);
        DbFixtures.insertEnrollmentActivity(inactiveEnrollment, SeedData.SWIMMING_CLASS, FEE, MARCH_1, null, true);

        assertThat(ids(enrollmentRepository.findEnrolledActivitiesByActivityClass(activity(SeedData.SWIMMING_CLASS))))
                .containsExactly(active);
    }

    @Test
    @DisplayName("busca matrículas por atividade retorna cada uma uma vez com o cliente")
    void shouldFindEnrollmentsByActivity() {
        String name = FakeData.firstName() + " " + FakeData.lastName();
        UUID customer = DbFixtures.insertCustomer(name, FakeData.cpf(), FakeData.email());
        UUID enrollment = DbFixtures.insertEnrollment(customer, true);
        DbFixtures.insertEnrollmentActivity(enrollment, SeedData.SWIMMING_CLASS, FEE, MARCH_1, null, true);
        DbFixtures.insertEnrollmentActivity(enrollment, judo(10, DayOfWeek.TUESDAY), FEE, MARCH_1, null, true);

        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByActivityClass(activity(SeedData.SWIMMING_CLASS));

        assertThat(enrollments).singleElement().satisfies(e -> {
            assertThat(e.getId()).isEqualTo(enrollment);
            assertThat(e.getCustomer().getName()).isEqualTo(name);
            assertThat(e.getEnrollmentActivities()).hasSize(2);
        });
    }

    @Test
    @DisplayName("busca por período inclui os dois limites e ignora linhas inativas")
    void shouldFindByStartDateBetween() {
        UUID onFirstDay = DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, FEE, MARCH_1);
        UUID onLastDay = DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, FEE, MARCH_31);
        DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, FEE, MARCH_1.minusDays(1));
        DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, FEE, MARCH_31.plusDays(1));
        enrollmentWith(DbFixtures.insertCustomer(), SeedData.SWIMMING_CLASS, MARCH_1, MARCH_31, false);

        assertThat(ids(enrollmentRepository.findActiveEnrollmentActivitiesByStartDateBetween(MARCH_1, MARCH_31)))
                .containsExactly(onFirstDay, onLastDay);
    }
}
