package br.ifsp.demo.ui.tests;

import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.FakeData;
import br.ifsp.demo.support.SeedData;
import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.ActivityFormPage;
import br.ifsp.demo.ui.pages.AdminActivitiesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela de edição de atividade")
class EditActivityUiTest extends UiTestBase {
    private ActivityFormPage openSwimming() {
        loginAsAdmin();
        return new AdminActivitiesPage(driver).waitUntilLoaded().edit(SeedData.SWIMMING_NAME);
    }

    @Test
    @DisplayName("carrega os dados atuais da atividade na edição")
    void shouldLoadCurrentData() {
        ActivityFormPage form = openSwimming();

        assertThat(form.selectedSport()).isEqualTo(SeedData.SWIMMING_NAME);
        assertThat(form.selectedRoom()).startsWith("Piscina");
        assertThat(form.selectedTrainer()).isEqualTo(SeedData.TRAINER_JULIANA_NAME);
        assertThat(form.startTime()).isEqualTo("11:30");
        assertThat(form.capacity()).isEqualTo("20");
        assertThat(form.monthlyFee()).isEqualTo("180");
    }

    @Test
    @DisplayName("salva nova capacidade e mostra na lista")
    void shouldEditCapacity() {
        int capacity = FakeData.faker().number().numberBetween(1, 20);

        AdminActivitiesPage list = openSwimming().setCapacity(String.valueOf(capacity)).submitAndReturnToList();

        assertThat(list.toast()).isEqualTo("Atividade atualizada!");
        assertThat(list.cell(SeedData.SWIMMING_NAME, 4)).isEqualTo("0/" + capacity);
    }

    @Test
    @DisplayName("mostra a nova mensalidade ao admin depois de editar")
    void shouldEditFee() {
        AdminActivitiesPage list = openSwimming().setMonthlyFee("215.50").submitAndReturnToList();

        assertThat(list.cell(SeedData.SWIMMING_NAME, 5)).isEqualTo("R$ 215,50");
    }

    @Test
    @DisplayName("mostra o erro do back-end quando a capacidade é menor que os matriculados")
    void shouldRejectCapacityBelowEnrolled() {
        DbFixtures.fillActivity(SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE, 2);

        ActivityFormPage form = openSwimming().setCapacity("1").submit();

        assertThat(form.apiError()).isEqualTo("New capacity cannot be lower than enrolled students: 2");
    }
}
