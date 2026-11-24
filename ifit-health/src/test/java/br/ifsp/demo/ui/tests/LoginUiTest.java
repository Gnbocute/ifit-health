package br.ifsp.demo.ui.tests;

import br.ifsp.demo.ui.UiTestBase;
import br.ifsp.demo.ui.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
@DisplayName("UI - Tela de login")
class LoginUiTest extends UiTestBase {
    @Test
    @DisplayName("exibe o formulário de login")
    void shouldDisplayLoginFormElements() {
        LoginPage login = loginPage();

        assertThat(login.pageTitle()).isEqualTo(LoginPage.TITLE);
        assertThat(login.isFormDisplayed()).isTrue();
        assertThat(login.submitText()).isEqualTo("Entrar");
    }
}
