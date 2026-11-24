package br.ifsp.demo.ui.tests;

import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.SeedData;
import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.MyActivitiesPage;
import br.ifsp.demo.ui.pages.StudentActivitiesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela de atividades disponíveis (aluno)")
class StudentActivitiesUiTest extends UiTestBase {
    private static final String SWIMMING = "NATAÇÃO";

    private UUID gymOnMonday(int startHour, int capacity) {
        return DbFixtures.insertActivity(SeedData.SPORT_WEIGHT_TRAINING, SeedData.ROOM_GYM, SeedData.TRAINER_JOHN,
                LocalTime.of(startHour, 0), LocalTime.of(startHour + 1, 0), capacity, new BigDecimal("120"), true, DayOfWeek.MONDAY);
    }

    private StudentActivitiesPage openAsNewStudent() {
        loginAsNewStudent();
        return new StudentActivitiesPage(driver).waitUntilLoaded();
    }

    @Test
    @DisplayName("mostra o card da atividade com professor, sala, horário, mensalidade e vagas")
    void shouldShowActivityCard() {
        StudentActivitiesPage page = openAsNewStudent();

        assertThat(page.sports()).containsExactly(SWIMMING);
        assertThat(page.cardText(SWIMMING))
                .contains(SeedData.TRAINER_JULIANA_NAME)
                .contains("Piscina")
                .contains("Seg · 11:30–12:30")
                .contains("R$ 180,00");
        assertThat(page.badge(SWIMMING)).isEqualTo("20 vagas");
    }

    @Test
    @DisplayName("não lista atividades inativas para o aluno")
    void shouldHideInactiveActivities() {
        DbFixtures.insertActivity(SeedData.SPORT_JUDO, SeedData.ROOM_JUDO, SeedData.TRAINER_CARLOS,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 10, new BigDecimal("90"), false, DayOfWeek.FRIDAY);

        assertThat(openAsNewStudent().sports()).containsExactly(SWIMMING);
    }

    @Test
    @DisplayName("seleciona atividades, mostra o total mensal e limpa a seleção")
    void shouldSelectActivities() {
        gymOnMonday(8, 10);
        StudentActivitiesPage page = openAsNewStudent();

        page.toggle(SWIMMING).toggle("MUSCULAÇÃO");

        assertThat(page.isSelected(SWIMMING)).isTrue();
        assertThat(page.selectionSummary()).isEqualTo("2 selecionadas · R$ 300,00/mês");
        page.clearSelection();
        assertThat(page.isSelectionBarVisible()).isFalse();
    }

    @Test
    @DisplayName("matricula após confirmar no diálogo e abre Minhas atividades")
    void shouldEnroll() {
        StudentActivitiesPage page = openAsNewStudent().toggle(SWIMMING).openConfirmation();
        assertThat(page.dialogText()).contains("Confirmar matrícula").contains("Total mensal").contains("R$ 180,00");

        MyActivitiesPage mine = page.confirm();

        assertThat(mine.toast()).isEqualTo("Matrícula realizada!");
        assertThat(mine.tab(0)).isEqualTo("Ativas (1)");
        assertThat(mine.sports()).containsExactly(SWIMMING);
    }

    @Test
    @DisplayName("mantém a seleção ao fechar o diálogo com Voltar")
    void shouldGoBackFromDialog() {
        StudentActivitiesPage page = openAsNewStudent().toggle(SWIMMING).openConfirmation().backFromDialog();

        assertThat(page.isSelected(SWIMMING)).isTrue();
        assertThat(page.currentPath()).isEqualTo("/aluno/atividades");
    }
}
