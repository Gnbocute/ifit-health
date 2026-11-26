package br.ifsp.demo.ui.tests;

import br.ifsp.demo.support.DbFixtures;
import br.ifsp.demo.support.SeedData;
import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.ReportsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela de relatórios")
class ReportsUiTest extends UiTestBase {
    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate MARCH_31 = LocalDate.of(2026, 3, 31);

    private ReportsPage openReports() {
        loginAsAdmin();
        return new ReportsPage(driver).open();
    }

    @Test
    @DisplayName("mostra os totais e a linha de cada atividade do período")
    void shouldGenerateReport() {
        DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE, MARCH_1);
        DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE, MARCH_1);

        ReportsPage page = openReports().setPeriod(MARCH_1, MARCH_31).generate().waitForResult();

        assertThat(page.period()).isEqualTo("Período: 01/03/2026 a 31/03/2026");
        assertThat(page.stat("Matriculados")).isEqualTo("2");
        assertThat(page.stat("Receita bruta acumulada")).isEqualTo("R$ 360,00");
        assertThat(page.stat("Ocupação média")).isEqualTo("10.0%");
        assertThat(page.activities()).singleElement().asString().startsWith(SeedData.SWIMMING_NAME);
        assertThat(page.cell(0, 2)).isEqualTo("20");
        assertThat(page.cell(0, 3)).isEqualTo("2");
        assertThat(page.cell(0, 4)).isEqualTo("R$ 360,00");
        assertThat(page.cell(0, 5)).isEqualTo("10.0%");
    }

    @Test
    @DisplayName("bloqueia data final anterior à inicial na tela de relatórios")
    void shouldRejectInvalidPeriod() {
        ReportsPage page = openReports().setPeriod(MARCH_31, MARCH_1).generate();

        assertThat(page.errorBox()).isEqualTo("A data final deve ser igual ou posterior à data inicial.");
    }

    @Test
    @DisplayName("mostra tabela vazia quando a modalidade não tem atividades")
    void shouldFilterBySport() {
        DbFixtures.enrollNewCustomer(SeedData.SWIMMING_CLASS, SeedData.SWIMMING_FEE, MARCH_1);

        ReportsPage page = openReports().setPeriod(MARCH_1, MARCH_31).chooseSport("Judô").generate().waitForResult();

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.stat("Matriculados")).isEqualTo("0");
    }
}
