package com.hepsiburada.pages;

import com.hepsiburada.config.ConfigReader;
import com.hepsiburada.utils.CaptchaOrBlockDetector;
import com.hepsiburada.utils.HumanTyper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Ana sayfadaki "Giris Yap" akisi + iki adimli login formu
 * (once e-posta -> devam -> sonra sifre ekrani).
 *
 * Akis: openHomePage() -> goToLoginForm() -> loginWith(user, pass)
 * Login submit'in HEMEN ardindan bot-engel kontrolu yapilir (proje kurali):
 * engel varsa BotDetectionException ile okunur sekilde fail edilir, asilmaz.
 */
public class LoginPage extends BasePage {

    // DevTools'ta gercek DOM uzerinde dogrulanmis locator'lar (Faz 7).
    private static final By HEADER_ACCOUNT_AREA = By.cssSelector("[data-test-id='account']");
    /**
     * Hesap alanina tiklaninca acilan menudeki "Giris Yap" satiri.
     * Metin tabanli secici; 2026-07-06 kosusunun fail screenshot'indan dogrulandi.
     * (Header'daki ana buton "Giris Yap veya uye ol" metnini tasidigi icin
     * normalize-space esitligi yalnizca menu satirini yakalar.)
     */
    private static final By ACCOUNT_MENU_LOGIN_LINK =
            By.xpath("//*[self::a or self::button][normalize-space()='Giriş Yap']");
    private static final By EMAIL_OR_PHONE_INPUT = By.id("txtUserName");
    /**
     * E-posta adimindaki "devam" butonu ile sifre adimindaki "Giris yap" butonu
     * ayni id'yi (btnLogin) tasiyor; iki buton ayni anda gorunur olmadigi icin
     * ayni locator iki asamada da guvenle kullanilabiliyor.
     */
    private static final By LOGIN_ACTION_BUTTON = By.cssSelector("button#btnLogin");
    /**
     * DOM'da ayni ID'li farkli tipte eleman gorulebildiginden locator
     * bilerek input etiketiyle sinirlandirildi (input#txtPassword).
     */
    private static final By PASSWORD_INPUT = By.cssSelector("input#txtPassword");
    /** Login sonrasi header'daki "Hesabim" alani (basari kaniti). */
    private static final By LOGGED_IN_ACCOUNT_LABEL =
            By.cssSelector("a[data-test-id='account'][title='Hesabım']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /** Ana sayfayi acar; ilk yuklemede cikan popuplari (cerez bandi vb.) kapatir. */
    public void openHomePage() {
        driver.get(ConfigReader.getRequired("app.base.url"));
        dismissPopups();
        // Bot korumasi bazen daha ana sayfada devreye girer; erken teshis edelim.
        CaptchaOrBlockDetector.checkAndFailIfBlocked(driver, "ana-sayfa");
    }

    /**
     * Kalici profil (chrome.profile.dir) onceki oturumu hatirlayabilir;
     * header'da "Hesabım" gorunuyorsa kullanici ZATEN girisli demektir.
     */
    public boolean isAlreadyLoggedIn() {
        return driver.findElements(LOGGED_IN_ACCOUNT_LABEL).stream()
                .anyMatch(WebElement::isDisplayed);
    }

    /** Header'daki hesap alanina tiklayip login formunu acar. */
    public void goToLoginForm() {
        dismissPopups();
        if (isAlreadyLoggedIn()) {
            com.hepsiburada.utils.StepLogger.log(
                    "Kalici profil onceki oturumu hatirliyor — kullanici zaten girisli, login formu atlaniyor.");
            return;
        }
        // Site uc davranis gosterebiliyor (kosudan kosuya degisti):
        //  (a) dogrudan login sayfasina yonlenme -> e-posta alani gelir,
        //  (b) hesap menusu acilir -> menudeki "Giris Yap"a tiklanmali,
        //  (c) tiklama arada yutuluyor -> hover + yeniden deneme gerekir.
        // Bu yuzden: hover'la tikla, kisa sure sonucu yokla, olmadiysa tekrar dene.
        org.openqa.selenium.interactions.Actions actions =
                new org.openqa.selenium.interactions.Actions(driver);
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            WebElement account = waits.clickable(HEADER_ACCOUNT_AREA);
            actions.moveToElement(account)
                    .pause(java.time.Duration.ofMillis(300))
                    .click()
                    .perform();
            // (a) form dogrudan geldi mi? (kisa yoklama)
            if (waits.isVisibleWithin(EMAIL_OR_PHONE_INPUT, java.time.Duration.ofSeconds(4))) {
                return;
            }
            // (b) menu acildi mi? GORUNURLUK kontrol edilir — link DOM'da
            // gizli olarak surekli bulunabildiginden varlik kontrolu yaniltir.
            if (driver.findElements(ACCOUNT_MENU_LOGIN_LINK).stream().anyMatch(WebElement::isDisplayed)) {
                waits.clickable(ACCOUNT_MENU_LOGIN_LINK).click();
                waits.visible(EMAIL_OR_PHONE_INPUT);
                return;
            }
            // (c) ikisi de olmadi: hover + tiklama dongunun basinda tekrarlanir.
        }
        throw new IllegalStateException(
                "Login formuna ulasilamadi: hesap menusu/form " + maxAttempts
                        + " denemede acilmadi (screenshots/ klasorunu kontrol edin).");
    }

