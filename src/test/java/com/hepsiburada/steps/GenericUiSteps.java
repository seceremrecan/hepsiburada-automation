package com.hepsiburada.steps;

import com.hepsiburada.utils.DriverFactory;
import com.hepsiburada.utils.ElementRepository;
import com.hepsiburada.utils.StepLogger;
import com.hepsiburada.utils.ValueResolver;
import com.hepsiburada.utils.Waits;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.ScenarioDataStore;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keyword-driven GENERIC adim kutuphanesi (referans mimarideki paylasilan
 * step-jar'in bu projedeki karsiligi). Elementler element-infos JSON
 * anahtarlariyla, veriler Data_* anahtarlariyla (value-infos) gecilir.
 *
 * Her adim hem Turkce hem Ingilizce metinle cagirilabilir (@Step alias).
 * HB-TC01'in saglamlastirilmis akislari ozel adimlarda durur; bu kutuphane
 * YENI senaryolarin Java yazmadan spec/concept ile kurulabilmesi icindir.
 */
public class GenericUiSteps {

    private WebDriver driver() {
        return DriverFactory.getDriver();
    }

    private Waits waits() {
        return new Waits(driver());
    }

    @Step({"<key> elementini bul ve tikla", "Find element <key> and click"})
    public void findElementAndClick(String key) {
        StepLogger.log("Generic: '" + key + "' elementine tiklaniyor.");
        waits().clickable(ElementRepository.by(key)).click();
    }

    @Step({"<key> elementine <text> yaz", "Find element <key> and sendkey text <text>"})
    public void findElementAndType(String key, String text) {
        String resolved = ValueResolver.resolve(text);
        StepLogger.log("Generic: '" + key + "' elementine metin yaziliyor.");
        WebElement element = waits().visible(ElementRepository.by(key));
        element.clear();
        element.sendKeys(resolved);
    }

    @Step({"<key> elementi <saniye> saniye icinde gorunur olana kadar bekle",
            "Wait until element <key> is visible within <saniye> seconds"})
    public void waitUntilVisible(String key, String saniye) {
        boolean visible = waits().isVisibleWithin(
                ElementRepository.by(key), Duration.ofSeconds(Long.parseLong(saniye)));
        assertThat(visible)
                .as("'%s' elementi %s saniye icinde gorunur olmadi", key, saniye)
                .isTrue();
    }

    @Step({"<key> elementinin gorunur oldugunu dogrula", "Verify element <key> is displayed"})
    public void verifyElementDisplayed(String key) {
        assertThat(waits().isVisible(ElementRepository.by(key)))
                .as("'%s' elementi gorunur olmali", key)
                .isTrue();
    }

    @Step({"<key> elementinin metnini <storeKey> olarak sakla", "Save element <key> text as <storeKey>"})
    public void saveElementText(String key, String storeKey) {
        String text = waits().visible(ElementRepository.by(key)).getText().trim();
        ScenarioDataStore.put(storeKey, text);
        StepLogger.log("Generic: '" + key + "' metni '" + storeKey + "' olarak saklandi: " + text);
    }

    @Step({"<storeKey> degerinin <expected> icerdigini dogrula",
            "Verify stored value <storeKey> contains <expected>"})
    public void verifyStoredValueContains(String storeKey, String expected) {
        Object stored = ScenarioDataStore.get(storeKey);
        assertThat(String.valueOf(stored))
                .as("Saklanan '%s' degeri beklenen metni icermeli", storeKey)
                .contains(ValueResolver.resolve(expected));
    }

    @Step({"<key> elementine JS ile tikla", "find element <key> and click(JS)"})
    public void clickElementWithJs(String key) {
        StepLogger.log("Generic: '" + key + "' elementine JS ile tiklaniyor.");
        WebElement element = waits().visible(ElementRepository.by(key));
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
    }

    @Step({"<key> elementinin uzerine gel", "Hover element <key>"})
    public void hoverElement(String key) {
        StepLogger.log("Generic: '" + key + "' elementinin uzerine geliniyor (hover).");
        new Actions(driver())
                .moveToElement(waits().visible(ElementRepository.by(key)))
                .perform();
    }

    /**
     * Metin uzerinden tiklama (referans: findElementWithTextAndClick).
     * Once ScenarioDataStore'da saklanmis deger, sonra Data_* anahtari,
     * en son literal metin olarak cozulur.
     */
    @Step({"<text> degerine esit elemente tikla", "find element equal to <text> and click"})
    public void findElementWithTextAndClick(String text) {
        Object stored = ScenarioDataStore.get(text);
        String value = stored != null ? String.valueOf(stored) : ValueResolver.resolve(text);
        StepLogger.log("Generic: '" + value + "' metnini tasiyan elemente tiklaniyor.");
        waits().clickable(By.xpath("//*[contains(text(),'" + value + "')]")).click();
    }

    @Step({"<key> elementinin sayfada olmadigini dogrula", "Check if element <key> does not exist"})
    public void checkElementNotExist(String key) {
        boolean exists = driver().findElements(ElementRepository.by(key)).stream()
                .anyMatch(WebElement::isDisplayed);
        assertThat(exists)
                .as("'%s' elementi sayfada GORUNMEMELIYDI", key)
                .isFalse();
        StepLogger.log("Generic: '" + key + "' sayfada yok (beklenen durum).");
    }

    // ------------------------------------------------------------------
    // Data-store matematik adimlari (referans: addTextsDouble / checkNumber).
    // TR sayi bicimi normalize edilir: binlik '.' silinir, ',' -> '.'
    // ------------------------------------------------------------------

    @Step({"Text <key1> ve <key2> degerlerini topla ve <saveKey> olarak kaydet (double)",
            "Add text <key1> and <key2> and save as <saveKey> (double)"})
    public void addTextsDouble(String key1, String key2, String saveKey) {
        double first = parseTrNumber(String.valueOf(ScenarioDataStore.get(key1)));
        double second = parseTrNumber(String.valueOf(ScenarioDataStore.get(key2)));
        String result = String.valueOf(first + second);
        ScenarioDataStore.put(saveKey, result);
        StepLogger.log("Generic: " + first + " + " + second + " = " + result
                + " ('" + saveKey + "' olarak kaydedildi).");
    }

    @Step({"<textKey> metninin ondalik kismini sil ve <saveKey> olarak kaydet",
            "Remove decimal part of text <textKey> and save as <saveKey>"})
    public void removeDecimalPart(String textKey, String saveKey) {
        String raw = String.valueOf(ScenarioDataStore.get(textKey));
        String withoutDecimal = raw.split("[.,]")[0];
        ScenarioDataStore.put(saveKey, withoutDecimal);
        StepLogger.log("Generic: '" + raw + "' -> '" + withoutDecimal
                + "' ('" + saveKey + "' olarak kaydedildi).");
    }

    private double parseTrNumber(String text) {
        return Double.parseDouble(text.replace(".", "").replace(",", "."));
    }
}
