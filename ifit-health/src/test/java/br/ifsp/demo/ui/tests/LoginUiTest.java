
package br.ifsp.demo.ui.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
class LoginUiTest {

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void shouldDisplayLoginFormElements() {
        driver.get("http://localhost:3000");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement email = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );

        WebElement emailField = driver.findElement(By.id("email"));
        WebElement passwordField = driver.findElement(By.id("pw"));
        WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));

        assertThat(emailField.isDisplayed()).isTrue();
        assertThat(passwordField.isDisplayed()).isTrue();
        assertThat(loginButton.isDisplayed()).isTrue();
        assertThat(loginButton.getText()).isEqualTo("Entrar");
    }
}