    /**
     * Iki adimli giris yapar. Bilgiler cagirana parametre olarak gelir;
     * bu sinif config'e degil, verilen degere calisir (yeniden kullanilabilirlik).
     * Alanlar insan-gibi rastgele gecikmeli yazilir (bot tespiti onlemi).
     */
    public void loginWith(String emailOrPhone, String password) {
        if (isAlreadyLoggedIn()) {
            com.hepsiburada.utils.StepLogger.log(
                    "Kullanici zaten girisli (kalici profil) — kimlik girisi atlaniyor.");
            return;
        }
        // E-posta/telefon alanini doldur
        WebElement emailField = waits.visible(EMAIL_OR_PHONE_INPUT);
        HumanTyper.typeLikeHuman(driver, emailField, emailOrPhone);

        // Form iki bicimde gelebiliyor: tek ekran (e-posta + sifre birlikte,
        // 2026-07-06 screenshot'inda goruldu) veya iki adimli (once e-posta ->
        // devam -> sifre). Sifre alani zaten gorunuyorsa ara tiklama atlanir.
        boolean passwordAlreadyVisible = driver.findElements(PASSWORD_INPUT).stream()
                .anyMatch(WebElement::isDisplayed);
        if (!passwordAlreadyVisible) {
            waits.clickable(LOGIN_ACTION_BUTTON).click();
        }

        // Sifre + giris yap
        WebElement passwordField = waits.visible(PASSWORD_INPUT);
        HumanTyper.typeLikeHuman(driver, passwordField, password);
        waits.clickable(LOGIN_ACTION_BUTTON).click();

        // Proje kurali: submit'in hemen ardindan CAPTCHA/engel + OTP kontrolu.
        CaptchaOrBlockDetector.checkAndFailIfBlocked(driver, "login-submit");
        CaptchaOrBlockDetector.checkForOtpChallenge(driver, "login-submit");
        dismissPopups(); // login sonrasi cikabilecek kampanya/bulten popuplari
    }

    /**
     * Login basarisinin KANITINI dondurur (assertion step katmaninda yapilir):
     * header'daki hesap alanindan "Hesabım" ifadesi cikarilmis kullanici adi.
     */
    public String getDisplayedUsername() {
        String text = waits.visible(LOGGED_IN_ACCOUNT_LABEL).getText();
        return text.replace("Hesabım", "").trim();
    }
}