package com.hepsiburada.utils;

import com.hepsiburada.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Projedeki TEK bekleme mekanizmasi. Thread.sleep kesinlikle kullanilmaz;
 * her bekleme bir kosula baglidir (WebDriverWait).
 *
 * Sayfa nesneleri bu sinifi kullanir; boylece timeout tek yerden
 * (default.properties) yonetilir.
 */
public class Waits {

    private final WebDriver driver;
    private final Duration defaultTimeout;

    public Waits(WebDriver driver) {
        this.driver = driver;
        this.defaultTimeout = Duration.ofSeconds(ConfigReader.getInt("timeout.explicit.seconds"));
    }

    private WebDriverWait newWait(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        // Yalnizca GECICI oldugu bilinen durumlar yoksayilir ve kosul yeniden
        // denenir: DOM yeniden cizimi (stale) ve sayfa gecisi anindaki
        // "no such execution context" (JavascriptException). WebDriverException
        // gibi genis bir ata BILEREK yoksayilmaz; ornegin gecersiz bir selector
        // aninda patlamali, timeout'a kadar gizlenmemeli.
        wait.ignoring(StaleElementReferenceException.class);
        wait.ignoring(JavascriptException.class);
        return wait;
    }

    public WebElement visible(By locator) {
        return newWait(defaultTimeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return newWait(defaultTimeout).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public boolean invisible(By locator) {
        return newWait(defaultTimeout).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /** Ozel kosullar icin genel kapi (or. urun sayisi > N). */
    public <T> T until(ExpectedCondition<T> condition) {
        return newWait(defaultTimeout).until(condition);
    }

    /**
     * Varsayilan timeout icinde gorunur olursa true, olmazsa false.
     * "Gorunur mu?" sorgulari icin tek kalip; exception firlatmaz.
     */
    public boolean isVisible(By locator) {
        return isVisibleWithin(locator, defaultTimeout);
    }

    /**
     * Kisa, "varsa isle yoksa gec" kontrolleri icin (popup'lar gibi).
     * Bulamazsa exception firlatmak yerine false doner; ana akisi bloklamaz.
     */
    public boolean isVisibleWithin(By locator, Duration timeout) {
        try {
            newWait(timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
