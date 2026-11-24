package br.ifsp.demo.ui.tests;

import br.ifsp.demo.support.SeedData;
import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.StudentActivitiesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela de atividades disponíveis (aluno)")
class StudentActivitiesUiTest extends UiTestBase {
    private static final String SWIMMING = "NATAÇÃO";

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
}
