package com.hepsiburada.steps;

import com.hepsiburada.utils.BotDetection;
import com.hepsiburada.utils.DriverFactory;
import com.hepsiburada.utils.ElementRepository;
import com.hepsiburada.utils.HumanTyper;
import com.hepsiburada.utils.PopupHandler;
import com.hepsiburada.utils.StepLogger;
import com.hepsiburada.utils.TextNormalizer;
import com.hepsiburada.utils.ValueResolver;
import com.hepsiburada.utils.Waits;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.ScenarioDataStore;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keyword-driven (anahtar-guodumlu) TEK generic adim kutuphanesi.
 *
 * Katmanli mimari:
 *   spec (Turkce is dili)  ->  concept/.cpt (generic adimlara acar)  ->  bu sinif
 *
 * Hicbir adim belirli bir sayfaya/butona gomulu DEGILDIR; her adim bir element
 * ANAHTARI (element-infos JSON) ve/veya metin parametresi alir. Boylece yeni
 * senaryolar Java yazmadan, yalnizca spec + concept + JSON ile kurulabilir.
 * Aksiyonlar da dogrulamalar (AssertJ) da burada; ayri POM sayfasi yoktur.
 */
public class StepImplementation {

    private static final Locale TR = Locale.forLanguageTag("tr-TR");

    private WebDriver driver() {
        return DriverFactory.getDriver();
    }

    private Waits waits() {
        return new Waits(driver());
    }

    private void dismissPopups() {
        PopupHandler.dismissPopupsIfPresent(driver());
    }

    private void scrollIntoCenter(WebElement element) {
        ((JavascriptExecutor) driver()).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
    }

