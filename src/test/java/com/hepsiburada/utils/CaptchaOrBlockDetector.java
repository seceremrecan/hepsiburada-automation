package com.hepsiburada.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Locale;

/**
 * CAPTCHA / bot-engel sayfasi TESPITI (asla asma/cozme yok — kapsam disi).
 *
 * Kullanim: kritik adimlarin hemen ardindan (ozellikle login submit sonrasi)
 * checkAndFailIfBlocked(driver, "login") cagrilir. Engel varsa screenshot
 * alinir ve BotDetectionException firlatilir; test okunur mesajla fail eder.
 *
 * Tespit iki katmanli:
 *  1) Bilinen CAPTCHA saglayicilarina ait DOM izleri (findElements ile,
 *     BEKLEMESIZ — engel yoksa testi yavaslatmamali).
 *  2) Sayfa basligi/govdesinde engel metni anahtar kelimeleri.
 *
 * TODO(locator): Asagidaki DOM isaretleri genel/sezgiseldir (reCAPTCHA,
 * hCaptcha, PerimeterX/HUMAN). Hepsiburada'nin GERCEK engel sayfasina ait
 * dogrulanmis bir selector elimizde yok; gercek bir engelle karsilasilinca
 * screenshots/ altindaki goruntuden teyit edilip liste guncellenmeli.
 */
public final class CaptchaOrBlockDetector {

    private static final List<By> DOM_MARKERS = List.of(
            By.cssSelector("iframe[src*='recaptcha']"),
            By.cssSelector("iframe[src*='hcaptcha']"),
            By.cssSelector("iframe[title*='captcha']"),
            By.id("px-captcha") // PerimeterX / HUMAN "Press & Hold" widget'i
    );

    private static final List<String> TEXT_MARKERS = List.of(
            "robot olmadığın",          // "robot olmadığını doğrula..."
            "güvenlik doğrulaması",
            "doğrulama gerekiyor",
            "erişim engellendi",
            "access denied",
            "unusual traffic",
            "are you a human"
    );

    /**
     * SMS/OTP dogrulama ekrani metin isaretleri. Bot-engelinden AYRI tutulur:
     * OTP bir guvenlik adimidir, kullaniciya "elle bir kez gir" onerisiyle
     * OtpRequiredException uzerinden raporlanir.
     */
    private static final List<String> OTP_TEXT_MARKERS = List.of(
            "doğrulama kodu",
            "sms ile gönder",
            "telefonunuza gönder",
            "cep telefonunuza"
    );

    private CaptchaOrBlockDetector() {
    }

    /** OTP ekrani gorunuyorsa screenshot + OtpRequiredException; yoksa sessizce doner. */
    public static void checkForOtpChallenge(WebDriver driver, String context) {
        String title = safeLower(driver.getTitle());
        String bodyText = safeLower(bodyTextOf(driver));
        for (String marker : OTP_TEXT_MARKERS) {
            String needle = marker.toLowerCase(Locale.forLanguageTag("tr"));
            if (title.contains(needle) || bodyText.contains(needle)) {
                var screenshot = ScreenshotUtil.capture(driver, "otp_ekrani_" + context);
                throw new OtpRequiredException(context + " — isaret: \"" + marker + "\"", screenshot);
            }
        }
    }

    /** Engel yoksa sessizce doner; varsa screenshot + BotDetectionException. */
    public static void checkAndFailIfBlocked(WebDriver driver, String context) {
        String marker = findBlockMarker(driver);
        if (marker != null) {
            var screenshot = ScreenshotUtil.capture(driver, "bot_block_" + context);
            throw new BotDetectionException(context, marker, screenshot);
        }
    }

    private static String findBlockMarker(WebDriver driver) {
        for (By domMarker : DOM_MARKERS) {
            // findElements bos liste doner, bekleme yapmaz — mutlu yolda maliyeti ~0.
            if (!driver.findElements(domMarker).isEmpty()) {
                return domMarker.toString();
            }
        }
        String title = safeLower(driver.getTitle());
        String bodyText = safeLower(bodyTextOf(driver));
        for (String textMarker : TEXT_MARKERS) {
            String needle = textMarker.toLowerCase(Locale.forLanguageTag("tr"));
            if (title.contains(needle) || bodyText.contains(needle)) {
                return "sayfa metni: \"" + textMarker + "\"";
            }
        }
        return null;
    }

    private static String bodyTextOf(WebDriver driver) {
        try {
            return driver.findElement(By.tagName("body")).getText();
        } catch (RuntimeException e) {
            return ""; // body henuz yoksa (bos sayfa vb.) metin kontrolunu atla
        }
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.forLanguageTag("tr"));
    }
}
