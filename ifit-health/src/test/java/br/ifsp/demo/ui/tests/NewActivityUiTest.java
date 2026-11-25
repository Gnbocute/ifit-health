package br.ifsp.demo.ui.tests;

import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.ActivityFormPage;
import br.ifsp.demo.ui.pages.AdminActivitiesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela de nova atividade")
class NewActivityUiTest extends UiTestBase {
    private ActivityFormPage form;
    @Override
    protected void setInitialPage() {
        loginAsAdmin();
        form = new ActivityFormPage(driver).openNew();
    }

    @Test
    @DisplayName("cria atividade válida e mostra na lista")
    void shouldCreateActivity() {
        int capacity = FakeData.faker().number().numberBetween(5, 31);
        int fee = FakeData.faker().number().numberBetween(80, 250);

        form.fill("Musculação", "Sala de Musculação", "John Doe", List.of("Terça", "Quinta"),
            "08:00", "09:00", String.valueOf(capacity), String.valueOf(fee));
        AdminActivitiesPage list = form.submitAndReturnToList();

        assertThat(list.toast()).isEqualTo("Atividade cadastrada!");
        assertThat(list.cell("Musculação", 3)).contains("Ter · Qui").contains("08:00–09:00");
        assertThat(list.cell("Musculação", 4)).isEqualTo("0/" + capacity);
        assertThat(list.cell("Musculação", 5)).isEqualTo("R$ " + fee + ",00");
        assertThat(DbFixtures.count("SELECT COUNT(*) FROM activity_class")).isEqualTo(2);
    }

    @Test
    @DisplayName("lista todos os campos obrigatórios com o formulário vazio")
    void shouldShowRequiredErrors() {
        form.submit();

        assertThat(form.formErrors()).containsExactly("Selecione a modalidade.",
            "Selecione a sala.",
            "Selecione o professor.",
            "Selecione ao menos um dia da semana.",
            "Informe horário de início e fim.",
            "Capacidade deve ser um número inteiro positivo.",
            "Mensalidade deve ser maior ou igual a zero.");
        assertThat(form.currentPath()).isEqualTo("/admin/atividades/nova");
    }
}
