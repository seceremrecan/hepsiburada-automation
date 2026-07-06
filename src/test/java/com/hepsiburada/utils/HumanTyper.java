package com.hepsiburada.utils;

import com.hepsiburada.config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Login alanlari icin insan-gibi yazma.
 *
 * Neden Actions.pause()? Proje kurali "Thread.sleep yok" — pause() ise
 * WebDriver'in kendi eylem zincirinin (W3C Actions API) parcasi olan,
 * tarayici tarafinda zamanlanan resmi bekleme adimidir; test thread'ini
 * korlemesine uyutmaz, eylem akisinin icinde calisir.
 * Tus basina gecikme, sabit degil, config'ten alinan araliktan rastgele
 * secilir; sabit araliklarla yazmak da bot imzasidir.
 */
public final class HumanTyper {

    private HumanTyper() {
    }

    public static void typeLikeHuman(WebDriver driver, WebElement field, String text) {
        int minDelay = ConfigReader.getInt("typing.delay.min.ms");
        int maxDelay = ConfigReader.getInt("typing.delay.max.ms");

        Actions actions = new Actions(driver);
        actions.click(field);
        for (char c : text.toCharArray()) {
            actions.sendKeys(String.valueOf(c))
                    .pause(Duration.ofMillis(ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1)));
        }
        actions.perform();
    }
}
