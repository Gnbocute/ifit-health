package br.ifsp.demo.ui.tests;

import br.ifsp.demo.support.SeedData;
import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.AdminActivitiesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela de atividades do admin")
class AdminActivitiesUiTest extends UiTestBase {
    @Test
    @DisplayName("mostra todas as colunas da atividade cadastrada")
    void shouldShowActivityColumns() {
        loginAsAdmin();
        AdminActivitiesPage page = new AdminActivitiesPage(driver).waitUntilLoaded();

        assertThat(page.heading()).isEqualToIgnoringCase("Atividades");
        assertThat(page.sports()).containsExactly(SeedData.SWIMMING_NAME);
        assertThat(page.cell(SeedData.SWIMMING_NAME, 1)).isEqualTo(SeedData.TRAINER_JULIANA_NAME);
        assertThat(page.cell(SeedData.SWIMMING_NAME, 2)).isEqualTo("Piscina");
        assertThat(page.cell(SeedData.SWIMMING_NAME, 3)).contains("Seg").contains("11:30–12:30");
        assertThat(page.cell(SeedData.SWIMMING_NAME, 4)).isEqualTo("0/20");
        assertThat(page.cell(SeedData.SWIMMING_NAME, 5)).isEqualTo("R$ 180,00");
        assertThat(page.cell(SeedData.SWIMMING_NAME, 6)).isEqualTo("Ativa");
    }
}
