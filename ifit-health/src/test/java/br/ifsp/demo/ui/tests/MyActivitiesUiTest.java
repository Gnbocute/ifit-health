package br.ifsp.demo.ui.tests;

import br.ifsp.demo.support.SeedData;
import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.MyActivitiesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela minhas atividades (aluno)")
class MyActivitiesUiTest extends UiTestBase {
    private static final String SWIMMING = "NATAÇÃO";
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private MyActivitiesPage openEnrolledInSwimming() {
        StudentAccount student = registerStudent();
        enrollViaApi(student, SeedData.SWIMMING_CLASS);
        loginPage().loginAs(student.email(), student.password()).waitForPath("/aluno/atividades");
        return new MyActivitiesPage(driver).open();
    }

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

    @Test
    @DisplayName("mostra a matrícula ativa com data de início e mensalidade")
    void shouldShowActiveEnrollment() {
        MyActivitiesPage page = openEnrolledInSwimming();

        assertThat(page.tab(0)).isEqualTo("Ativas (1)");
        assertThat(page.cardText(SWIMMING))
                .contains(SeedData.TRAINER_JULIANA_NAME)
                .contains("Seg 11:30–12:30")
                .contains("Início: " + LocalDate.now().format(BR))
                .contains("R$ 180,00");
    }
}
