package com.hepsiburada.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * CAPTCHA / bot-engel ve SMS-OTP TESPITI (tek dosyada toplanmis konu).
 *
 * Ilke: dogrulama mekanizmalari ASLA asilmaz/cozulmez (kapsam disi). Sadece
 * tespit edilir; ekran goruntusu alinip okunur bir mesajla test fail edilir.
 * Kullanim: kritik bir dogrulama beklenenden gec/gorunmez kaldiginda cagirilir
 * (or. login sonrasi kullanici adi ya da arama grid'i gelmezse — neden CAPTCHA
 * veya OTP mi diye burada bakilir).
 *
 * Onceden 3 ayri dosyaydi (CaptchaOrBlockDetector + BotDetectionException +
 * OtpRequiredException); ayni konuya ait olduklari icin tek dosyada, ic ice
 * exception siniflariyla birlestirildi (daha az dosya, daha okunur).
 */
public final class BotDetection {

    private static final Locale TR = Locale.forLanguageTag("tr");

    private static final List<By> DOM_MARKERS = List.of(
            By.cssSelector("iframe[src*='recaptcha']"),
            By.cssSelector("iframe[src*='hcaptcha']"),
            By.cssSelector("iframe[title*='captcha']"),
            By.id("px-captcha") // PerimeterX / HUMAN "Press & Hold" widget'i
    );

    private static final List<String> TEXT_MARKERS = List.of(
            "robot olmadığın",
            "güvenlik doğrulaması",
            "doğrulama gerekiyor",
            "erişim engellendi",
            "access denied",
            "unusual traffic",
            "are you a human"
    );

    private static final List<String> OTP_TEXT_MARKERS = List.of(
            "doğrulama kodu",
            "sms ile gönder",
            "telefonunuza gönder",
            "cep telefonunuza"
    );

    private BotDetection() {
    }

    /** OTP ekrani gorunuyorsa screenshot + OtpRequiredException; yoksa sessizce doner. */
    public static void checkForOtpChallenge(WebDriver driver, String context) {
        String title = safeLower(driver.getTitle());
        String bodyText = safeLower(bodyTextOf(driver));
        for (String marker : OTP_TEXT_MARKERS) {
            String needle = marker.toLowerCase(TR);
            if (title.contains(needle) || bodyText.contains(needle)) {
                Path screenshot = ScreenshotUtil.capture(driver, "otp_ekrani_" + context);
                throw new OtpRequiredException(context + " — isaret: \"" + marker + "\"", screenshot);
            }
        }
    }

    /** Bot-engel/CAPTCHA yoksa sessizce doner; varsa screenshot + BlockedException. */
    public static void checkAndFailIfBlocked(WebDriver driver, String context) {
        String marker = findBlockMarker(driver);
        if (marker != null) {
            Path screenshot = ScreenshotUtil.capture(driver, "bot_block_" + context);
            throw new BlockedException(context, marker, screenshot);
        }
    }

    private static String findBlockMarker(WebDriver driver) {
        for (By domMarker : DOM_MARKERS) {
            if (!driver.findElements(domMarker).isEmpty()) {
                return domMarker.toString();
            }
        }
        String title = safeLower(driver.getTitle());
        String bodyText = safeLower(bodyTextOf(driver));
        for (String textMarker : TEXT_MARKERS) {
            if (title.contains(textMarker.toLowerCase(TR)) || bodyText.contains(textMarker.toLowerCase(TR))) {
                return "sayfa metni: \"" + textMarker + "\"";
            }
        }
        return null;
    }

    private static String bodyTextOf(WebDriver driver) {
        try {
            return driver.findElement(By.tagName("body")).getText();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(TR);
    }

    /** CAPTCHA / bot-engel sayfasi tespit edilince firlatilir (asilmaz, raporlanir). */
    public static class BlockedException extends RuntimeException {
        public BlockedException(String context, String marker, Path screenshotPath) {
            super("BOT KORUMASI TESPIT EDILDI [" + context + "] — site CAPTCHA/dogrulama sayfasi gosterdi. "
                    + "Tetikleyen isaret: '" + marker + "'. Engeli asmak bilerek denenmedi (kapsam disi). "
                    + "Ekran goruntusu: " + (screenshotPath != null ? screenshotPath : "alinamadi") + ". "
                    + "Oneri: testi farkli bir IP/zamanda tekrar kosun ya da manuel dogrulama sonrasi deneyin.");
        }
    }

    /** Site SMS/OTP dogrulama kodu istediginde firlatilir (kod OKUNMAZ, raporlanir). */
    public static class OtpRequiredException extends RuntimeException {
        public OtpRequiredException(String context, Path screenshotPath) {
            super("SMS/OTP DOGRULAMASI ISTENDI [" + context + "] — otomasyon dogrulama kodunu "
                    + "bilerek OKUMAZ (kapsam disi). Cozum: chrome.profile.dir ayari acikken testi bir kez "
                    + "calistirip OTP ekrani geldiginde kodu ELLE girin; profil cihazi hatirlayacagi icin "
                    + "sonraki kosularda kod istenmez. Ekran goruntusu: "
                    + (screenshotPath != null ? screenshotPath : "alinamadi"));
        }
    }
}