    /** Tiklamayi kesen bir katman (reklam bandi, sabit header, modal) varsa JS ile tiklar. */
    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
    }

    // ------------------------------------------------------------------
    // Navigasyon / genel
    // ------------------------------------------------------------------

    @Step({"Navigate to <url>", "<url> adresine git"})
    public void navigateTo(String url) {
        String resolved = ValueResolver.resolve(url);
        StepLogger.log("Adres aciliyor: " + resolved);
        driver().get(resolved);
        dismissPopups();
        // Bot korumasi bazen daha ana sayfada devreye girer; erken teshis et
        // (engel yoksa maliyeti ~0, findElements beklemesiz kontrol).
        BotDetection.checkAndFailIfBlocked(driver(), "ana-sayfa");
    }

    @Step({"Dismiss popups", "Acilan popuplari kapat"})
    public void dismissPopupsStep() {
        dismissPopups();
        StepLogger.log("Acilan popup/cerez katmanlari kapatildi (varsa).");
    }

    // ------------------------------------------------------------------
    // Tiklama / yazma
    // ------------------------------------------------------------------

    @Step({"Find element <key> and click", "<key> elementine tikla"})
    public void clickElement(String key) {
        dismissPopups();
        By by = ElementRepository.by(key);
        StepLogger.log("'" + key + "' elementine tiklaniyor.");
        try {
            WebElement element = waits().clickable(by);
            scrollIntoCenter(element);
            element.click();
        } catch (ElementClickInterceptedException e) {
            // Acik kalmis bir modal/overlay tiklamayi kesti: kaldirip bir kez daha dene.
            PopupHandler.removeBlockingCheckoutOverlay(driver());
            dismissPopups();
            try {
                waits().clickable(by).click();
            } catch (ElementClickInterceptedException stillIntercepted) {
                // Kaldirilamayan katman (ornegin sayfanin tepesindeki reklam bandi)
                // hedefin uzerine biniyor: son care olarak JS ile tikla.
                StepLogger.log("'" + key + "' tiklamasi kesildi; JS ile tiklaniyor.");
                jsClick(driver().findElement(by));
            }
        }
    }

    @Step({"Click element <key> if present", "Varsa <key> elementine tikla"})
    public void clickElementIfPresent(String key) {
        By by = ElementRepository.by(key);
        if (!waits().isVisibleWithin(by, Duration.ofSeconds(4))) {
            StepLogger.log("'" + key + "' gorunmedi; opsiyonel adim atlaniyor.");
            return;
        }
        try {
            WebElement element = driver().findElement(by);
            scrollIntoCenter(element);
            try {
                element.click();
            } catch (ElementClickInterceptedException intercepted) {
                // Element GORUNUYOR ama uzerine bir katman binmis (reklam bandi vb.).
                // Adim opsiyonel olsa da element mevcut; tiklamayi atlamak yerine JS ile tikla.
                StepLogger.log("Opsiyonel '" + key + "' tiklamasi kesildi; JS ile tiklaniyor.");
                jsClick(element);
            }
            StepLogger.log("Opsiyonel: '" + key + "' elementine tiklandi.");
        } catch (WebDriverException e) {
            StepLogger.log("Opsiyonel '" + key + "' tiklanamadi, gecildi: " + e.getMessage());
        }
    }

    @Step({"Click element <key> unless <gorunecekKey> is visible",
            "<gorunecekKey> gorunmuyorsa <key> elementine tikla"})
    public void clickUnlessVisible(String key, String gorunecekKey) {
        // Iki adimli login (once e-posta -> Devam -> sifre) ile tek ekranli login
        // (e-posta + sifre birlikte) arasindaki farki generic sekilde ele alir:
        // hedef alan (sifre) zaten gorunuyorsa ara "Devam/Giris" tiklamasi ATLANIR.
        if (waits().isVisibleWithin(ElementRepository.by(gorunecekKey), Duration.ofSeconds(2))) {
            StepLogger.log("'" + gorunecekKey + "' zaten gorunur; '" + key + "' tiklamasi atlandi (tek ekranli form).");
            return;
        }
        clickElement(key);
    }

    @Step({"Find element <key> and sendkey text <text>", "<key> elementine <text> yaz"})
    public void typeIntoElement(String key, String text) {
        dismissPopups();
        String resolved = ValueResolver.resolve(text);
        StepLogger.log("'" + key + "' elementine metin yaziliyor (insan-gibi).");
        WebElement element = waits().visible(ElementRepository.by(key));
        element.clear();
        HumanTyper.typeLikeHuman(driver(), element, resolved);
    }

    @Step({"Find element <key> and search text <text>", "<key> elementine <text> yazip ara"})
    public void typeAndSearch(String key, String text) {
        dismissPopups();
        String resolved = ValueResolver.resolve(text);
        By by = ElementRepository.by(key);
        StepLogger.log("'" + key + "' arama kutusuna '" + resolved + "' yazilip Enter'a basiliyor.");
        // Bazi arama kutulari once pasif bir kopya olarak gelir; tiklayinca GERCEK
        // input ile degistirilir (swap). Bu yuzden: GORUNUR + ETKIN adayi sec, yaz;
        // swap aninda stale/interactable hatasi cikarsa taze elemanla yeniden dene.
        final int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                WebElement box = activeInput(by);
                // Kutuyu aktive et: normal tiklama ustteki sarmalayici tarafindan
                // kesilebiliyor; o durumda JS ile odaklan.
                try {
                    box.click();
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver()).executeScript("arguments[0].focus();", box);
                }
                box = activeInput(by);
                box.sendKeys(resolved);
                if (String.valueOf(box.getAttribute("value")).contains(resolved)) {
                    box.sendKeys(Keys.ENTER);
                    dismissPopups();
                    return;
                }
                box.clear();
            } catch (WebDriverException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("'" + key + "' arama kutusuna '" + resolved
                + "' " + maxAttempts + " denemede yazilamadi (arama bileseni swap davranisi).");
    }

    /** Locator'a uyan, su an GORUNUR ve ETKIN olan ilk input'u dondurur (swap'e dayanikli). */
    private WebElement activeInput(By by) {
        return waits().until(d -> d.findElements(by).stream()
                .filter(el -> el.isDisplayed() && el.isEnabled())
                .findFirst().orElse(null));
    }

    @Step({"Switch to new tab", "Acilan yeni sekmeye gec"})
    public void switchToNewTab() {
        List<String> handles = new ArrayList<>(driver().getWindowHandles());
        driver().switchTo().window(handles.get(handles.size() - 1));
        dismissPopups();
        StepLogger.log("Son acilan sekmeye gecildi.");
    }

    // ------------------------------------------------------------------
    // Saklama
    // ------------------------------------------------------------------

    @Step({"Store attribute <attr> of element <key> as <storeKey>",
            "<key> elementinin <attr> ozniteligini <storeKey> olarak sakla"})
    public void storeAttribute(String attr, String key, String storeKey) {
        String value = waits().visible(ElementRepository.by(key)).getAttribute(attr);
        String trimmed = value == null ? "" : value.trim();
        ScenarioDataStore.put(storeKey, trimmed);
        StepLogger.log("'" + key + "' [" + attr + "] -> '" + storeKey + "' olarak saklandi: " + trimmed);
    }

    // ------------------------------------------------------------------
    // Dogrulamalar (assertion)
    // ------------------------------------------------------------------

    @Step({"Verify element <key> is displayed", "<key> elementinin gorunur oldugunu dogrula"})
    public void verifyDisplayed(String key) {
        boolean visible = waits().isVisible(ElementRepository.by(key));
        if (!visible) {
            // Element gelmediyse once NEDENI okunur raporla: OTP ekrani mi,
            // bot-engel/CAPTCHA mi? Ikisi de degilse asagida normal assertion.
            BotDetection.checkForOtpChallenge(driver(), key);
            BotDetection.checkAndFailIfBlocked(driver(), key);
        }
        assertThat(visible)
                .as("'%s' elementi gorunur olmali", key)
                .isTrue();
        StepLogger.log("[OK] '" + key + "' gorunur.");
    }

    @Step({"Verify element <key> or <altKey> is displayed",
            "<key> veya <altKey> elementinin gorunur oldugunu dogrula"})
    public void verifyDisplayedEither(String key, String altKey) {
        // Iki gecerli kanittan biri yeterli. Ornek: sepete ekleme onayi iki
        // bicimde gelebiliyor — dogrudan "Urun sepetinizde" mesaji (normal akis)
        // VEYA onerilen-satici modali sonrasi sepet rozetinin gorunmesi. Once
        // asil kanit kisa beklenir; gelmezse alternatif kanit kontrol edilir.
        boolean visible = waits().isVisibleWithin(ElementRepository.by(key), Duration.ofSeconds(6))
                || waits().isVisibleWithin(ElementRepository.by(altKey), Duration.ofSeconds(4));
        if (!visible) {
            BotDetection.checkForOtpChallenge(driver(), key);
            BotDetection.checkAndFailIfBlocked(driver(), key);
        }
        assertThat(visible)
                .as("'%s' veya '%s' elementlerinden biri gorunur olmali", key, altKey)
                .isTrue();
        StepLogger.log("[OK] '" + key + "' veya '" + altKey + "' gorunur.");
    }

    @Step({"Wait until element <key> is visible within <saniye> seconds",
            "<key> elementi <saniye> saniye icinde gorunur olana kadar bekle"})
    public void waitUntilVisible(String key, String saniye) {
        boolean visible = waits().isVisibleWithin(
                ElementRepository.by(key), Duration.ofSeconds(Long.parseLong(saniye)));
        assertThat(visible)
                .as("'%s' elementi %s saniye icinde gorunur olmadi", key, saniye)
                .isTrue();
    }

    @Step({"Current url should contain <beklenen>", "Gecerli adres <beklenen> icermeli"})
    public void verifyUrlContains(String beklenen) {
        String expected = ValueResolver.resolve(beklenen).toLowerCase(TR);
        assertThat(driver().getCurrentUrl().toLowerCase(TR))
                .as("Adres '%s' icermeli", expected)
                .contains(expected);
        StepLogger.log("[OK] Adres '" + expected + "' iceriyor.");
    }

    @Step({"Verify text of element <key> contains stored <storeKey>",
            "<key> elementinin metni <storeKey> olarak saklanani icermeli"})
    public void verifyElementTextContainsStored(String key, String storeKey) {
        String actual = waits().visible(ElementRepository.by(key)).getText();
        String stored = String.valueOf(ScenarioDataStore.get(storeKey));
        assertThat(TextNormalizer.matchesLoosely(actual, stored))
                .as("'%s' metni saklanan '%s' degerini icermeli. Gorunen: '%s'", key, stored, actual)
                .isTrue();
        StepLogger.log("[OK] '" + key + "' metni saklanan '" + storeKey + "' ile eslesti.");
    }

    @Step({"Verify one of elements <key> contains stored <storeKey>",
            "<key> elementlerinden biri <storeKey> olarak saklanani icermeli"})
    public void verifyAnyElementContainsStored(String key, String storeKey) {
        By by = ElementRepository.by(key);
        waits().isVisible(by); // en az bir kart yuklensin diye bekle (bos liste hata degil)
        String stored = String.valueOf(ScenarioDataStore.get(storeKey));
        List<String> seen = new ArrayList<>();
        boolean found = false;
        for (WebElement el : driver().findElements(by)) {
            String text = el.getText();
            seen.add(text);
            if (TextNormalizer.matchesLoosely(text, stored)) {
                found = true;
                break;
            }
        }
        assertThat(found)
                .as("'%s' elementlerinden hicbiri saklanan '%s' degerini icermiyor. Gorunenler: %s",
                        key, stored, seen)
                .isTrue();
        StepLogger.log("[OK] '" + key + "' listesinde saklanan '" + storeKey + "' bulundu.");
    }
}
