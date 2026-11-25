package br.ifsp.demo.ui.tests;

import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.MyActivitiesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela minhas atividades (aluno)")
class MyActivitiesUiTest extends UiTestBase {
    @Test
    @DisplayName("mostra abas vazias para aluno sem matrícula")
    void shouldShowEmptyState() {
        loginAsNewStudent();

        MyActivitiesPage page = new MyActivitiesPage(driver).open();

        assertThat(page.tab(0)).isEqualTo("Ativas (0)");
        assertThat(page.tab(1)).isEqualTo("Histórico (0)");
        assertThat(page.panelText()).isEqualTo("Você não possui matrículas ativas.");
        assertThat(page.showHistory().panelText()).isEqualTo("Nenhuma atividade encerrada.");
    }
}
