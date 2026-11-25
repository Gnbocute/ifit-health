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

    @Test
    @DisplayName("insere a atividade e o primeiro preço válido a partir de hoje")
    void shouldInsertActivityWithInitialPrice() {
        ActivityClass activity = activityClassRepository.save(newGymClass(12, "150.50"));

        assertThat(DbFixtures.text("SELECT weekday FROM activity_class WHERE id = ?", activity.getId().toString()))
                .isEqualTo("TUESDAY,THURSDAY");
        assertThat(DbFixtures.text("SELECT valid_from FROM activity_prices WHERE activity_id = ?", activity.getId().toString()))
                .isEqualTo(LocalDate.now().toString());
    }
}
