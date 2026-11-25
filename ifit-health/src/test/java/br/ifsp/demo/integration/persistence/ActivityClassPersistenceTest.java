package br.ifsp.demo.integration.persistence;

import br.ifsp.demo.domain.model.ActivityClass;
import br.ifsp.demo.domain.model.Schedule;
import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.PersistenceTestBase;
import br.ifsp.demo.support.SeedData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Persistência de turmas, histórico de preços e catálogos")
class ActivityClassPersistenceTest extends PersistenceTestBase {
    private ActivityClass newGymClass(int capacity, String fee) {
        Schedule schedule = new Schedule(EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), LocalTime.of(8, 0), LocalTime.of(9, 0));
        return new ActivityClass(activityClassRepository.findRoomById(SeedData.ROOM_GYM),
            activityClassRepository.findSportById(SeedData.SPORT_WEIGHT_TRAINING),
            activityClassRepository.findTrainerById(SeedData.TRAINER_JOHN), schedule, capacity, new BigDecimal(fee));
    }

    private ActivityClass changeFee(ActivityClass activity, String fee) {
        activity.edit(activity.getRoom(), activity.getSport(), activity.getTrainer(), activity.getSchedule(),
                activity.getCapacity(), new BigDecimal(fee));
        return activityClassRepository.save(activity);
    }

    @Test
    @DisplayName("insere a atividade e o primeiro preço válido a partir de hoje")
    void shouldInsertActivityWithInitialPrice() {
        ActivityClass activity = activityClassRepository.save(newGymClass(12, "150.50"));

        assertThat(DbFixtures.text("SELECT weekday FROM activity_class WHERE id = ?", activity.getId().toString()))
                .isEqualTo("TUESDAY,THURSDAY");
        assertThat(DbFixtures.text("SELECT valid_from FROM activity_prices WHERE activity_id = ?", activity.getId().toString()))
                .isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("reconstrói a atividade com sala, modalidade, professor e horário")
    void shouldFindActivityById() {
        ActivityClass activity = activity(SeedData.SWIMMING_CLASS);

        assertThat(activity.getSport().getName()).isEqualTo(SeedData.SWIMMING_NAME);
        assertThat(activity.getRoom().getId()).isEqualTo(SeedData.ROOM_POOL);
        assertThat(activity.getTrainer().getName()).isEqualTo(SeedData.TRAINER_JULIANA_NAME);
        assertThat(activity.getSchedule().getWeekdays()).containsExactly(SeedData.SWIMMING_DAY);
        assertThat(activity.getSchedule().getStartTime()).isEqualTo(SeedData.SWIMMING_START);
        assertThat(activity.getSchedule().getEndTime()).isEqualTo(SeedData.SWIMMING_END);
        assertThat(activity.getCapacity()).isEqualTo(SeedData.SWIMMING_CAPACITY);
        assertThat(activity.getMonthlyFee()).isEqualByComparingTo(SeedData.SWIMMING_FEE);
        assertThat(activity.isActive()).isTrue();
    }

    @Test
    @DisplayName("retorna null ao buscar atividade com id desconhecido no repositório")
    void shouldReturnNullForUnknownId() {
        assertThat(activityClassRepository.findById(UUID.randomUUID())).isNull();
    }

    @Test
    @DisplayName("busca por sala e por professor retorna só atividades ativas")
    void shouldFindOnlyActiveActivitiesByRoomAndTrainer() {
        DbFixtures.insertActivity(SeedData.SPORT_SWIMMING, SeedData.ROOM_POOL, SeedData.TRAINER_JULIANA,
            LocalTime.of(15, 0), LocalTime.of(16, 0), 5, new BigDecimal("100"), false, DayOfWeek.FRIDAY);
        var room = activityClassRepository.findRoomById(SeedData.ROOM_POOL);
        var trainer = activityClassRepository.findTrainerById(SeedData.TRAINER_JULIANA);

        assertThat(activityClassRepository.findByRoom(room)).extracting(ActivityClass::getId).containsExactly(SeedData.SWIMMING_CLASS);
        assertThat(activityClassRepository.findByTrainer(trainer)).extracting(ActivityClass::getId).containsExactly(SeedData.SWIMMING_CLASS);
    }

    @Test
    @DisplayName("findAll do repositório retorna atividades ativas e inativas")
    void shouldFindAllActivities() {
        UUID inactive = DbFixtures.insertActivity(SeedData.SPORT_JUDO, SeedData.ROOM_JUDO, SeedData.TRAINER_CARLOS,
            LocalTime.of(9, 0), LocalTime.of(10, 0), 10, new BigDecimal("90"), false, DayOfWeek.FRIDAY);

        assertThat(activityClassRepository.findAll()).extracting(ActivityClass::getId)
            .containsExactlyInAnyOrder(SeedData.SWIMMING_CLASS, inactive);
    }

    @Test
    @DisplayName("mapeia sala, modalidade e professor e retorna null quando não existem")
    void shouldFindCatalogItemsById() {
        assertThat(activityClassRepository.findRoomById(SeedData.ROOM_POOL).getCapacity()).isEqualTo(SeedData.ROOM_POOL_CAPACITY);
        assertThat(activityClassRepository.findSportById(SeedData.SPORT_SWIMMING).getName()).isEqualTo(SeedData.SWIMMING_NAME);
        assertThat(activityClassRepository.findTrainerById(SeedData.TRAINER_JULIANA).getName()).isEqualTo(SeedData.TRAINER_JULIANA_NAME);
        assertThat(activityClassRepository.findRoomById(UUID.randomUUID())).isNull();
        assertThat(activityClassRepository.findSportById(UUID.randomUUID())).isNull();
        assertThat(activityClassRepository.findTrainerById(UUID.randomUUID())).isNull();
    }

    @Test
    @DisplayName("agenda nova mensalidade para o mês seguinte e mantém a vigente")
    void shouldScheduleNewFeeForNextMonth() {
        ActivityClass existing = activity(SeedData.SWIMMING_CLASS);
        existing.edit(
            existing.getRoom(),
            existing.getSport(),
            existing.getTrainer(),
            existing.getSchedule(),
            existing.getCapacity(),
            new BigDecimal("200")
        );

        ActivityClass saved = activityClassRepository.save(existing);
        LocalDate nextMonth = LocalDate.now().withDayOfMonth(1).plusMonths(1);

        assertThat(saved.getMonthlyFee()).isEqualByComparingTo("180");
        assertThat(activity(SeedData.SWIMMING_CLASS).getMonthlyFee()).isEqualByComparingTo("180");
        assertThat(activityClassRepository.findPriceHistory(SeedData.SWIMMING_CLASS)).extracting(BigDecimal::intValue).containsExactly(180, 200);
        assertThat(DbFixtures.text(
            "SELECT valid_from FROM activity_prices WHERE activity_id = ? AND monthly_fee = ?",
            SeedData.SWIMMING_CLASS.toString(),
            new BigDecimal("200")
        )).isEqualTo(nextMonth.toString());
    }

    @Test
    @DisplayName("substitui a mensalidade agendada quando muda de novo no mesmo mês")
    void shouldReplaceScheduledFee() {
        changeFee(activity(SeedData.SWIMMING_CLASS), "200");
        changeFee(activity(SeedData.SWIMMING_CLASS), "220");

        assertThat(activityClassRepository.findPriceHistory(SeedData.SWIMMING_CLASS)).extracting(BigDecimal::intValue)
                .containsExactly(180, 220);
        assertThat(activityClassRepository.findLatestPrice(SeedData.SWIMMING_CLASS, BigDecimal.ONE)).isEqualByComparingTo("220");
    }

    @Test
    @DisplayName("não cria histórico de preço ao salvar com a mesma mensalidade")
    void shouldNotCreateHistoryWhenFeeDoesNotChange() {
        changeFee(activity(SeedData.SWIMMING_CLASS), "180");

        assertThat(DbFixtures.count("SELECT COUNT(*) FROM activity_prices WHERE activity_id = ?",
                SeedData.SWIMMING_CLASS.toString())).isEqualTo(1);
    }
}
